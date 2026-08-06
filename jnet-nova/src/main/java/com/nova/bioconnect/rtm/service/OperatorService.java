package com.nova.bioconnect.rtm.service;

import com.nova.bioconnect.rtm.dml.DmlClientManager;
import com.nova.bioconnect.rtm.dml.DmlMessageBuilder;
import com.nova.bioconnect.rtm.entity.*;
import com.nova.bioconnect.rtm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * RTMOPL equivalent service - manages operator/medical staff data.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD operations on operator data</li>
 *   <li>Manage operator privileges, unit assignments, and methods</li>
 *   <li>Push operator data changes to devices via DML protocol (OPL.R01)</li>
 * </ul>
 */
@Service
public class OperatorService {

    private static final Logger log = LoggerFactory.getLogger(OperatorService.class);

    private final OperatorRepository operatorRepository;
    private final OperatorPrivilegeRepository privilegeRepository;
    private final OperatorToUnitRepository operatorToUnitRepository;
    private final MethodRepository methodRepository;
    private final DmlMessageBuilder dmlBuilder;
    private final DmlClientManager dmlClientManager;

    public OperatorService(OperatorRepository operatorRepository,
                            OperatorPrivilegeRepository privilegeRepository,
                            OperatorToUnitRepository operatorToUnitRepository,
                            MethodRepository methodRepository,
                            DmlMessageBuilder dmlBuilder,
                            DmlClientManager dmlClientManager) {
        this.operatorRepository = operatorRepository;
        this.privilegeRepository = privilegeRepository;
        this.operatorToUnitRepository = operatorToUnitRepository;
        this.methodRepository = methodRepository;
        this.dmlBuilder = dmlBuilder;
        this.dmlClientManager = dmlClientManager;
    }

    @Transactional
    public OperatorEntity createOperator(OperatorCreateRequest request) {
        OperatorEntity ent = new OperatorEntity();
        ent.setOperatorNum(UUID.randomUUID().toString());
        ent.setOperatorId(request.operatorId());
        ent.setFirstName(request.firstName());
        ent.setLastName(request.lastName());
        ent.setIsSupervisor(request.isSupervisor() ? "T" : "F");
        ent.setPrivilegeLevel(request.privilegeLevel());
        ent.setStatus("A");
        OperatorEntity saved = operatorRepository.save(ent);

        if (request.privileges() != null) {
            for (OperatorCreateRequest.Privilege priv : request.privileges()) {
                OperatorPrivilegeEntity privEnt = new OperatorPrivilegeEntity();
                privEnt.setOperatorNum(saved.getOperatorNum());
                privEnt.setInstType(priv.instType());
                privEnt.setPrivilegeCode(priv.privilegeCode());
                privEnt.setPrivilegeDesc(priv.privilegeDesc());
                privilegeRepository.save(privEnt);
            }
        }

        if (request.unitLocNums() != null) {
            for (String locNum : request.unitLocNums()) {
                OperatorToUnitEntity otuEnt = new OperatorToUnitEntity();
                otuEnt.setOperatorNum(saved.getOperatorNum());
                otuEnt.setLocNum(locNum);
                operatorToUnitRepository.save(otuEnt);
            }
        }

        if (request.methods() != null) {
            for (OperatorCreateRequest.Method method : request.methods()) {
                MethodEntity methodEnt = new MethodEntity();
                methodEnt.setOperatorNum(saved.getOperatorNum());
                methodEnt.setInstType(method.instType());
                methodEnt.setMethodCode(method.methodCode());
                methodEnt.setMethodName(method.methodName());
                methodRepository.save(methodEnt);
            }
        }

        log.info("Created operator: {} ({})", saved.getOperatorId(), saved.getOperatorNum());
        pushOperatorToDevices(saved);
        return saved;
    }

    @Transactional
    public OperatorEntity updateOperator(String operatorNum, OperatorUpdateRequest request) {
        Optional<OperatorEntity> opt = operatorRepository.findByOperatorNum(operatorNum);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Operator not found: " + operatorNum);
        }
        OperatorEntity ent = opt.get();
        if (request.firstName() != null) ent.setFirstName(request.firstName());
        if (request.lastName() != null) ent.setLastName(request.lastName());
        if (request.isSupervisor() != null) ent.setIsSupervisor(request.isSupervisor() ? "T" : "F");
        if (request.privilegeLevel() != null) ent.setPrivilegeLevel(request.privilegeLevel());
        OperatorEntity saved = operatorRepository.save(ent);

