package com.nova.bioconnect.icpmgr.protocol;

import com.nova.bioconnect.icpmgr.entity.*;
import com.nova.bioconnect.icpmgr.repository.*;
import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DML StateMachine Actions
 * Implements business logic for state transitions.
 * Ported from C# DMLProtocol.cs (ProcessObservation, ProcessEvents, SendSetup, etc.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DmlStateMachineActions {

    private final DmlMessageBuilder messageBuilder;
    private final DmlDeviceRepository deviceRepository;
    private final DmlSampleDataRepository sampleDataRepository;
    private final DmlObservationRepository observationRepository;
    private final DmlDeviceEventRepository deviceEventRepository;
    private final DmlLocationRepository locationRepository;
    private final DmlLocLastUpdateRepository locLastUpdateRepository;
    private final DmlOperatorRepository operatorRepository;
    private final DmlPatientRepository patientRepository;
    private final DmlPhysicianRepository physicianRepository;
    private final DmlLotRepository lotRepository;
    private final DmlLotChemRepository lotChemRepository;
    private final DmlDeviceToLotRepository deviceToLotRepository;
    private final DmlConfigDataRepository configDataRepository;
    private final DmlLocToConfigRepository locToConfigRepository;
    private final DmlWifiSetupRepository wifiSetupRepository;
    private final DmlWifiCredentialRepository wifiCredentialRepository;
    private final DmlFirmwareRepository firmwareRepository;
    private final DmlOrderRepository orderRepository;
    private final DmlInstrumentTestRepository instrumentTestRepository;

    private static final DateTimeFormatter DML_DTTM_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final DateTimeFormatter DB_DTTM_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int MAX_ITEMS_PER_MSG = 100;

    // =========================================================================
    // processObservations - mirrors C# ProcessObservation(string typeOfOBS)
    // =========================================================================

    @Transactional
    public void processObservations(String message, DmlSession session, int obsCount) {
        log.info("Processing {} observations for session {}", obsCount, session.getSessionId());
        try {
            Document doc = parseXmlDocument(message);
            Element root = doc.getDocumentElement();

            // Extract control_id
            String controlId = getAttributeValue(root, "HDR/HDR.control_id");
            session.setControlId(controlId);

            boolean isQC = "OBS.R02".equals(root.getTagName());
            NodeList svcList = root.getElementsByTagName("SVC");

            for (int i = 0; i < svcList.getLength(); i++) {
                Element svc = (Element) svcList.item(i);
                processSingleSample(svc, session, isQC);
            }

            session.setWaiting(!session.isContinuous());
            sendAcknowledge(session, controlId, false);
        } catch (Exception e) {
            log.error("Error processing observations for session {}", session.getSessionId(), e);
            sendAcknowledge(session, session.getControlId(), true);
        }
    }

    private void processSingleSample(Element svc, DmlSession session, boolean isQC) {
        DmlSampleData sample = new DmlSampleData();
        sample.setDeviceSerialId(session.getSerialId());
        sample.setIsQc(isQC);
        sample.setTransmittedFlag("F");

        // SVC.role_cd
        sample.setControlType(getAttributeValue(svc, "SVC.role_cd"));

        // SVC.observation_dttm
        String obsDttm = getAttributeValue(svc, "SVC.observation_dttm");
        if (obsDttm != null && !obsDttm.isEmpty()) {
            sample.setSampleDate(parseDmlDateTime(obsDttm));
        }

        if (isQC) {
            // QC sample - extract CTC (control) info
            sample.setControlLotNum(getAttributeValue(svc, "CTC/CTC.lot_number"));
            sample.setLotLevel(getAttributeValue(svc, "CTC/CTC.level_cd"));
            sample.setInternalExternal("Internal"); // default
        } else {
            // Patient sample - extract PT (patient) info
            String patientId = getAttributeValue(svc, "PT/PT.patient_id");
            String location = getAttributeValue(svc, "PT/PT.location");

            // Parse location "Facility^Location"
            if (location != null && !location.isEmpty()) {
                String[] parts = location.split("\\^");
                if (parts.length > 0) sample.setFacility(parts[0]);
                if (parts.length > 1) sample.setLocation(parts[1]);
            }
            if (sample.getFacility() == null) sample.setFacility(session.getFacility());

            // Determine patient ID type from NTE
            String sampleIdType = findNteValue(svc, "SAMPLE ID TYPE");
            sample.setSampleIdType(sampleIdType);

            // Assign patient IDs based on type
            if ("MRN".equals(sampleIdType)) {
                sample.setMedrecNum(patientId);
            } else if ("ACCT".equals(sampleIdType)) {
                sample.setAccountNum(patientId);
            } else {
                sample.setPatientId(patientId);
            }
        }

        // ORD.order_id -> accession_num
        String accessionNum = getAttributeValue(svc, "ORD/ORD.order_id");
        sample.setAccessionNum(accessionNum);

        // RGT (reagent) info - extract strip lot
        NodeList rgtList = svc.getElementsByTagName("RGT");
        for (int i = 0; i < rgtList.getLength(); i++) {
            Element rgt = (Element) rgtList.item(i);
            String lotType = getAttributeValue(rgt, "RGT.lot_type");
            if ("TestStrip".equals(lotType) || "MT_TS".equals(lotType)) {
                sample.setStripLotNum(getAttributeValue(rgt, "RGT.lot_number"));
            }
        }

        // Store original XML
        sample.setXmlText(elementToXml(svc));

        // Check for duplicate
        if (sample.getSampleDate() != null && session.getSerialId() != null) {
            long existing = sampleDataRepository.countBySampleDateAndDevice(
                    sample.getSampleDate(), session.getSerialId());
            if (existing > 0) {
                log.debug("Duplicate sample detected, skipping insert");
                return;
            }
        }

        // Generate sample key
        sample.setSampleKeyNum(UUID.randomUUID().toString().replace("-", ""));

        // Set device info
        sample.setDeviceType(session.getDeviceType());
        sample.setDeviceName(session.getDeviceName());
        sample.setDeviceSwVer(session.getSwVersion());
        sample.setLocNum(session.getLocationNum());

        // Save sample
        sampleDataRepository.save(sample);
        log.debug("Saved sample with key {} for device {}", sample.getSampleKeyNum(), session.getSerialId());

        // Extract and save individual observations
        NodeList obsNodes = svc.getElementsByTagName("OBS");
        for (int i = 0; i < obsNodes.getLength(); i++) {
            Element obsElem = (Element) obsNodes.item(i);
            saveIndividualObservation(obsElem, sample, session);
        }

        // Delete corresponding order if accession_num exists
        if (accessionNum != null && !accessionNum.isEmpty()) {
            try {
                orderRepository.deleteByAccessionNum(accessionNum);
            } catch (Exception e) {
                log.warn("Failed to delete order with accession_num {}: {}", accessionNum, e.getMessage());
            }
        }
    }

    private void saveIndividualObservation(Element obsElem, DmlSampleData sample, DmlSession session) {
        try {
            DmlObservation obs = new DmlObservation();
            obs.setSampleKeyNum(sample.getSampleKeyNum());
            obs.setAccessionNum(sample.getAccessionNum());
            obs.setPatientId(sample.getPatientId());
            obs.setMrn(sample.getMedrecNum());
            obs.setAccountNum(sample.getAccountNum());
            obs.setControlType(sample.getControlType());
            obs.setControlLotNum(sample.getControlLotNum());
            obs.setStripLotNum(sample.getStripLotNum());
            obs.setObservationDttm(sample.getSampleDate());

            // OBS.observation_id
            obs.setTestCd(getAttributeValue(obsElem, "OBS.observation_id"));
            // OBS.value
            obs.setResultValue(getAttributeValue(obsElem, "OBS.value"));
            // OBS units
            obs.setResultUnits(getAttributeValue(obsElem, "OBS.value", "U"));
            // OBS.interpretation_cd
            obs.setInterpretationCd(getAttributeValue(obsElem, "OBS.interpretation_cd"));
            // OBS.normal_lo-hi_limit
            obs.setNormalLoLimit(getAttributeValue(obsElem, "OBS.normal_lo-hi_limit"));
            obs.setNormalHiLimit(getAttributeValue(obsElem, "OBS.normal_lo-hi_limit", "U"));
            // OBS.critical_lo-hi_limit
            obs.setCriticalLoLimit(getAttributeValue(obsElem, "OBS.critical_lo-hi_limit"));
            obs.setCriticalHiLimit(getAttributeValue(obsElem, "OBS.critical_lo-hi_limit", "U"));
            // OBS.status_cd
            obs.setResultFlags(getAttributeValue(obsElem, "OBS.status_cd"));

            obs.setXmlText(elementToXml(obsElem));
            obs.setTransmittedFlag("F");

            // Find device entity
            if (session.getSerialId() != null) {
                deviceRepository.findBySerialId(session.getSerialId()).ifPresent(obs::setDevice);
            }

            observationRepository.save(obs);
            log.debug("Saved observation for test {} value {}", obs.getTestCd(), obs.getResultValue());
        } catch (Exception e) {
            log.error("Error saving individual observation", e);
        }
    }

    // =========================================================================
    // processEvents - mirrors C# ProcessEvents(XmlNodeReader)
    // =========================================================================

    @Transactional
    public void processEvents(String message, DmlSession session, int evsCount) {
        log.info("Processing {} events for session {}", evsCount, session.getSessionId());
        try {
            Document doc = parseXmlDocument(message);
            Element root = doc.getDocumentElement();

            String controlId = getAttributeValue(root, "HDR/HDR.control_id");
            session.setControlId(controlId);

            NodeList evtList = root.getElementsByTagName("EVT");
            for (int i = 0; i < evtList.getLength(); i++) {
                Element evt = (Element) evtList.item(i);
                processSingleEvent(evt, session);
            }

            session.setWaiting(true);
            sendAcknowledge(session, controlId, false);
        } catch (Exception e) {
            log.error("Error processing events for session {}", session.getSessionId(), e);
            sendAcknowledge(session, session.getControlId(), true);
        }
    }

    private void processSingleEvent(Element evt, DmlSession session) {
        DmlDeviceEvent event = new DmlDeviceEvent();
        event.setDeviceSerialId(session.getSerialId());
        event.setInstNum(session.getInstNum());

        // EVT.description contains description in V attr and event_type in text
        Element descElem = (Element) evt.getElementsByTagName("EVT.description").item(0);
        if (descElem != null) {
            event.setEventDesc(descElem.getAttribute("V"));
            String eventTypeText = descElem.getTextContent();
            // TY=MT -> "M", TY=SE -> "E", else "O"
            if (eventTypeText != null && eventTypeText.contains("TY=MT")) {
                event.setEventType("M");
            } else if (eventTypeText != null && eventTypeText.contains("TY=SE")) {
                event.setEventType("E");
            } else {
                event.setEventType("O");
            }
        }

        // EVT.event_dttm
        String eventDttm = getAttributeValue(evt, "EVT.event_dttm");
        if (eventDttm != null && !eventDttm.isEmpty()) {
            event.setEventDttm(parseDmlDateTime(eventDttm));
        }

        // EVT.severity_cd
        event.setSeverityCd(getAttributeValue(evt, "EVT.severity_cd"));

        // OPR.operator_id
        event.setOperatorId(getAttributeValue(evt, "OPR/OPR.operator_id"));

        // Handle special "OP MSG READ" event
        if ("OP MSG READ".equals(event.getEventDesc())) {
            handleOpMessageRead(event, session);
            return;
        }

        event.setUuid(UUID.randomUUID().toString().replace("-", ""));
        event.setXmlText(elementToXml(evt));

        deviceEventRepository.save(event);
        log.debug("Saved device event: {} at {}", event.getEventDesc(), event.getEventDttm());
    }

    private void handleOpMessageRead(DmlDeviceEvent event, DmlSession session) {
        // In C#, this updates operator_message table.
        // For now, just log it - operator message tracking can be extended later.
        log.info("Operator message read event from operator {} on device {}",
                event.getOperatorId(), session.getSerialId());
    }

    // =========================================================================
    // processSystemStatus - mirrors C# ProcessSystemStatus(string topicName)
    // =========================================================================

    @Transactional
    public void processSystemStatus(String message, DmlSession session, String topicType) {
        log.info("Processing system status '{}' for session {}", topicType, session.getSessionId());
        try {
            Document doc = parseXmlDocument(message);
            Element root = doc.getDocumentElement();

            String controlId = getAttributeValue(root, "HDR/HDR.control_id");
            session.setControlId(controlId);

            boolean hasError = false;

            switch (topicType) {
                case "NOVA.ANALYZER_STATE":
                    NodeList analyzerStates = root.getElementsByTagName("ANALYZER_STATE");
                    hasError = processAnalyzerState(analyzerStates, session);
                    break;
                case "NOVA.CARTRIDGE_STATUS":
                    NodeList cartridgeStatuses = root.getElementsByTagName("CARTRIDGE_STATUS");
                    hasError = processCartridgeStatus(cartridgeStatuses, session);
                    break;
                case "NOVA.TEST_STATUS":
                    NodeList testStatuses = root.getElementsByTagName("TEST_STATUS");
                    hasError = processTestStatus(testStatuses, session);
                    break;
                default:
                    log.warn("Unknown system status topic: {}", topicType);
            }

            sendAcknowledge(session, controlId, hasError);
        } catch (Exception e) {
            log.error("Error processing system status for session {}", session.getSessionId(), e);
            sendAcknowledge(session, session.getControlId(), true);
        }
    }

    private boolean processAnalyzerState(NodeList nodes, DmlSession session) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            String state = getAttributeValue(elem, "ANALYZER_STATE.state");
            log.info("Analyzer state: {} for device {}", state, session.getSerialId());
            // Update device state in database if needed
        }
        return false;
    }

    private boolean processCartridgeStatus(NodeList nodes, DmlSession session) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            String cartridgeId = getAttributeValue(elem, "CARTRIDGE_STATUS.cartridge_id");
            String status = getAttributeValue(elem, "CARTRIDGE_STATUS.status");
            log.info("Cartridge {} status: {} for device {}", cartridgeId, status, session.getSerialId());
        }
        return false;
    }

    private boolean processTestStatus(NodeList nodes, DmlSession session) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            String testId = getAttributeValue(elem, "TEST_STATUS.test_id");
            String status = getAttributeValue(elem, "TEST_STATUS.status");
            log.info("Test {} status: {} for device {}", testId, status, session.getSerialId());
        }
        return false;
    }

    // =========================================================================
    // sendSetup - mirrors C# SendSetup() / SendSetup_meter()
    // =========================================================================

    @Transactional
    public boolean sendSetup(DmlSession session) {
        log.info("Sending setup for session {}", session.getSessionId());
        try {
            String locNum = session.getLocationNum();
            if (locNum == null || locNum.isEmpty()) {
                log.warn("No location number for setup, skipping");
                return false;
            }

            // Check if setup needs to be sent (loc_last_update)
            boolean shouldSend = shouldSendData(locNum, "SETUP", session);
            if (!shouldSend) {
                log.debug("Setup data is up-to-date, not sending");
                return false;
            }

            // Load config data for this location
            List<DmlConfigData> configData = configDataRepository.findByLocNum(locNum);

            // Load test configuration for this device type
            List<DmlInstrumentTest> testConfigs = session.getDeviceType() != null ?
                    instrumentTestRepository.findByInstTypeOrdered(session.getDeviceType()) :
                    Collections.emptyList();

            // Build and send setup message
            String setupMsg = messageBuilder.buildSetupMessage(session, configData, testConfigs);
            session.setWaiting(true);
            session.sendMessage(setupMsg);
            return true;
        } catch (Exception e) {
            log.error("Error sending setup for session {}", session.getSessionId(), e);
            return false;
        }
    }

    // =========================================================================
    // sendWifiSetup - mirrors C# SendWifiSetup()
    // =========================================================================

    @Transactional
    public boolean sendWifiSetup(DmlSession session) {
        log.info("Sending WiFi setup for session {}", session.getSessionId());
        try {
            String locNum = session.getLocationNum();
            if (locNum == null || locNum.isEmpty()) {
                return false;
            }

            boolean shouldSend = shouldSendData(locNum, "WIFI_SETUP", session);
            if (!shouldSend) {
                return false;
            }

            // Find WiFi setup config for this location
            String instClass = session.getDeviceClass();
            List<DmlWifiSetup> wifiSetups = wifiSetupRepository.findByLocNumAndInstClass(locNum, instClass);
            if (wifiSetups.isEmpty()) {
                log.debug("No WiFi setup found for loc {} class {}", locNum, instClass);
                return false;
            }

            DmlWifiSetup wifiSetup = wifiSetups.get(0);
            String wifiData = wifiSetup.getWifiData();
            if (wifiData == null || wifiData.isEmpty()) {
                return false;
            }

            // Get WiFi credentials (try 1FacAnd1Location, then 1FacAndAllLocation, then AllFac)
            String macAddress = session.getFromInstId() != null ? session.getFromInstId() : "";
            String[] credentials = getWifiCredentials(locNum, session.getFacility(), macAddress);
            String userName = credentials[0];
            String password = credentials[1];

            // Build message with injected credentials
            String wifiMsg = messageBuilder.buildWifiSetupMessage(session, wifiData, userName, password);
            session.setWaiting(true);
            session.sendMessage(wifiMsg);
            return true;
        } catch (Exception e) {
            log.error("Error sending WiFi setup for session {}", session.getSessionId(), e);
            return false;
        }
    }

    private String[] getWifiCredentials(String locNum, String facNum, String macAddress) {
        String userName = "";
        String password = "";

        // Try 1FacAnd1Location
        List<DmlWifiCredential> creds = wifiCredentialRepository.findByFacilityAndLocationAndMac(facNum, locNum, macAddress);
        if (!creds.isEmpty()) {
            userName = creds.get(0).getWifiUserName();
            password = creds.get(0).getWifiPassword();
        }
        if (userName.isEmpty() && password.isEmpty()) {
            // Try 1FacAndAllLocation
            creds = wifiCredentialRepository.findByFacilityAllLocationsAndMac(facNum, macAddress);
            if (!creds.isEmpty()) {
                userName = creds.get(0).getWifiUserName();
                password = creds.get(0).getWifiPassword();
            }
        }
        if (userName.isEmpty() && password.isEmpty()) {
            // Try AllFac
            creds = wifiCredentialRepository.findAllFacilitiesAndMac(macAddress);
            if (!creds.isEmpty()) {
                userName = creds.get(0).getWifiUserName();
                password = creds.get(0).getWifiPassword();
            }
        }
        return new String[]{userName != null ? userName : "", password != null ? password : ""};
    }

    // =========================================================================
    // sendWifiCert - mirrors C# SendWifiCert()
    // =========================================================================

    @Transactional
    public boolean sendWifiCert(DmlSession session) {
        log.info("Sending WiFi cert for session {}", session.getSessionId());
        try {
            String locNum = session.getLocationNum();
            if (locNum == null || locNum.isEmpty()) {
                return false;
            }

            boolean shouldSend = shouldSendData(locNum, "WIFI_CERT", session);
            if (!shouldSend) {
                return false;
            }

            // For now, send a standard cert message
            // In production, load actual cert from database
            String certMsg = messageBuilder.buildWifiCertMessage(session, null);
            session.setWaiting(true);
            session.sendMessage(certMsg);
            return true;
        } catch (Exception e) {
            log.error("Error sending WiFi cert for session {}", session.getSessionId(), e);
            return false;
        }
    }

    // =========================================================================
    // sendLocationList - mirrors C# SendLocationList()
    // =========================================================================

    @Transactional
    public boolean sendLocationList(DmlSession session) {
        log.info("Sending location list for session {}", session.getSessionId());
        try {
            String locNum = session.getLocationNum();
            if (locNum == null || locNum.isEmpty()) {
                return false;
            }

            boolean shouldSend = shouldSendData(locNum, "LOCATIONS", session);
            if (!shouldSend) {
                return false;
            }

            // Load all facilities (level 1)
            List<DmlLocation> facilities = locationRepository.findAllFacilities();
            // Load all locations (level 2)
            List<DmlLocation> units = locationRepository.findByLevelNum(2);

            // Build and send message (supports partial if too large)
            String locMsg = messageBuilder.buildLocationListMessage(session, facilities, units);
            session.setWaiting(true);
            session.sendMessage(locMsg);
            return true;
        } catch (Exception e) {
            log.error("Error sending location list for session {}", session.getSessionId(), e);
            return false;
        }
    }

    // =========================================================================
    // sendOperatorList - mirrors C# SendOperatorList()
    // =========================================================================

    @Transactional
    public boolean sendOperatorList(DmlSession session) {
        log.info("Sending operator list for session {}", session.getSessionId());
        try {
            String locNum = session.getLocationNum();
            if (locNum == null || locNum.isEmpty()) {
                return false;
            }

            boolean shouldSend = shouldSendData(locNum, "OPERATORS", session);
            if (!shouldSend) {
                return false;
            }

            // Load active operators
            List<DmlOperator> operators = operatorRepository.findAllActive();

            // Send in batches if needed (supports partial messages)
            if (operators.size() <= MAX_ITEMS_PER_MSG) {
                String opMsg = messageBuilder.buildOperatorListMessage(session, operators, false);
                session.setWaiting(true);
                session.sendMessage(opMsg);
            } else {
                // Send in chunks
                for (int i = 0; i < operators.size(); i += MAX_ITEMS_PER_MSG) {
                    int end = Math.min(i + MAX_ITEMS_PER_MSG, operators.size());
                    List<DmlOperator> chunk = operators.subList(i, end);
                    boolean isPartial = end < operators.size();
                    String opMsg = messageBuilder.buildOperatorListMessage(session, chunk, isPartial);
                    session.setWaiting(true);
                    session.sendMessage(opMsg);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error sending operator list for session {}", session.getSessionId(), e);
            return false;
        }
    }

    // =========================================================================
    // sendPatientList - mirrors C# SendPatientList()
    // =========================================================================

    @Transactional
    public boolean sendPatientList(DmlSession session) {
        log.info("Sending patient list for session {}", session.getSessionId());
        try {
            String locNum = session.getLocationNum();
            if (locNum == null || locNum.isEmpty()) {
                return false;
            }

            boolean shouldSend = shouldSendData(locNum, "PATIENTS", session);
            if (!shouldSend) {
                return false;
            }

            // Load active patients
            List<DmlPatient> patients = patientRepository.findAllActive();

            // Send in batches
            if (patients.size() <= MAX_ITEMS_PER_MSG) {
                String ptMsg = messageBuilder.buildPatientListMessage(session, patients, false);
                session.setWaiting(true);
                session.sendMessage(ptMsg);
            } else {
                for (int i = 0; i < patients.size(); i += MAX_ITEMS_PER_MSG) {
                    int end = Math.min(i + MAX_ITEMS_PER_MSG, patients.size());
                    List<DmlPatient> chunk = patients.subList(i, end);
                    boolean isPartial = end < patients.size();
                    String ptMsg = messageBuilder.buildPatientListMessage(session, chunk, isPartial);
                    session.setWaiting(true);
                    session.sendMessage(ptMsg);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error sending patient list for session {}", session.getSessionId(), e);
            return false;
        }
    }

    // =========================================================================
    // sendPhysicianList - mirrors C# SendPhysicianList()
    // =========================================================================

    @Transactional
    public boolean sendPhysicianList(DmlSession session) {
        log.info("Sending physician list for session {}", session.getSessionId());
        try {
            String locNum = session.getLocationNum();
            if (locNum == null || locNum.isEmpty()) {
                return false;
            }

            boolean shouldSend = shouldSendData(locNum, "PHYSICIANS", session);
            if (!shouldSend) {
                return false;
            }

            // Load active physicians
            List<DmlPhysician> physicians = physicianRepository.findAllActive();

            // Send in batches
            if (physicians.size() <= MAX_ITEMS_PER_MSG) {
                String physMsg = messageBuilder.buildPhysicianListMessage(session, physicians, false);
                session.setWaiting(true);
                session.sendMessage(physMsg);
            } else {
                for (int i = 0; i < physicians.size(); i += MAX_ITEMS_PER_MSG) {
                    int end = Math.min(i + MAX_ITEMS_PER_MSG, physicians.size());
                    List<DmlPhysician> chunk = physicians.subList(i, end);
                    boolean isPartial = end < physicians.size();
                    String physMsg = messageBuilder.buildPhysicianListMessage(session, chunk, isPartial);
                    session.setWaiting(true);
                    session.sendMessage(physMsg);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error sending physician list for session {}", session.getSessionId(), e);
            return false;
        }
    }

    // =========================================================================
    // sendFirmware - mirrors C# SendFirmware()
    // =========================================================================

    @Transactional
    public boolean sendFirmware(DmlSession session) {
        log.info("Sending firmware for session {}", session.getSessionId());
        try {
            String deviceType = session.getDeviceType();
            if (deviceType == null || deviceType.isEmpty()) {
                return false;
            }

            // Find latest firmware for this device type
            List<DmlFirmware> firmwares = firmwareRepository.findByDeviceTypeAndStatus(deviceType, "Active");
            if (firmwares.isEmpty()) {
                log.debug("No firmware found for device type {}", deviceType);
                return false;
            }

            DmlFirmware firmware = firmwares.get(0);
            String frmMsg = messageBuilder.buildFirmwareMessage(session, firmware);
            session.setWaiting(true);
            session.sendMessage(frmMsg);
            return true;
        } catch (Exception e) {
            log.error("Error sending firmware for session {}", session.getSessionId(), e);
            return false;
        }
    }

    // =========================================================================
    // sendReagents - mirrors C# SendReagents()
    // =========================================================================

    @Transactional
    public boolean sendReagents(DmlSession session) {
        log.info("Sending reagent list for session {}", session.getSessionId());
        try {
            String locNum = session.getLocationNum();
            if (locNum == null || locNum.isEmpty()) {
                return false;
            }

            boolean shouldSend = shouldSendData(locNum, "LOTS", session);
            if (!shouldSend) {
                return false;
            }

            // Find lots for this device type
            String instType = session.getDeviceType();
            List<DmlLot> lots = lotRepository.findLotsForDevice(instType);
            if (lots.isEmpty()) {
                // Send empty reagent message
                String reagMsg = messageBuilder.buildReagentMessage(session, Collections.emptyList(), Collections.emptyMap());
                session.setWaiting(true);
                session.sendMessage(reagMsg);
                return true;
            }

            // Load chemistry data for each lot
            Map<String, List<DmlLotChem>> lotChemMap = new HashMap<>();
            for (DmlLot lot : lots) {
                if (lot.getLotsKeyNum() != null) {
                    List<DmlLotChem> chems = lotChemRepository.findByLotsKeyNum(lot.getLotsKeyNum());
                    lotChemMap.put(lot.getLotsKeyNum(), chems);
                }
            }

            // Build and send reagent message
            String reagMsg = messageBuilder.buildReagentMessage(session, lots, lotChemMap);
            session.setWaiting(true);
            session.sendMessage(reagMsg);
            return true;
        } catch (Exception e) {
            log.error("Error sending reagents for session {}", session.getSessionId(), e);
            return false;
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Check if data needs to be sent based on last update time.
     * Mirrors C# logic: check loc_last_update table.
     */
    private boolean shouldSendData(String locNum, String dataType, DmlSession session) {
        try {
            // Always send if session forces it
            // For now, default to true (send data)
            // In production, check loc_last_update for changes since last update
            long count = locLastUpdateRepository.countUpdatesSince(locNum, dataType,
                    LocalDateTime.now().minusYears(1));
            return count > 0 || true; // Default: always send
        } catch (Exception e) {
            log.debug("Error checking shouldSendData, defaulting to true: {}", e.getMessage());
            return true;
        }
    }

    private void sendAcknowledge(DmlSession session, String controlId, boolean isError) {
        try {
            String ackMsg = messageBuilder.buildAckMessage(controlId, isError ? "AE" : "AA", session);
            session.sendMessage(ackMsg);
        } catch (Exception e) {
            log.error("Error sending ACK", e);
        }
    }

    private Document parseXmlDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
    }

    private String getAttributeValue(Element parent, String path) {
        return getAttributeValue(parent, path, "V");
    }

    /**
     * Get attribute value from a path like "HDR/HDR.control_id" or "SVC.role_cd".
     */
    private String getAttributeValue(Element parent, String path, String attrName) {
        try {
            String[] parts = path.split("/");
            Element current = parent;
            for (int i = 0; i < parts.length; i++) {
                NodeList nodes = current.getElementsByTagName(parts[i]);
                if (nodes.getLength() == 0) return null;
                current = (Element) nodes.item(0);
            }
            String value = current.getAttribute(attrName);
            return value != null && !value.isEmpty() ? value : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find NTE element value by its V attribute.
     */
    private String findNteValue(Element parent, String searchText) {
        NodeList nteList = parent.getElementsByTagName("NTE");
        for (int i = 0; i < nteList.getLength(); i++) {
            Element nte = (Element) nteList.item(i);
            NodeList textNodes = nte.getElementsByTagName("NTE.text");
            for (int j = 0; j < textNodes.getLength(); j++) {
                Element textElem = (Element) textNodes.item(j);
                if (searchText.equals(textElem.getAttribute("V"))) {
                    return textElem.getTextContent().trim();
                }
            }
        }
        return null;
    }

    private LocalDateTime parseDmlDateTime(String dttm) {
        if (dttm == null || dttm.isEmpty()) return null;
        try {
            // Try ISO format with timezone
            return LocalDateTime.parse(dttm, DML_DTTM_FORMAT);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(dttm, DB_DTTM_FORMAT);
            } catch (Exception e2) {
                try {
                    java.util.Calendar cal = DatatypeConverter.parseDateTime(dttm);
                    return LocalDateTime.ofInstant(cal.toInstant(), java.time.ZoneId.systemDefault());
                } catch (Exception e3) {
                    log.warn("Could not parse date: {}", dttm);
                    return null;
                }
            }
        }
    }

    private String elementToXml(Element elem) {
        try {
            javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
            java.io.StringWriter writer = new java.io.StringWriter();
            transformer.transform(new javax.xml.transform.dom.DOMSource(elem),
                    new javax.xml.transform.stream.StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
