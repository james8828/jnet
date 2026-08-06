package com.nova.bioconnect.icpmgr.protocol;

import com.nova.bioconnect.icpmgr.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * DML Message Builder
 * Constructs DML XML messages according to protocol specification.
 * Reference: DML_Novanet Interface Specs and C# DMLProtocol.cs
 */
@Component
public class DmlMessageBuilder {

    private static final DateTimeFormatter DML_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final DateTimeFormatter DML_DATE_SIMPLE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter EXP_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // =========================================================================
    // ACK / ESC / EOT messages
    // =========================================================================

    public String buildAckMessage(String controlId, String typeCd, DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ACK.R01>");
        sb.append(genHeader(session));
        sb.append("<ACK>");
        sb.append("<ACK.ack_control_id V=\"").append(escapeXml(controlId)).append("\"/>");
        sb.append("<ACK.type_cd V=\"").append(typeCd).append("\"/>");
        sb.append("</ACK>");
        sb.append("</ACK.R01>");
        return sb.toString();
    }

    public String buildEscapeMessage(String reason, DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ESC.R01>");
        sb.append(genHeader(session));
        sb.append("<ESC>");
        sb.append("<ESC.reason V=\"").append(escapeXml(reason)).append("\"/>");
        sb.append("</ESC>");
        sb.append("</ESC.R01>");
        return sb.toString();
    }

    public String buildEotMessage(String topic, DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<EOT.R01>");
        sb.append(genHeader(session));
        sb.append("<EOT>");
        sb.append("<EOT.topic_cd V=\"").append(topic).append("\"/>");
        sb.append("<EOT.update_dttm V=\"").append(getCurrentDmlTime()).append("\"/>");
        sb.append("</EOT>");
        sb.append("</EOT.R01>");
        return sb.toString();
    }

    // =========================================================================
    // Request messages (ROBS, RDEV)
    // =========================================================================

    public String buildObservationRequest(DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ROBS.R01>");
        sb.append(genHeader(session));
        sb.append("<ROBS>");
        sb.append("<ROBS.request_cd V=\"NEW\"/>");
        sb.append("</ROBS>");
        sb.append("</ROBS.R01>");
        return sb.toString();
    }

    public String buildEventRequest(DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<RDEV.R01>");
        sb.append(genHeader(session));
        sb.append("<RDEV>");
        sb.append("<RDEV.request_cd V=\"NEW\"/>");
        sb.append("</RDEV>");
        sb.append("</RDEV.R01>");
        return sb.toString();
    }

    // =========================================================================
    // Set time / Continuous / Terminate / Keep alive
    // =========================================================================

    public String buildSetTimeMessage(DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<SET_TIME.R01>");
        sb.append(genHeader(session));
        sb.append("<SET_TIME>");
        sb.append("<SET_TIME.current_dttm V=\"").append(getCurrentDmlTime()).append("\"/>");
        sb.append("</SET_TIME>");
        sb.append("</SET_TIME.R01>");
        return sb.toString();
    }

    public String buildContinuousMessage(DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<DTV.R01>");
        sb.append(genHeader(session));
        sb.append("<DTV>");
        sb.append("<DTV.command_cd V=\"START_CONTINUOUS\"/>");
        sb.append("</DTV>");
        sb.append("</DTV.R01>");
        return sb.toString();
    }

    public String buildTerminateMessage(String reason, String extra, DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<TERM.R01>");
        sb.append(genHeader(session));
        sb.append("<TERM>");
        sb.append("<TERM.reason V=\"").append(escapeXml(reason)).append("\"/>");
        if (extra != null && !extra.isEmpty()) {
            sb.append("<TERM.extra V=\"").append(escapeXml(extra)).append("\"/>");
        }
        sb.append("</TERM>");
        sb.append("</TERM.R01>");
        return sb.toString();
    }

    public String buildKeepAliveMessage(DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<KPA.R01>");
        sb.append(genHeader(session));
        sb.append("<KPA/>");
        sb.append("</KPA.R01>");
        return sb.toString();
    }

    // =========================================================================
    // Setup messages (NOVA.STATSTRIP.SETUP)
    // Mirrors C# SendSetup_meter()
    // =========================================================================

