package com.nova.bioconnect.rtm.service;

import com.nova.bioconnect.rtm.config.BioConnectProperties;
import com.nova.bioconnect.rtm.hl7.Hl7Constants;
import com.nova.bioconnect.rtm.hl7.Hl7Message;
import com.nova.bioconnect.rtm.message.AckBuilder;
import com.nova.bioconnect.rtm.message.AdtBuilder;
import com.nova.bioconnect.rtm.model.MergeInfo;
import com.nova.bioconnect.rtm.model.PatientInfo;
import com.nova.bioconnect.rtm.model.VisitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles inbound ADT messages (from HIS) for the RTMADTP patient data exchange flow.
 *
 * <p>Inbound: validates the trigger event, extracts patient/visit/merge data, delegates
 * persistence and DML PAT.R01 push to {@link PatientService}, then returns an ACK
 * (MSH + MSA) to the sender.
 *
 * <p>Trigger events listed in {@link Hl7Constants#IGNORED_ADT_TRIGGERS} are acknowledged
 * but not forwarded. ADR^A19 (query) is not supported.
 *
 * <p>Patient data push to devices is handled by {@link PatientService} via the DML protocol
 * (PAT.R01), not via HL7 MLLP forwarding.
 */
@Service
public class AdtService {

    private static final Logger log = LoggerFactory.getLogger(AdtService.class);

    private final BioConnectProperties properties;
    private final PatientService patientService;

    public AdtService(BioConnectProperties properties, PatientService patientService) {
        this.properties = properties;
        this.patientService = patientService;
    }

    /**
     * Process an inbound ADT message and return the ACK to send back to the sender.
     *
     * @param inbound the ADT message received from HIS
     * @return the acknowledgement message (AA on success, AE on error / ignored)
     */
    public Hl7Message handleInbound(Hl7Message inbound) {
        String trigger = inbound.getTriggerEvent();
        String controlId = inbound.getMessageControlId();
        log.info("ADT inbound: {}^{} (control id {})", inbound.getMessageTypeCode(), trigger, controlId);

        if (isIgnored(trigger)) {
            log.warn("ADT trigger {} is ignored by the interface; acknowledging without processing", trigger);
            return AckBuilder.build(inbound, Hl7Constants.ACK_AA, properties.getProcessingId(), properties.getVersion());
        }

        try {
            PatientInfo patient = AdtBuilder.extractPatient(inbound);
            VisitInfo visit = AdtBuilder.extractVisit(inbound);
            MergeInfo merge = AdtBuilder.extractMerge(inbound);
            log.debug("ADT patient MRN={}, account={}, trigger={}",
                    patient == null ? "" : patient.internalPatientId(),
                    patient == null ? "" : patient.accountNumber(),
                    trigger);

            patientService.processAdtEvent(trigger, patient, visit, merge);
            return AckBuilder.build(inbound, Hl7Constants.ACK_AA, properties.getProcessingId(), properties.getVersion());
        } catch (Exception e) {
            log.error("Failed to process ADT {} (control id {}): {}", trigger, controlId, e.getMessage(), e);
            return AckBuilder.build(inbound, Hl7Constants.ACK_AE, properties.getProcessingId(), properties.getVersion());
        }
    }

    private boolean isIgnored(String trigger) {
        if (trigger == null || trigger.isEmpty()) {
            return false;
        }
        if (Hl7Constants.IGNORED_ADR_TRIGGER.equals(trigger)) {
            return true;
        }
        return Hl7Constants.IGNORED_ADT_TRIGGERS.contains(trigger);
    }
}