package com.nova.bioconnect.device.protocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DML Message Parser
 * Parses DML XML messages and extracts key fields
 */
@Slf4j
@Component
public class DmlMessageParser {

    /**
     * Parse the message type from DML XML message
     */
    public String parseMessageType(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Find the first XML element name (message type)
        int start = message.indexOf('<');
        int end = message.indexOf('>', start);
        if (start >= 0 && end > start) {
            String tag = message.substring(start + 1, end);
            // Remove any attributes
            int spaceIdx = tag.indexOf(' ');
            if (spaceIdx > 0) {
                tag = tag.substring(0, spaceIdx);
            }
            // Remove any closing slash
            if (tag.endsWith("/")) {
                tag = tag.substring(0, tag.length() - 1);
            }
            return tag;
        }
        return "";
    }

    /**
     * Parse control_id from DML XML message
     */
    public String parseControlId(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Look for HDR.control_id element
        String pattern = "HDR.control_id V=\"";
        int idx = message.indexOf(pattern);
        if (idx >= 0) {
            int start = idx + pattern.length();
            int end = message.indexOf('"', start);
            if (end > start) {
                return message.substring(start, end);
            }
        }
        return "";
    }

    /**
     * Parse ack_control_id from ACK message
     */
    public String parseAckControlId(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String pattern = "ACK.ack_control_id V=\"";
        int idx = message.indexOf(pattern);
        if (idx >= 0) {
            int start = idx + pattern.length();
            int end = message.indexOf('"', start);
            if (end > start) {
                return message.substring(start, end);
            }
        }
        return parseControlId(message);
    }

    /**
     * Parse a simple attribute value from XML
     */
    public String parseAttribute(String message, String elementName, String attrName) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String pattern = elementName + " " + attrName + " V=\"";
        int idx = message.indexOf(pattern);
        if (idx >= 0) {
            int start = idx + pattern.length();
            int end = message.indexOf('"', start);
            if (end > start) {
                return message.substring(start, end);
            }
        }
        return "";
    }

    /**
     * Parse a simple element value (text between tags)
     */
    public String parseElementValue(String message, String elementName) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String openTag = "<" + elementName + ">";
        String closeTag = "</" + elementName + ">";
        int start = message.indexOf(openTag);
        if (start >= 0) {
            int end = message.indexOf(closeTag, start);
            if (end > start + openTag.length()) {
                return message.substring(start + openTag.length(), end).trim();
            }
        }
        return "";
    }

    /**
     * Parse device info from HEL.R01 message
     */
    public DeviceInfo parseDeviceInfo(String message) {
        DeviceInfo info = new DeviceInfo();
        info.setSerialId(parseAttribute(message, "HEL.device_serial", "V"));
        info.setDeviceName(parseAttribute(message, "HEL.device_name", "V"));
        info.setSwVersion(parseAttribute(message, "HEL.device_sw_ver", "V"));
        info.setHwVersion(parseAttribute(message, "HEL.device_hw_ver", "V"));
        info.setDeviceType(parseAttribute(message, "HEL.device_type", "V"));
        info.setDeviceClass(parseAttribute(message, "HEL.device_class", "V"));
        info.setFromInstId(parseAttribute(message, "HEL.from_inst_id", "V"));
        info.setVendorId(parseAttribute(message, "HEL.vendor_id", "V"));
        return info;
    }

    /**
     * Parse status info from DST.R01 message
     */
    public StatusInfo parseStatusInfo(String message) {
        StatusInfo info = new StatusInfo();
        info.setNewObservationsQty(parseAttribute(message, "DST.new_observations_qty", "V"));
        info.setNewEventsQty(parseAttribute(message, "DST.new_events_qty", "V"));
        info.setLocationNum(parseAttribute(message, "DST.loc_num", "V"));
        info.setFacility(parseAttribute(message, "DST.fac_name", "V"));
        info.setInstNum(parseAttribute(message, "DST.inst_num", "V"));
        info.setSupportsSetTime(parseAttribute(message, "DST.supports_set_time", "V"));
        info.setSupportsContinuous(parseAttribute(message, "DST.supports_continuous", "V"));
        return info;
    }

    /**
     * Parse observation count from OBS.R01/R02 messages
     */
    public int parseObservationCount(String message) {
        // Count OBS.observation elements
        int count = 0;
        int idx = 0;
        while ((idx = message.indexOf("<OBS.observation", idx)) != -1) {
            count++;
            idx++;
        }
        return count;
    }

    /**
     * Parse event count from EVS.R01 messages
     */
    public int parseEventCount(String message) {
        int count = 0;
        int idx = 0;
        while ((idx = message.indexOf("<EVS.event", idx)) != -1) {
            count++;
            idx++;
        }
        return count;
    }

    /**
     * Parse EOT topic from EOT.R01 message
     */
    public String parseEotTopic(String message) {
        return parseAttribute(message, "EOT.topic_cd", "V");
    }

    /**
     * Inner class for device info
     */
    @lombok.Data
    public static class DeviceInfo {
        private String serialId;
        private String deviceName;
        private String swVersion;
        private String hwVersion;
        private String deviceType;
        private String deviceClass;
        private String fromInstId;
        private String vendorId;
    }

    /**
     * Inner class for status info
     */
    @lombok.Data
    public static class StatusInfo {
        private String newObservationsQty;
        private String newEventsQty;
        private String locationNum;
        private String facility;
        private String instNum;
        private String supportsSetTime;
        private String supportsContinuous;
    }
}