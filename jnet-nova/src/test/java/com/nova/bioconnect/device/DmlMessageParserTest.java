package com.nova.bioconnect.device;

import com.nova.bioconnect.device.protocol.DmlMessageParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DML Message Parser Tests
 */
class DmlMessageParserTest {

    private DmlMessageParser parser;

    @BeforeEach
    void setUp() {
        parser = new DmlMessageParser();
    }

    @Test
    @DisplayName("Parse HEL.R01 message type")
    void testParseMessageTypeHel() {
        String message = "<HEL.R01><HDR><HDR.control_id V=\"12345\"/></HDR><HEL><HEL.device_serial V=\"SN123\"/></HEL></HEL.R01>";
        assertEquals("HEL.R01", parser.parseMessageType(message));
    }

    @Test
    @DisplayName("Parse DST.R01 message type")
    void testParseMessageTypeDst() {
        String message = "<DST.R01><HDR><HDR.control_id V=\"12345\"/></HDR><DST/></DST.R01>";
        assertEquals("DST.R01", parser.parseMessageType(message));
    }

    @Test
    @DisplayName("Parse control_id from message")
    void testParseControlId() {
        String message = "<HEL.R01><HDR><HDR.control_id V=\"12345\"/></HDR><HEL/></HEL.R01>";
        assertEquals("12345", parser.parseControlId(message));
    }

    @Test
    @DisplayName("Parse ack_control_id from ACK message")
    void testParseAckControlId() {
        String message = "<ACK.R01><HDR><HDR.control_id V=\"67890\"/></HDR><ACK><ACK.ack_control_id V=\"12345\"/><ACK.type_cd V=\"AA\"/></ACK></ACK.R01>";
        assertEquals("12345", parser.parseAckControlId(message));
    }

    @Test
    @DisplayName("Parse device info from HEL.R01")
    void testParseDeviceInfo() {
        String message = "<HEL.R01><HDR><HDR.control_id V=\"1\"/></HDR>" +
                "<HEL>" +
                "<HEL.device_serial V=\"SN-TEST-001\"/>" +
                "<HEL.device_name V=\"Test Device\"/>" +
                "<HEL.device_sw_ver V=\"2.0.1\"/>" +
                "<HEL.device_hw_ver V=\"1.0\"/>" +
                "<HEL.device_type V=\"StatStrip\"/>" +
                "<HEL.device_class V=\"POCT\"/>" +
                "<HEL.from_inst_id V=\"StatStrip\"/>" +
                "<HEL.vendor_id V=\"NOVA\"/>" +
                "</HEL></HEL.R01>";

        DmlMessageParser.DeviceInfo info = parser.parseDeviceInfo(message);
        assertEquals("SN-TEST-001", info.getSerialId());
        assertEquals("Test Device", info.getDeviceName());
        assertEquals("2.0.1", info.getSwVersion());
        assertEquals("1.0", info.getHwVersion());
        assertEquals("StatStrip", info.getDeviceType());
        assertEquals("POCT", info.getDeviceClass());
        assertEquals("StatStrip", info.getFromInstId());
        assertEquals("NOVA", info.getVendorId());
    }

    @Test
    @DisplayName("Parse status info from DST.R01")
    void testParseStatusInfo() {
        String message = "<DST.R01><HDR><HDR.control_id V=\"2\"/></HDR>" +
                "<DST>" +
                "<DST.new_observations_qty V=\"5\"/>" +
                "<DST.new_events_qty V=\"2\"/>" +
                "<DST.loc_num V=\"LOC-001\"/>" +
                "<DST.fac_name V=\"Main Lab\"/>" +
                "<DST.inst_num V=\"INST-001\"/>" +
                "<DST.supports_set_time V=\"T\"/>" +
                "<DST.supports_continuous V=\"T\"/>" +
                "</DST></DST.R01>";

        DmlMessageParser.StatusInfo info = parser.parseStatusInfo(message);
        assertEquals("5", info.getNewObservationsQty());
        assertEquals("2", info.getNewEventsQty());
        assertEquals("LOC-001", info.getLocationNum());
        assertEquals("Main Lab", info.getFacility());
        assertEquals("INST-001", info.getInstNum());
        assertEquals("T", info.getSupportsSetTime());
        assertEquals("T", info.getSupportsContinuous());
    }

    @Test
    @DisplayName("Parse observation count from OBS.R01")
    void testParseObservationCount() {
        String message = "<OBS.R01><HDR><HDR.control_id V=\"3\"/></HDR>" +
                "<OBS><OBS.observation V=\"1\"/><OBS.observation V=\"2\"/><OBS.observation V=\"3\"/></OBS>" +
                "</OBS.R01>";
        assertEquals(3, parser.parseObservationCount(message));
    }

    @Test
    @DisplayName("Parse event count from EVS.R01")
    void testParseEventCount() {
        String message = "<EVS.R01><HDR><HDR.control_id V=\"4\"/></HDR>" +
                "<EVS><EVS.event V=\"1\"/><EVS.event V=\"2\"/></EVS>" +
                "</EVS.R01>";
        assertEquals(2, parser.parseEventCount(message));
    }

    @Test
    @DisplayName("Parse EOT topic from EOT.R01")
    void testParseEotTopic() {
        String message = "<EOT.R01><HDR><HDR.control_id V=\"5\"/></HDR>" +
                "<EOT><EOT.topic_cd V=\"NOVA.STATSTRIP.SETUP\"/></EOT>" +
                "</EOT.R01>";
        assertEquals("NOVA.STATSTRIP.SETUP", parser.parseEotTopic(message));
    }

    @Test
    @DisplayName("Parse message type with nested elements")
    void testParseMessageTypeWithNested() {
        String message = "<ACK.R01><HDR><HDR.control_id V=\"test\"/></HDR><ACK><ACK.ack_control_id V=\"abc\"/><ACK.type_cd V=\"AA\"/></ACK></ACK.R01>";
        assertEquals("ACK.R01", parser.parseMessageType(message));
    }

    @Test
    @DisplayName("Handle empty message")
    void testHandleEmptyMessage() {
        assertEquals("", parser.parseMessageType(""));
        assertEquals("", parser.parseControlId(""));
    }

    @Test
    @DisplayName("Handle null message")
    void testHandleNullMessage() {
        assertEquals("", parser.parseMessageType(null));
        assertEquals("", parser.parseControlId(null));
    }
}