    /**
     * Build StatStrip setup message with key-value pairs and test config.
     */
    public String buildSetupMessage(DmlSession session,
                                    List<DmlConfigData> configValues,
                                    List<DmlInstrumentTest> testConfigs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<NOVA.STATSTRIP.SETUP>");
        sb.append(genHeader(session));

        // KEY_VALUE section
        sb.append("<KEY_VALUE>");
        if (configValues != null && !configValues.isEmpty()) {
            for (DmlConfigData cfg : configValues) {
                appendKeyValue(sb, cfg.getConfigKey(), cfg.getConfigValue());
            }
        } else {
            sb.append("none");
        }
        // Append facility/location if available
        if (session.getFacility() != null && !session.getFacility().isEmpty()) {
            sb.append("<Facility V=\"").append(escapeXml(session.getFacility())).append("\"/>");
        }
        sb.append("</KEY_VALUE>");

        // TEST_CONFIG section
        sb.append("<TEST_CONFIG>");
        if (testConfigs != null) {
            for (DmlInstrumentTest tc : testConfigs) {
                appendTestConfig(sb, tc);
            }
        }
        sb.append("</TEST_CONFIG>");

        // QC_CONFIG (stub - can be extended)
        sb.append("<QC_CONFIG/>");

        // DIAGCODES (stub)
        sb.append("<DIAGCODES/>");

        sb.append("</NOVA.STATSTRIP.SETUP>");
        return sb.toString();
    }

    private void appendKeyValue(StringBuilder sb, String key, String value) {
        if (key == null || key.isEmpty()) return;
        // C# splits key by '*' to get element name and attribute name
        String[] parts = key.split("\\*", 2);
        String elemName = parts[0];
        String attrName = parts.length > 1 ? parts[1] : "V";
        sb.append("<").append(elemName);
        if (value != null) {
            sb.append(" ").append(attrName).append("=\"").append(escapeXml(value)).append("\"");
        }
        sb.append("/>");
    }

    private void appendTestConfig(StringBuilder sb, DmlInstrumentTest tc) {
        sb.append("<TST>");
        sb.append("<TST.test_name V=\"").append(escapeXml(tc.getTestName())).append("\"/>");
        if (tc.getTestCode() != null) {
            sb.append("<TST.test_cd V=\"").append(escapeXml(tc.getTestCode())).append("\"");
            if (tc.getTestCodeSystem() != null) {
                sb.append(" SN=\"").append(escapeXml(tc.getTestCodeSystem())).append("\"");
            }
            sb.append("/>");
        }
        if (tc.getUnits() != null) {
            sb.append("<TST.units V=\"").append(escapeXml(tc.getUnits())).append("\"/>");
        }
        if (tc.getLoLimit() != null && tc.getHiLimit() != null) {
            sb.append("<TST.lo-hi_limit V=\"[").append(tc.getLoLimit()).append(";")
              .append(tc.getHiLimit()).append("]\" U=\"").append(escapeXml(tc.getUnits() != null ? tc.getUnits() : "")).append("\"/>");
        }
        if (tc.getLoPanicLimit() != null && tc.getHiPanicLimit() != null) {
            sb.append("<TST.critical_lo-hi_limit V=\"[").append(tc.getLoPanicLimit()).append(";")
              .append(tc.getHiPanicLimit()).append("]\" U=\"").append(escapeXml(tc.getUnits() != null ? tc.getUnits() : "")).append("\"/>");
        }
        sb.append("</TST>");
    }

    // =========================================================================
    // WiFi Setup message (NOVA.WIFI_SETUP.R01)
    // Mirrors C# SendWifiSetup()
    // =========================================================================

