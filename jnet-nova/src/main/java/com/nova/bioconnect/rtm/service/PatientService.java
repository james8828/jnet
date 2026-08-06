package com.nova.bioconnect.rtm.service;

import com.nova.bioconnect.rtm.dml.DmlClientManager;
import com.nova.bioconnect.rtm.dml.DmlMessageBuilder;
import com.nova.bioconnect.rtm.entity.PatientAccountEntity;
import com.nova.bioconnect.rtm.entity.PatientEntity;
import com.nova.bioconnect.rtm.entity.PatientVisitEntity;
import com.nova.bioconnect.rtm.model.MergeInfo;
import com.nova.bioconnect.rtm.model.PatientInfo;
import com.nova.bioconnect.rtm.model.VisitInfo;
import com.nova.bioconnect.rtm.repository.PatientAccountRepository;
import com.nova.bioconnect.rtm.repository.PatientRepository;
import com.nova.bioconnect.rtm.repository.PatientVisitRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * RTMADTP equivalent service - manages patient data lifecycle.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Persist patient, account, visit data to database</li>
 *   <li>Push patient data changes to devices via DML protocol (PAT.R01)</li>
 *   <li>Handle patient merge (A31/A40) and identity change (A47)</li>
 * </ul>
 * todo 获取患者信息（属性待定）后推送到设备
 */
