package com.nova.bioconnect.novanet.service;

import com.nova.bioconnect.novanet.client.Hl7Client;
import com.nova.bioconnect.novanet.client.Hl7ClientManager;
import com.nova.bioconnect.novanet.config.BioConnectProperties;
import com.nova.bioconnect.novanet.hl7.Hl7Constants;
import com.nova.bioconnect.novanet.hl7.Hl7Message;
import com.nova.bioconnect.novanet.message.AckBuilder;
import com.nova.bioconnect.novanet.message.ResultBuilder;
import com.nova.bioconnect.novanet.model.ResultMessage;
import com.nova.bioconnect.novanet.util.MessageIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Handles inbound result messages (ORU^R01 / OUL^R21 from the device) and outbound result
 * forwarding (to the LIS/HIS).
 *
 * <p>Inbound: extracts the result model, standardises the message and forwards it to the LIS
 * client, then returns an ACK (MSH + MSA) to the sender (the device).
 */
@Service
public class ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultService.class);

    private final Hl7ClientManager clientManager;
    private final BioConnectProperties properties;
    private final MessageIdGenerator idGenerator;

    public ResultService(Hl7ClientManager clientManager, BioConnectProperties properties,
                         MessageIdGenerator idGenerator) {
        this.clientManager = clientManager;
        this.properties = properties;
        this.idGenerator = idGenerator;
    }

    /**
     * Process an inbound result message and return the ACK to send back to the sender (device).
     *
     * @param inbound the ORU^R01 / OUL^R21 message received from the device
     * @return the acknowledgement message (AA on success, AE on error)
     */
    public Hl7Message handleInbound(Hl7Message inbound) {
        String type = inbound.getMessageType();
        String controlId = inbound.getMessageControlId();
        log.info("Result inbound: {} (control id {})", type, controlId);

        try {
            ResultMessage result = ResultBuilder.extract(inbound);
            log.debug("Result {} : {} observations, qc={}", type,
                    result.results() == null ? 0 : result.results().size(), result.isQc());
            forwardToLis(result);
            return AckBuilder.build(inbound, Hl7Constants.ACK_AA, properties.getProcessingId(), properties.getVersion());
        } catch (Exception e) {
            log.error("Failed to process result {} (control id {}): {}", type, controlId, e.getMessage(), e);
            return AckBuilder.build(inbound, Hl7Constants.ACK_AE, properties.getProcessingId(), properties.getVersion());
        }
    }

    /**
     * Build and send a standardised result message to the LIS/HIS (Results OUT interface).
     *
     * @return a future completing with the LIS's ACK, or a failed future if the LIS client is
     *         disabled/not connected.
     */
    public CompletableFuture<Hl7Message> forwardToLis(ResultMessage result) {
        Hl7Client lis = clientManager.getLisClient();
        if (lis == null) {
            log.debug("LIS client disabled; result not forwarded");
            return CompletableFuture.failedFuture(new IllegalStateException("lis client disabled"));
        }
        Hl7Message outbound = ResultBuilder.build(result,
                properties.getSendingApplication(), properties.getSendingFacility(),
                idGenerator.next(), properties.getProcessingId(), properties.getVersion());
        log.info("Result outbound -> LIS: {} (control id {})", outbound.getMessageType(),
                outbound.getMessageControlId());
        CompletableFuture<Hl7Message> ack = lis.send(outbound);
        ack.whenComplete((a, err) -> {
            if (err != null) {
                log.warn("Result forward to LIS failed (control id {}): {}",
                        outbound.getMessageControlId(), err.toString());
            } else {
                String code = a.getSegment(Hl7Constants.MSA).map(s -> s.getField(1)).orElse("");
                log.info("Result forward to LIS ACKed: {} (control id {})", code, outbound.getMessageControlId());
            }
        });
        return ack;
    }
}