        if (request.privileges() != null) {
            privilegeRepository.findByOperatorNum(operatorNum).forEach(privilegeRepository::delete);
            for (OperatorCreateRequest.Privilege priv : request.privileges()) {
                OperatorPrivilegeEntity privEnt = new OperatorPrivilegeEntity();
                privEnt.setOperatorNum(operatorNum);
                privEnt.setInstType(priv.instType());
                privEnt.setPrivilegeCode(priv.privilegeCode());
                privEnt.setPrivilegeDesc(priv.privilegeDesc());
                privilegeRepository.save(privEnt);
            }
        }

        pushOperatorToDevices(saved);
        return saved;
    }

    @Transactional
    public void deleteOperator(String operatorNum) {
        operatorRepository.findByOperatorNum(operatorNum).ifPresent(ent -> {
            ent.setStatus("I");
            operatorRepository.save(ent);
            log.info("Deleted operator: {}", ent.getOperatorId());
        });
    }

    public Optional<OperatorEntity> findByOperatorId(String operatorId) {
        return operatorRepository.findByOperatorId(operatorId);
    }

    public Optional<OperatorEntity> findByOperatorNum(String operatorNum) {
        return operatorRepository.findByOperatorNum(operatorNum);
    }

    public List<OperatorEntity> findAllActive() {
        return operatorRepository.findAll().stream()
                .filter(e -> "A".equals(e.getStatus()))
                .toList();
    }

    public List<OperatorPrivilegeEntity> findPrivileges(String operatorNum) {
        return privilegeRepository.findByOperatorNum(operatorNum);
    }

    public List<OperatorToUnitEntity> findUnits(String operatorNum) {
        return operatorToUnitRepository.findByOperatorNum(operatorNum);
    }

    public List<MethodEntity> findMethods(String operatorNum) {
        return methodRepository.findByOperatorNum(operatorNum);
    }

    private void pushOperatorToDevices(OperatorEntity operator) {
        try {
            DmlMessageBuilder.OperatorData data = new DmlMessageBuilder.OperatorData(
                    operator.getOperatorId(),
                    operator.getFirstName(),
                    operator.getLastName(),
                    "T".equals(operator.getIsSupervisor()),
                    operator.getPrivilegeLevel() != null ? operator.getPrivilegeLevel() : 2,
                    operator.getFacility() != null ? operator.getFacility() : "",
                    operator.getLocation() != null ? operator.getLocation() : ""
            );
            String messageId = UUID.randomUUID().toString();
            String operatorXml = dmlBuilder.buildOperatorMessage(messageId, data);

            CompletableFuture<String> ackFuture = dmlClientManager.send(operatorXml, messageId);
            ackFuture.whenComplete((ack, err) -> {
                if (err != null) {
                    log.warn("Operator DML push failed (operatorId={}): {}",
                            operator.getOperatorId(), err.getMessage());
                } else {
                    log.info("Operator DML push ACKed: operatorId={}, ack={}",
                            operator.getOperatorId(), ack.length() > 100 ? ack.substring(0, 100) + "..." : ack);
                }
            });
            log.info("Operator DML message sent to device: operatorId={}, messageId={}",
                    operator.getOperatorId(), messageId);
        } catch (Exception e) {
            log.error("Failed to push operator to device", e);
        }
    }

    public record OperatorCreateRequest(
            String operatorId,
            String firstName,
            String lastName,
            boolean isSupervisor,
            Integer privilegeLevel,
            List<Privilege> privileges,
            List<String> unitLocNums,
            List<Method> methods
    ) {
        public record Privilege(String instType, String privilegeCode, String privilegeDesc) {}
        public record Method(String instType, String methodCode, String methodName) {}
    }

    public record OperatorUpdateRequest(
            String firstName,
            String lastName,
            Boolean isSupervisor,
            Integer privilegeLevel,
            List<OperatorCreateRequest.Privilege> privileges
    ) {}
}