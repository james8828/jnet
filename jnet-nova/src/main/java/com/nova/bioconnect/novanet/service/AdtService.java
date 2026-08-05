package com.nova.bioconnect.novanet.service;

import com.nova.bioconnect.novanet.client.Hl7Client;
import com.nova.bioconnect.novanet.client.Hl7ClientManager;
import com.nova.bioconnect.novanet.config.BioConnectProperties;
import com.nova.bioconnect.novanet.hl7.Hl7Constants;
import com.nova.bioconnect.novanet.hl7.Hl7Message;
import com.nova.bioconnect.novanet.message.AckBuilder;
import com.nova.bioconnect.novanet.message.AdtBuilder;
import com.nova.bioconnect.novanet.model.MergeInfo;
import com.nova.bioconnect.novanet.model.PatientInfo;
import com.nova.bioconnect.novanet.model.VisitInfo;
import com.nova.bioconnect.novanet.util.MessageIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Handles inbound ADT messages (from HIS) and outbound ADT forwarding (to the device/instrument).
 *
 * <p>Inbound: validates the trigger event, extracts patient/visit/merge data, standardises the
 * message and forwards it to the device client, then returns an ACK (MSH + MSA) to the sender.
 * Trigger events listed in {@link Hl7Constants#IGNORED_ADT_TRIGGERS} are acknowledged but not
 * forwarded. ADR^A19 (query) is not supported.
 */
@Service
public class AdtService {

    private static final Logger log = LoggerFactory.getLogger(AdtService.class);

    private final Hl7ClientManager clientManager;
    private final BioConnectProperties properties;
    private final MessageIdGenerator idGenerator;

    public AdtService(Hl7ClientManager clientManager, BioConnectProperties properties,
                      MessageIdGenerator idGenerator) {
        this.clientManager = clientManager;
        this.properties = properties;
        this.idGenerator = idGenerator;
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
            log.warn("ADT trigger {} is ignored by the interface; acknowledging without forwarding", trigger);
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
            forwardToDevice(trigger, patient, visit, merge);
            return AckBuilder.build(inbound, Hl7Constants.ACK_AA, properties.getProcessingId(), properties.getVersion());
        } catch (Exception e) {
            log.error("Failed to process ADT {} (control id {}): {}", trigger, controlId, e.getMessage(), e);
            return AckBuilder.build(inbound, Hl7Constants.ACK_AE, properties.getProcessingId(), properties.getVersion());
        }
    }

    /**
     * Build and send a standardised ADT message to the device/instrument (ADT OUT interface).
     *
     * @return a future completing with the device's ACK, or a failed future if the device client
     *         is disabled/not connected.
     */
    public CompletableFuture<Hl7Message> forwardToDevice(String trigger, PatientInfo patient,
                                                         VisitInfo visit, MergeInfo merge) {
        Hl7Client device = clientManager.getDeviceClient();
        if (device == null) {
            log.debug("Device client disabled; ADT {} not forwarded", trigger);
            return CompletableFuture.failedFuture(new IllegalStateException("device client disabled"));
        }
        Hl7Message outbound = AdtBuilder.build(trigger, patient, visit, merge,
                properties.getSendingApplication(), properties.getSendingFacility(),
                idGenerator.next(), properties.getProcessingId(), properties.getVersion());
        log.info("ADT outbound -> device: {}^{} (control id {})", outbound.getMessageTypeCode(), trigger,
                outbound.getMessageControlId());
        CompletableFuture<Hl7Message> ack = device.send(outbound);
        ack.whenComplete((a, err) -> {
            if (err != null) {
                log.warn("ADT forward to device failed (control id {}): {}",
                        outbound.getMessageControlId(), err.toString());
            } else {
                String code = a.getSegment(Hl7Constants.MSA).map(s -> s.getField(1)).orElse("");
                log.info("ADT forward to device ACKed: {} (control id {})", code, outbound.getMessageControlId());
            }
        });
        return ack;
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