    /**
     * Build WiFi setup message with injected username/password.
     */
    public String buildWifiSetupMessage(DmlSession session, String wifiDataXml,
                                        String userName, String password) {
        String content = wifiDataXml;
        if (userName != null && !userName.isEmpty()) {
            content = replaceXmlElement(content, "<userName>", "</userName>", userName);
        }
        if (password != null && !password.isEmpty()) {
            content = replaceXmlElement(content, "<password>", "</password>", password);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<NOVA.WIFI_SETUP.R01>");
        sb.append(genHeader(session));
        sb.append(content);
        sb.append("</NOVA.WIFI_SETUP.R01>");
        return sb.toString();
    }

    /**
     * Build WiFi certificate message.
     */
    public String buildWifiCertMessage(DmlSession session, String certData) {
        StringBuilder sb = new StringBuilder();
        sb.append("<NOVA.WIFI_CERT.R01>");
        sb.append(genHeader(session));
        sb.append("<WIFI_CERT>");
        if (certData != null && !certData.isEmpty()) {
            sb.append("<WIFI_CERT.data ENC=\"B64\">");
            sb.append(certData);
            sb.append("</WIFI_CERT.data>");
        }
        sb.append("</WIFI_CERT>");
        sb.append("</NOVA.WIFI_CERT.R01>");
        return sb.toString();
    }

    // =========================================================================
    // Location list message (NOVA.LOC.R01)
    // Mirrors C# SendLocationList()
    // =========================================================================

    /**
     * Build location list message from location hierarchy.
     */
    public String buildLocationListMessage(DmlSession session, List<DmlLocation> facilities) {
        StringBuilder sb = new StringBuilder();
        sb.append("<NOVA.LOC.R01>");
        sb.append(genHeader(session));

        if (facilities != null) {
            for (DmlLocation facility : facilities) {
                sb.append("<LOC>");
                sb.append("<LOC.facility V=\"").append(escapeXml(facility.getFacility())).append("\">");
                // Find child locations of this facility
                // Note: caller should group locations by facility
                sb.append("</LOC.facility>");
                sb.append("</LOC>");
            }
        }
        sb.append("</NOVA.LOC.R01>");
        return sb.toString();
    }

    /**
     * Build location list with facility and units (full version).
     */
    public String buildLocationListMessage(DmlSession session,
                                            List<DmlLocation> facilities,
                                            List<DmlLocation> units) {
        StringBuilder sb = new StringBuilder();
        sb.append("<NOVA.LOC.R01>");
        sb.append(genHeader(session));

        if (facilities != null) {
            for (DmlLocation facility : facilities) {
                sb.append("<LOC>");
                sb.append("<LOC.facility V=\"").append(escapeXml(facility.getFacility() != null ? facility.getFacility() : facility.getLocName())).append("\">");
                if (units != null) {
                    for (DmlLocation unit : units) {
                        if (facility.getLocNum().equals(unit.getParentLocNum())) {
                            String df = "T".equalsIgnoreCase(unit.getIsDefault()) ? "T" : "F";
                            sb.append("<unit V=\"").append(escapeXml(unit.getLocName())).append("\" DF=\"").append(df).append("\"/>");
                        }
                    }
                }
                sb.append("</LOC.facility>");
                sb.append("</LOC>");
            }
        }
        sb.append("</NOVA.LOC.R01>");
        return sb.toString();
    }

    // =========================================================================
    // Operator list message (OP_LST.R01)
    // Mirrors C# SendOperatorList()
    // =========================================================================

    public String buildOperatorListMessage(DmlSession session, List<DmlOperator> operators,
                                            boolean isPartial) {
        StringBuilder sb = new StringBuilder();
        sb.append(isPartial ? "<OP_LST.R02>" : "<OP_LST.R01>");
        sb.append(genHeader(session));

        if (operators != null) {
            for (DmlOperator op : operators) {
                sb.append("<OPR>");
                sb.append("<OPR.operator_id V=\"").append(escapeXml(op.getOperatorId())).append("\"/>");
                if (op.getOperatorName() != null && !op.getOperatorName().isEmpty()) {
                    sb.append("<OPR.name V=\"").append(escapeXml(op.getOperatorName())).append("\">");
                    if (op.getFirstName() != null) {
                        sb.append("<GIV V=\"").append(escapeXml(op.getFirstName())).append("\"/>");
                    }
                    if (op.getLastName() != null) {
                        sb.append("<FAM V=\"").append(escapeXml(op.getLastName())).append("\"/>");
                    }
                    sb.append("</OPR.name>");
                }
                if (op.getAccessControlLevel() != null) {
                    sb.append("<OPR.access_control_level V=\"").append(op.getAccessControlLevel()).append("\"/>");
                }
                sb.append("</OPR>");
            }
        }
        sb.append(isPartial ? "</OP_LST.R02>" : "</OP_LST.R01>");
        return sb.toString();
    }

    // =========================================================================
    // Patient list message (PT_LST.R01)
    // Mirrors C# SendPatientList()
    // =========================================================================

    public String buildPatientListMessage(DmlSession session, List<DmlPatient> patients,
                                           boolean isPartial) {
        StringBuilder sb = new StringBuilder();
        sb.append(isPartial ? "<PT_LST.R02>" : "<PT_LST.R01>");
        sb.append(genHeader(session));

        if (patients != null) {
            for (DmlPatient p : patients) {
                sb.append("<PT>");
                sb.append("<PT.patient_id V=\"").append(escapeXml(p.getPatientId())).append("\"/>");
                if (p.getMedrecNum() != null) {
                    sb.append("<PT.medrec_num V=\"").append(escapeXml(p.getMedrecNum())).append("\"/>");
                }
                if (p.getAccountNum() != null) {
                    sb.append("<PT.account_num V=\"").append(escapeXml(p.getAccountNum())).append("\"/>");
                }
                if (p.getPatientName() != null && !p.getPatientName().isEmpty()) {
                    sb.append("<PT.name V=\"").append(escapeXml(p.getPatientName())).append("\">");
                    if (p.getFirstName() != null) {
                        sb.append("<GIV V=\"").append(escapeXml(p.getFirstName())).append("\"/>");
                    }
                    if (p.getLastName() != null) {
                        sb.append("<FAM V=\"").append(escapeXml(p.getLastName())).append("\"/>");
                    }
                    sb.append("</PT.name>");
                }
                if (p.getBirthDate() != null) {
                    sb.append("<PT.birth_date V=\"").append(p.getBirthDate()).append("\"/>");
                }
                if (p.getSex() != null) {
                    sb.append("<PT.sex V=\"").append(p.getSex()).append("\"/>");
                }
                if (p.getLocation() != null) {
                    sb.append("<PT.location V=\"").append(escapeXml(p.getLocation())).append("\"/>");
                }
                sb.append("</PT>");
            }
        }
        sb.append(isPartial ? "</PT_LST.R02>" : "</PT_LST.R01>");
        return sb.toString();
    }

    // =========================================================================
    // Physician list message (NOVA.PHYS.R01)
    // Mirrors C# SendPhysicianList()
    // =========================================================================

    public String buildPhysicianListMessage(DmlSession session, List<DmlPhysician> physicians,
                                             boolean isPartial) {
        StringBuilder sb = new StringBuilder();
        sb.append(isPartial ? "<NOVA.PHYS.R02>" : "<NOVA.PHYS.R01>");
        sb.append(genHeader(session));

        if (physicians != null) {
            for (DmlPhysician ph : physicians) {
                sb.append("<PHYS>");
                sb.append("<PHYS.physician_id V=\"").append(escapeXml(ph.getPhysicianId())).append("\"/>");
                if (ph.getPhysicianName() != null && !ph.getPhysicianName().isEmpty()) {
                    sb.append("<PHYS.name V=\"").append(escapeXml(ph.getPhysicianName())).append("\">");
                    if (ph.getFirstName() != null) {
                        sb.append("<GIV V=\"").append(escapeXml(ph.getFirstName())).append("\"/>");
                    }
                    if (ph.getLastName() != null) {
                        sb.append("<FAM V=\"").append(escapeXml(ph.getLastName())).append("\"/>");
                    }
                    if (ph.getPrefix() != null) {
                        sb.append("<PFX V=\"").append(escapeXml(ph.getPrefix())).append("\"/>");
                    }
                    if (ph.getSuffix() != null) {
                        sb.append("<SFX V=\"").append(escapeXml(ph.getSuffix())).append("\"/>");
                    }
                    sb.append("</PHYS.name>");
                }
                sb.append("</PHYS>");
            }
        }
        sb.append(isPartial ? "</NOVA.PHYS.R02>" : "</NOVA.PHYS.R01>");
        return sb.toString();
    }

    // =========================================================================
    // Firmware message (NOVA.FRM.R01)
    // Mirrors C# SendFirmware()
    // =========================================================================

    public String buildFirmwareMessage(DmlSession session, DmlFirmware firmware) {
        StringBuilder sb = new StringBuilder();
        sb.append("<NOVA.FRM.R01>");
        sb.append(genHeader(session));
        sb.append("<FRM>");
        if (firmware != null && firmware.getFirmwareData() != null) {
            sb.append("<FRM.data ENC=\"B64\">");
            sb.append(firmware.getFirmwareData());
            sb.append("</FRM.data>");
            if (firmware.getFileName() != null) {
                sb.append("<FRM.file_name V=\"").append(escapeXml(firmware.getFileName())).append("\"/>");
            }
        }
        sb.append("</FRM>");
        sb.append("</NOVA.FRM.R01>");
        return sb.toString();
    }

    // =========================================================================
    // Reagent message (NOVA.REAG.R01)
    // Mirrors C# SendReagents()
    // =========================================================================

    /**
     * Build reagent list message from lots and their chemistry data.
     */
    public String buildReagentMessage(DmlSession session,
                                      List<DmlLot> lots,
                                      java.util.Map<String, List<DmlLotChem>> lotChemMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("<NOVA.REAG.R01>");
        sb.append(genHeader(session));

        if (lots != null) {
            for (DmlLot lot : lots) {
                sb.append("<LOT>");
                sb.append("<LOT.lot_number V=\"").append(escapeXml(lot.getLot())).append("\"/>");
                String lotTypeCd = mapLotTypeToCode(lot.getLotType());
                sb.append("<LOT.type V=\"").append(lotTypeCd).append("\"/>");
                if (lot.getExpDate() != null) {
                    sb.append("<LOT.expiration_dttm V=\"").append(lot.getExpDate().atStartOfDay().format(EXP_DATE_FORMAT)).append("\"/>");
                }
                if (lot.getLotName() != null && !lot.getLotName().isEmpty()) {
                    sb.append("<LOT.lot_name V=\"").append(escapeXml(lot.getLotName())).append("\"/>");
                }

                // Add chemistry/range data
                if (lotChemMap != null && lot.getLotsKeyNum() != null) {
                    List<DmlLotChem> chems = lotChemMap.get(lot.getLotsKeyNum());
                    if (chems != null) {
                        for (DmlLotChem chem : chems) {
                            if (chem.getLevelNumber() != null && !chem.getLevelNumber().isEmpty()) {
                                sb.append("<Level>");
                                sb.append("<Level.number V=\"").append(escapeXml(chem.getLevelNumber())).append("\"/>");
                                String levelType = chem.getLevelType() != null ?
                                        mapLotTypeToCode(chem.getLevelType()) : "QC";
                                sb.append("<Level.type V=\"").append(levelType).append("\"/>");
                                if (chem.getObservationId() != null && !chem.getObservationId().isEmpty()) {
                                    sb.append("<TST>");
                                    sb.append("<TST.observation_id V=\"").append(escapeXml(chem.getObservationId())).append("\"/>");
                                    if (chem.getLoLimit() != null && chem.getHiLimit() != null) {
                                        sb.append("<TST.lo-hi_limit V=\"[").append(chem.getLoLimit())
                                          .append(";").append(chem.getHiLimit()).append("]\" U=\"")
                                          .append(escapeXml(chem.getUnits() != null ? chem.getUnits() : "")).append("\"/>");
                                    }
                                    sb.append("</TST>");
                                }
                                sb.append("</Level>");
                            }
                        }
                    }
                }
                sb.append("</LOT>");
            }
        }
        sb.append("</NOVA.REAG.R01>");
        return sb.toString();
    }

    private String mapLotTypeToCode(String lotType) {
        if (lotType == null) return "QC";
        return switch (lotType) {
            case "Reagent" -> "RG";
            case "Linearity" -> "LN";
            case "TestStrip" -> "TS";
            case "Proficiency", "PRO" -> "PRO";
            default -> lotType.startsWith("MT") ? lotType : "QC";
        };
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private String genHeader(DmlSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("<HDR>");
        String controlId = session.getControlId();
        sb.append("<HDR.control_id V=\"").append(controlId != null ? controlId : "").append("\"/>");
        sb.append("<HDR.sequence_num V=\"1\"/>");
        sb.append("<HDR.message_dttm V=\"").append(getCurrentDmlTime()).append("\"/>");
        if (session.getVendorId() != null && !session.getVendorId().isEmpty()) {
            sb.append("<HDR.vendor_id V=\"").append(session.getVendorId()).append("\"/>");
        }
        sb.append("</HDR>");
        return sb.toString();
    }

    private String getCurrentDmlTime() {
        return LocalDateTime.now().format(DML_DATE_FORMAT);
    }

    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Replace XML element content (mirrors C# ReplaceXMLElement).
     */
    private String replaceXmlElement(String xmlString, String startElement, String endElement, String newValue) {
        if (xmlString == null || newValue == null) return xmlString;
        int i = xmlString.toLowerCase().indexOf(startElement.toLowerCase());
        int j = xmlString.toLowerCase().indexOf(endElement.toLowerCase());
        if (i >= 0 && j > i) {
            String before = xmlString.substring(0, i + startElement.length());
            String after = xmlString.substring(j);
            return before + escapeXml(newValue) + after;
        }
        return xmlString;
    }
}