@Slf4j
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientAccountRepository accountRepository;
    private final PatientVisitRepository visitRepository;
    private final DmlMessageBuilder dmlBuilder;
    private final DmlClientManager dmlClientManager;

    public PatientService(PatientRepository patientRepository,
                          PatientAccountRepository accountRepository,
                          PatientVisitRepository visitRepository,
                          DmlMessageBuilder dmlBuilder,
                          DmlClientManager dmlClientManager) {
        this.patientRepository = patientRepository;
        this.accountRepository = accountRepository;
        this.visitRepository = visitRepository;
        this.dmlBuilder = dmlBuilder;
        this.dmlClientManager = dmlClientManager;
    }

    @Transactional
    public void processAdtEvent(String trigger, PatientInfo patient, VisitInfo visit, MergeInfo merge) {
        log.info("Processing ADT event: trigger={}, MRN={}, account={}",
                trigger, patient != null ? patient.internalPatientId() : "",
                patient != null ? patient.accountNumber() : "");

        switch (trigger) {
            case "A01", "A04", "A08" -> registerOrUpdatePatient(patient, visit);
            case "A40" -> mergePatient(patient, merge);
            case "A47" -> changePatientId(patient, merge);
            case "A02", "A03" -> dischargeOrTransferPatient(patient, visit);
            default -> log.debug("Trigger {} acknowledged but not processed for RTM", trigger);
        }
    }

    private void registerOrUpdatePatient(PatientInfo patient, VisitInfo visit) {
        if (patient == null) return;

        String patientNum = ensurePatientExists(patient);
        ensureAccountExists(patientNum, patient);
        if (visit != null) {
            ensureVisitExists(patientNum, patient, visit);
        }

        pushPatientToDevices(patient, visit);
    }

    private void mergePatient(PatientInfo patient, MergeInfo merge) {
        if (patient == null || merge == null) return;

        String keptPatientNum = ensurePatientExists(patient);
        String mergedPatientNum = findOrCreatePatient(
                merge.priorExternalPatientId(), merge.priorInternalPatientId());

        accountRepository.findByPatientNum(mergedPatientNum).forEach(acct -> {
            acct.setPatientNum(keptPatientNum);
            accountRepository.save(acct);
        });
        visitRepository.findByPatientNum(mergedPatientNum).forEach(v -> {
            v.setPatientNum(keptPatientNum);
            visitRepository.save(v);
        });

        patientRepository.findByPatientNum(mergedPatientNum).ifPresent(ent -> {
            ent.setStatus("I");
            patientRepository.save(ent);
        });

        log.info("Merged patient {} into {}", mergedPatientNum, keptPatientNum);
        pushPatientToDevices(patient, null);
    }

    private void changePatientId(PatientInfo patient, MergeInfo merge) {
        if (patient == null) return;

        String patientNum = ensurePatientExists(patient);
        updatePatientEntity(patientNum, patient);

        if (merge != null) {
            patientRepository.findByPatientNum(patientNum).ifPresent(ent -> {
                if (merge.priorExternalPatientId() != null) {
                    ent.setPatientId(merge.priorExternalPatientId());
                }
                if (merge.priorInternalPatientId() != null) {
                    ent.setMedrecNum(merge.priorInternalPatientId());
                }
                patientRepository.save(ent);
            });
        }

        pushPatientToDevices(patient, null);
    }

    private void dischargeOrTransferPatient(PatientInfo patient, VisitInfo visit) {
        if (patient == null) return;
        String patientNum = ensurePatientExists(patient);

        if (visit != null && visit.visitNumber() != null) {
            visitRepository.findByVisitNumber(visit.visitNumber()).ifPresent(v -> {
                v.setStatus("I");
                if (visit.dischargeDateTime() != null) {
                    v.setDischargingDate(visit.dischargeDateTime());
                }
                visitRepository.save(v);
            });
        }

        pushPatientToDevices(patient, visit);
    }

    private String ensurePatientExists(PatientInfo patient) {
        Optional<PatientEntity> existing = findPatientByIdentifiers(patient);
        if (existing.isPresent()) {
            return existing.get().getPatientNum();
        }
        return createPatientEntity(patient);
    }

    private Optional<PatientEntity> findPatientByIdentifiers(PatientInfo patient) {
        if (patient.internalPatientId() != null) {
            Optional<PatientEntity> found = patientRepository.findByMedrecNum(patient.internalPatientId());
            if (found.isPresent()) return found;
        }
        if (patient.externalPatientId() != null) {
            Optional<PatientEntity> found = patientRepository.findByPatientId(patient.externalPatientId());
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    private String findOrCreatePatient(String patientId, String medrecNum) {
        if (patientId != null) {
            return patientRepository.findByPatientId(patientId)
                    .map(PatientEntity::getPatientNum)
                    .orElseGet(() -> createPatientByIds(patientId, medrecNum));
        }
        if (medrecNum != null) {
            return patientRepository.findByMedrecNum(medrecNum)
                    .map(PatientEntity::getPatientNum)
                    .orElseGet(() -> createPatientByIds(patientId, medrecNum));
        }
        return createPatientByIds(null, null);
    }

    private String createPatientByIds(String patientId, String medrecNum) {
        String num = UUID.randomUUID().toString();
        PatientEntity ent = new PatientEntity();
        ent.setPatientNum(num);
        ent.setPatientId(patientId);
        ent.setMedrecNum(medrecNum);
        ent.setStatus("A");
        patientRepository.save(ent);
        return num;
    }

    private String createPatientEntity(PatientInfo patient) {
        String patientNum = UUID.randomUUID().toString();
        PatientEntity ent = new PatientEntity();
        ent.setPatientNum(patientNum);
        ent.setPatientId(patient.externalPatientId());
        ent.setMedrecNum(patient.internalPatientId());
        ent.setFirstName(patient.firstName());
        ent.setLastName(patient.lastName());
        ent.setMiddleName(patient.middleName());
        ent.setBirthDate(patient.dateOfBirth());
        ent.setSex(patient.gender());
        ent.setRace(patient.race());
        ent.setAddress(patient.address());
        ent.setPhoneHome(patient.phoneHome());
        ent.setStatus("A");
        patientRepository.save(ent);
        return patientNum;
    }

    private void updatePatientEntity(String patientNum, PatientInfo patient) {
        patientRepository.findByPatientNum(patientNum).ifPresent(ent -> {
            if (patient.firstName() != null) ent.setFirstName(patient.firstName());
            if (patient.lastName() != null) ent.setLastName(patient.lastName());
            if (patient.middleName() != null) ent.setMiddleName(patient.middleName());
            if (patient.dateOfBirth() != null) ent.setBirthDate(patient.dateOfBirth());
            if (patient.gender() != null) ent.setSex(patient.gender());
            if (patient.race() != null) ent.setRace(patient.race());
            if (patient.address() != null) ent.setAddress(patient.address());
            if (patient.phoneHome() != null) ent.setPhoneHome(patient.phoneHome());
            patientRepository.save(ent);
        });
    }

    private void ensureAccountExists(String patientNum, PatientInfo patient) {
        if (patient.accountNumber() == null) return;
        boolean exists = accountRepository.findByPatientNum(patientNum).stream()
                .anyMatch(a -> patient.accountNumber().equals(a.getAccountNumber()));
        if (!exists) {
            PatientAccountEntity acct = new PatientAccountEntity();
            acct.setAccountNum(UUID.randomUUID().toString());
            acct.setPatientNum(patientNum);
            acct.setAccountNumber(patient.accountNumber());
            acct.setStatus("A");
            accountRepository.save(acct);
        }
    }

    private void ensureVisitExists(String patientNum, PatientInfo patient, VisitInfo visit) {
        if (visit.visitNumber() == null) return;
        Optional<PatientVisitEntity> existing = visitRepository.findByVisitNumber(visit.visitNumber());
        if (existing.isEmpty()) {
            createVisitEntity(patientNum, patient, visit);
        } else {
            updateVisitEntity(existing.get(), patient, visit);
        }
    }

    private void createVisitEntity(String patientNum, PatientInfo patient, VisitInfo visit) {
        PatientVisitEntity ent = new PatientVisitEntity();
        ent.setVisitNum(UUID.randomUUID().toString());
        ent.setPatientNum(patientNum);
        ent.setAccountNum(patient.accountNumber());
        ent.setVisitNumber(visit.visitNumber());
        ent.setVisitType(visit.patientClass());
        ent.setLocation(visit.assignedLocation());
        ent.setRoom(visit.room());
        ent.setBed(visit.bed());
        ent.setFacility(visit.facility());
        ent.setAdmittingDoctor(visit.attendingPhysician());
        ent.setVisitDate(visit.admitDateTime());
        ent.setDischargingDate(visit.dischargeDateTime());
        ent.setStatus("A");
        visitRepository.save(ent);
    }

    private void updateVisitEntity(PatientVisitEntity ent, PatientInfo patient, VisitInfo visit) {
        if (visit.patientClass() != null) ent.setVisitType(visit.patientClass());
        if (visit.assignedLocation() != null) ent.setLocation(visit.assignedLocation());
        if (visit.room() != null) ent.setRoom(visit.room());
        if (visit.bed() != null) ent.setBed(visit.bed());
        if (visit.facility() != null) ent.setFacility(visit.facility());
        if (visit.attendingPhysician() != null) ent.setAdmittingDoctor(visit.attendingPhysician());
        if (visit.admitDateTime() != null) ent.setVisitDate(visit.admitDateTime());
        if (visit.dischargeDateTime() != null) ent.setDischargingDate(visit.dischargeDateTime());
        visitRepository.save(ent);
    }

    private void pushPatientToDevices(PatientInfo patient, VisitInfo visit) {
        try {
            String messageId = UUID.randomUUID().toString();
            String patientXml = dmlBuilder.buildPatientMessage(messageId, patient, visit);

            CompletableFuture<String> ackFuture = dmlClientManager.send(patientXml, messageId);
            ackFuture.whenComplete((ack, err) -> {
                if (err != null) {
                    log.warn("Patient DML push failed (MRN={}): {}",
                            patient != null ? patient.internalPatientId() : "", err.getMessage());
                } else {
                    log.info("Patient DML push ACKed: MRN={}, ack={}",
                            patient != null ? patient.internalPatientId() : "",
                            ack.length() > 100 ? ack.substring(0, 100) + "..." : ack);
                }
            });
            log.info("Patient DML message sent to device: MRN={}, messageId={}",
                    patient != null ? patient.internalPatientId() : "", messageId);
        } catch (Exception e) {
            log.error("Failed to push patient to device", e);
        }
    }

    public Optional<PatientEntity> findByPatientNum(String patientNum) {
        return patientRepository.findByPatientNum(patientNum);
    }

    public Optional<PatientEntity> findByMedrecNum(String medrecNum) {
        return patientRepository.findByMedrecNum(medrecNum);
    }

    public List<PatientEntity> findAllActive() {
        return patientRepository.findAll().stream()
                .filter(e -> "A".equals(e.getStatus()))
                .toList();
    }
}