package com.nova.bioconnect.icpmgr.protocol;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * DML Message Handler
 * Processes incoming DML messages and drives the state machine
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DmlMessageHandler {

    private final DmlMessageParser parser;
    private final DmlMessageBuilder builder;
    private final DmlStateMachineActions actions;

    /**
     * Handle an incoming DML message
     * @param message the raw XML message
     * @param session the current DML session
     * @return list of response messages to send back
     */
    public List<String> handleMessage(String message, DmlSession session) {
        List<String> responses = new ArrayList<>();

        try {
            String msgType = parser.parseMessageType(message);
            log.info("Session {} - Received message type: {}", session.getSessionId(), msgType);

            // Reset KPA timeout on any message received
            session.resetKpaTimeout();
            session.setWaiting(false);

            switch (msgType) {
                case "HEL.R01":
                    responses.addAll(handleHello(message, session));
                    break;

                case "DST.R01":
                    responses.addAll(handleDeviceStatus(message, session));
                    break;

                case "ACK.R01":
                    responses.addAll(handleAcknowledge(message, session));
                    break;

                case "OBS.R01":
                case "OBS.R02":
                    responses.addAll(handleObservation(message, session));
                    break;

                case "EOT.R01":
                    responses.addAll(handleEndOfTopic(message, session));
                    break;

                case "EVS.R01":
                    responses.addAll(handleEvents(message, session));
                    break;

                case "KPA.R01":
                    responses.addAll(handleKeepAlive(session));
                    break;

                case "ESC.R01":
                    responses.addAll(handleEscape(session));
                    break;

                case "END.R01":
                    responses.addAll(handleEnd(message, session));
                    break;

                case "DTV.NOVA_REQ.R02":
                    responses.addAll(handleQuery(message, session));
                    break;

                case "NOVA.ANALYZER_STATE":
                case "NOVA.CARTRIDGE_STATUS":
                case "NOVA.TEST_STATUS":
                    responses.addAll(handleSystemStatus(message, session, msgType));
                    break;

                default:
                    log.warn("Session {} - Unknown message type: {}", session.getSessionId(), msgType);
                    responses.add(builder.buildEscapeMessage("Unknown message type: " + msgType, session));
                    session.close();
                    break;
            }
        } catch (Exception e) {
            log.error("Session {} - Error handling message", session.getSessionId(), e);
            responses.add(builder.buildEscapeMessage("Error processing message: " + e.getMessage(), session));
        }

        return responses;
    }

    /**
     * Handle HEL.R01 - Hello message from device
     */
    private List<String> handleHello(String message, DmlSession session) {
        List<String> responses = new ArrayList<>();

        // Parse device info
        DmlMessageParser.DeviceInfo deviceInfo = parser.parseDeviceInfo(message);
        session.setSerialId(deviceInfo.getSerialId());
        session.setDeviceName(deviceInfo.getDeviceName());
        session.setSwVersion(deviceInfo.getSwVersion());
        session.setHwVersion(deviceInfo.getHwVersion());
        session.setDeviceType(deviceInfo.getDeviceType());
        session.setDeviceClass(deviceInfo.getDeviceClass());
        session.setFromInstId(deviceInfo.getFromInstId());
        session.setVendorId(deviceInfo.getVendorId());

        log.info("Session {} - Device Hello: serial={}, name={}, type={}",
                session.getSessionId(), deviceInfo.getSerialId(),
                deviceInfo.getDeviceName(), deviceInfo.getDeviceType());

        // Parse control_id
        String controlId = parser.parseControlId(message);
        session.setControlId(controlId);

        // Send ACK Hello
        String ackMsg = builder.buildAckMessage(controlId, "AA", session);
        responses.add(ackMsg);

        // Transition state
        session.sendEvent(DmlEvent.HEL_RECEIVED);

        return responses;
    }

    /**
     * Handle DST.R01 - Device status message
     */
    private List<String> handleDeviceStatus(String message, DmlSession session) {
        List<String> responses = new ArrayList<>();

        // Parse status info
        DmlMessageParser.StatusInfo statusInfo = parser.parseStatusInfo(message);
        session.setNewObservationsQty(parseInt(statusInfo.getNewObservationsQty(), 0));
        session.setNewEventsQty(parseInt(statusInfo.getNewEventsQty(), 0));
        session.setLocationNum(statusInfo.getLocationNum());
        session.setFacility(statusInfo.getFacility());
        session.setInstNum(statusInfo.getInstNum());
        session.setSetTimeSupported("T".equalsIgnoreCase(statusInfo.getSupportsSetTime()));
        session.setContinuousSupported("T".equalsIgnoreCase(statusInfo.getSupportsContinuous()));

        log.info("Session {} - Device Status: obs={}, evs={}, loc={}, setTime={}, continuous={}",
                session.getSessionId(),
                session.getNewObservationsQty(),
                session.getNewEventsQty(),
                session.getLocationNum(),
                session.isSetTimeSupported(),
                session.isContinuousSupported());

        // Update extended state
        session.setVariable("new_observations_qty", session.getNewObservationsQty());
        session.setVariable("new_events_qty", session.getNewEventsQty());
        session.setVariable("set_time_supported", session.isSetTimeSupported());
        session.setVariable("continuous_supported", session.isContinuousSupported());

        // Send ACK
        String controlId = parser.parseControlId(message);
        String ackMsg = builder.buildAckMessage(controlId, "AA", session);
        responses.add(ackMsg);

        // Transition to ACK_HELLO then to REQ_OBS (or skip if no observations)
        session.sendEvent(DmlEvent.DST_RECEIVED);

        // If observations exist, request them
        if (session.getNewObservationsQty() > 0) {
            String obsRequest = builder.buildObservationRequest(session);
            responses.add(obsRequest);
        } else {
            // Skip observations, go to events
            session.sendEvent(DmlEvent.SEND_EVS_REQUEST);
            if (session.getNewEventsQty() > 0) {
                String evsRequest = builder.buildEventRequest(session);
                responses.add(evsRequest);
            } else {
                // Skip events, go to set time
                if (session.isSetTimeSupported()) {
                    String timeMsg = builder.buildSetTimeMessage(session);
                    responses.add(timeMsg);
                    session.sendEvent(DmlEvent.SEND_SET_TIME);
                } else {
                    // Skip to setup
                    sendSetupSequence(message, session, responses);
                }
            }
        }

        return responses;
    }

    /**
     * Handle ACK.R01 - Acknowledgment message
     */
    private List<String> handleAcknowledge(String message, DmlSession session) {
        List<String> responses = new ArrayList<>();

        String ackControlId = parser.parseAckControlId(message);
        String controlId = parser.parseControlId(message);

        log.info("Session {} - ACK: ack_control_id={}, type={}",
                session.getSessionId(), ackControlId, controlId);

        // Transition based on current state
        DmlState currentState = session.getCurrentState();
        switch (currentState) {
            case SETUP_SENT:
            case SETUP_SENT_WAITING_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                sendWifiSetupSequence(message, session, responses);
                break;

            case WIFI_SETUP_SENT:
            case WIFI_SETUP_SENT_WAITING_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                sendWifiCertSequence(message, session, responses);
                break;

            case WIFI_CERT_SENT:
            case WIFI_CERT_SENT_WAITING_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                sendLocationSequence(message, session, responses);
                break;

            case LOC_SENT:
            case LOC_SENT_WAITING_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                sendOperatorSequence(message, session, responses);
                break;

            case OPL_SENT:
            case OPL_SENT_WAITING_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                sendPatientSequence(message, session, responses);
                break;

            case PTL_SENT:
            case PTL_SENT_WAITING_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                sendPhysicianSequence(message, session, responses);
                break;

            case PHYS_SENT:
            case PHYS_SENT_WAITING_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                sendFirmwareSequence(message, session, responses);
                break;

            case FIRM_SENT:
            case FIRM_SENT_WAITING_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                sendReagentSequence(message, session, responses);
                break;

            case REAG_SENT:
            case REAG_SENT_WAITING_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                // Decision: continuous or terminate
                if (session.isContinuousSupported() && session.getLocationNum() != null && !session.getLocationNum().isEmpty()) {
                    String continuousMsg = builder.buildContinuousMessage(session);
                    responses.add(continuousMsg);
                    session.sendEvent(DmlEvent.SEND_CONTINUOUS);
                } else {
                    String terminateMsg = builder.buildTerminateMessage("NRM", "", session);
                    responses.add(terminateMsg);
                    session.sendEvent(DmlEvent.SEND_TERMINATE);
                }
                break;

            case SET_TIME:
            case SET_TIME_ACK:
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                sendSetupSequence(message, session, responses);
                break;

            case CONTINUOUS:
            case CONTINUOUS_ACK:
                session.setContinuous(true);
                session.setKpaEnabled(true);
                session.setKpaTimeoutCount(0);
                session.setWaiting(false);
                session.setBusy(false);
                log.info("Session {} - Entering continuous mode", session.getSessionId());
                // Send keepalive periodically
                break;

            default:
                // Generic ACK - just acknowledge the transition
                session.sendEvent(DmlEvent.ACK_RECEIVED);
                break;
        }

        return responses;
    }

    /**
     * Handle OBS.R01/R02 - Observation data
     */
    private List<String> handleObservation(String message, DmlSession session) {
        List<String> responses = new ArrayList<>();

        int obsCount = parser.parseObservationCount(message);
        log.info("Session {} - Received {} observations", session.getSessionId(), obsCount);

        // Process observation data
        actions.processObservations(message, session, obsCount);

        // Send ACK
        String controlId = parser.parseControlId(message);
        responses.add(builder.buildAckMessage(controlId, "AA", session));

        return responses;
    }

    /**
     * Handle EOT.R01 - End of topic
     */
    private List<String> handleEndOfTopic(String message, DmlSession session) {
        List<String> responses = new ArrayList<>();

        String topic = parser.parseEotTopic(message);
        log.info("Session {} - EOT received for topic: {}", session.getSessionId(), topic);

        DmlState currentState = session.getCurrentState();

        // EOT transitions
        switch (currentState) {
            case REQ_OBS:
                session.sendEvent(DmlEvent.OBS_EOT_RECEIVED);
                // Now request events
                if (session.getNewEventsQty() > 0) {
                    responses.add(builder.buildEventRequest(session));
                    session.sendEvent(DmlEvent.SEND_EVS_REQUEST);
                } else {
                    // Skip events
                    if (session.isSetTimeSupported()) {
                        responses.add(builder.buildSetTimeMessage(session));
                        session.sendEvent(DmlEvent.SEND_SET_TIME);
                    } else {
                        sendSetupSequence(message, session, responses);
                    }
                }
                break;

            case REQ_EVS:
                session.sendEvent(DmlEvent.EVS_EOT_RECEIVED);
                // Go to set time or skip
                if (session.isSetTimeSupported()) {
                    responses.add(builder.buildSetTimeMessage(session));
                    session.sendEvent(DmlEvent.SEND_SET_TIME);
                } else {
                    sendSetupSequence(message, session, responses);
                }
                break;

            default:
                log.warn("Session {} - EOT received in unexpected state: {}",
                        session.getSessionId(), currentState);
                break;
        }

        return responses;
    }

    /**
     * Handle EVS.R01 - Event data
     */
    private List<String> handleEvents(String message, DmlSession session) {
        List<String> responses = new ArrayList<>();

        int evsCount = parser.parseEventCount(message);
        log.info("Session {} - Received {} events", session.getSessionId(), evsCount);

        // Process event data
        actions.processEvents(message, session, evsCount);

        // Send ACK
        String controlId = parser.parseControlId(message);
        responses.add(builder.buildAckMessage(controlId, "AA", session));

        return responses;
    }

    /**
     * Handle KPA.R01 - Keep alive
     */
    private List<String> handleKeepAlive(DmlSession session) {
        List<String> responses = new ArrayList<>();
        session.resetKpaTimeout();
        log.debug("Session {} - Keep alive received", session.getSessionId());
        return responses;
    }

    /**
     * Handle ESC.R01 - Escape (device requesting disconnect)
     */
    private List<String> handleEscape(DmlSession session) {
        List<String> responses = new ArrayList<>();
        log.info("Session {} - Device sent ESC, disconnecting", session.getSessionId());
        session.close();
        return responses;
    }

    /**
     * Handle END.R01 - End connection
     */
    private List<String> handleEnd(String message, DmlSession session) {
        List<String> responses = new ArrayList<>();

        // Send ACK first
        String controlId = parser.parseControlId(message);
        responses.add(builder.buildAckMessage(controlId, "AA", session));

        log.info("Session {} - Device sent END, disconnecting", session.getSessionId());
        session.close();
        return responses;
    }

    /**
     * Handle DTV.NOVA_REQ.R02 - Query from device
     */
    private List<String> handleQuery(String message, DmlSession session) {
        List<String> responses = new ArrayList<>();
        log.info("Session {} - Query received from device", session.getSessionId());

        // ACK the query
        String controlId = parser.parseControlId(message);
        responses.add(builder.buildAckMessage(controlId, "AA", session));

        // Transition
        session.sendEvent(DmlEvent.QUERY_RECEIVED);

        return responses;
    }

    /**
     * Handle system status messages
     */
    private List<String> handleSystemStatus(String message, DmlSession session, String topicType) {
        List<String> responses = new ArrayList<>();
        log.info("Session {} - System status received: {}", session.getSessionId(), topicType);

        // Process system status
        actions.processSystemStatus(message, session, topicType);

        // Send ACK
        String controlId = parser.parseControlId(message);
        responses.add(builder.buildAckMessage(controlId, "AA", session));

        return responses;
    }

    // === Helper methods for sequence transitions ===

    private void sendSetupSequence(String message, DmlSession session, List<String> responses) {
        session.sendEvent(DmlEvent.SEND_SETUP);
        boolean sent = actions.sendSetup(session);
        if (sent) {
            session.sendEvent(DmlEvent.SETUP_SENT_COMPLETE);
        } else {
            // Skip to WiFi setup
            session.sendEvent(DmlEvent.SEND_WIFI_SETUP);
            sendWifiSetupSequence(message, session, responses);
        }
    }

    private void sendWifiSetupSequence(String message, DmlSession session, List<String> responses) {
        session.sendEvent(DmlEvent.SEND_WIFI_SETUP);
        boolean sent = actions.sendWifiSetup(session);
        if (sent) {
            session.sendEvent(DmlEvent.WIFI_SETUP_SENT_COMPLETE);
        } else {
            session.sendEvent(DmlEvent.SEND_WIFI_CERT);
            sendWifiCertSequence(message, session, responses);
        }
    }

    private void sendWifiCertSequence(String message, DmlSession session, List<String> responses) {
        session.sendEvent(DmlEvent.SEND_WIFI_CERT);
        boolean sent = actions.sendWifiCert(session);
        if (sent) {
            session.sendEvent(DmlEvent.WIFI_CERT_SENT_COMPLETE);
        } else {
            session.sendEvent(DmlEvent.SEND_LOCATION);
            sendLocationSequence(message, session, responses);
        }
    }

    private void sendLocationSequence(String message, DmlSession session, List<String> responses) {
        session.sendEvent(DmlEvent.SEND_LOCATION);
        boolean sent = actions.sendLocationList(session);
        if (sent) {
            session.sendEvent(DmlEvent.LOC_SENT_COMPLETE);
        } else {
            session.sendEvent(DmlEvent.SEND_OPERATOR);
            sendOperatorSequence(message, session, responses);
        }
    }

    private void sendOperatorSequence(String message, DmlSession session, List<String> responses) {
        session.sendEvent(DmlEvent.SEND_OPERATOR);
        boolean sent = actions.sendOperatorList(session);
        if (sent) {
            session.sendEvent(DmlEvent.OPL_SENT_COMPLETE);
        } else {
            session.sendEvent(DmlEvent.SEND_PATIENT);
            sendPatientSequence(message, session, responses);
        }
    }

    private void sendPatientSequence(String message, DmlSession session, List<String> responses) {
        session.sendEvent(DmlEvent.SEND_PATIENT);
        boolean sent = actions.sendPatientList(session);
        if (sent) {
            session.sendEvent(DmlEvent.PTL_SENT_COMPLETE);
        } else {
            session.sendEvent(DmlEvent.SEND_PHYSICIAN);
            sendPhysicianSequence(message, session, responses);
        }
    }

    private void sendPhysicianSequence(String message, DmlSession session, List<String> responses) {
        session.sendEvent(DmlEvent.SEND_PHYSICIAN);
        boolean sent = actions.sendPhysicianList(session);
        if (sent) {
            session.sendEvent(DmlEvent.PHYS_SENT_COMPLETE);
        } else {
            session.sendEvent(DmlEvent.SEND_FIRMWARE);
            sendFirmwareSequence(message, session, responses);
        }
    }

    private void sendFirmwareSequence(String message, DmlSession session, List<String> responses) {
        session.sendEvent(DmlEvent.SEND_FIRMWARE);
        boolean sent = actions.sendFirmware(session);
        if (sent) {
            session.sendEvent(DmlEvent.FIRM_SENT_COMPLETE);
        } else {
            session.sendEvent(DmlEvent.SEND_REAGENT);
            sendReagentSequence(message, session, responses);
        }
    }

    private void sendReagentSequence(String message, DmlSession session, List<String> responses) {
        session.sendEvent(DmlEvent.SEND_REAGENT);
        boolean sent = actions.sendReagents(session);
        if (sent) {
            session.sendEvent(DmlEvent.REAG_SENT_COMPLETE);
        } else {
            // Skip reagent, go to decision
            if (session.isContinuousSupported() && session.getLocationNum() != null && !session.getLocationNum().isEmpty()) {
                responses.add(builder.buildContinuousMessage(session));
                session.sendEvent(DmlEvent.SEND_CONTINUOUS);
            } else {
                responses.add(builder.buildTerminateMessage("NRM", "", session));
                session.sendEvent(DmlEvent.SEND_TERMINATE);
            }
        }
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}