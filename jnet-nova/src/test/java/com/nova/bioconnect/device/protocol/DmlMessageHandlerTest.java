package com.nova.bioconnect.device.protocol;

import com.nova.bioconnect.device.protocol.xml.*;
import com.nova.bioconnect.device.service.NovaSyncService;
import com.nova.bioconnect.device.service.SampleDataService;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DML 消息处理器状态机单元测试
 *
 * <p>验证基于 C# {@code DMLProtocol} 参考实现重写后的状态机业务逻辑，覆盖：</p>
 * <ul>
 *   <li>HEL.R01 → ACK 回显 control_id + 状态推进</li>
 *   <li>DST.R01 → ACK + 根据观察/事件数量决定后续 REQ</li>
 *   <li>EOT.R01 仅推进状态机不产生 ACK</li>
 *   <li>END.R01 / ESC.R01 / 未知消息 → 关闭会话</li>
 *   <li>control_id 自增序列</li>
 * </ul>
 *
 * <p>使用 {@link EmbeddedChannel} 提供真实 Channel 上下文，Service 层使用 Mock 隔离。</p>
 */
@DisplayName("DML 消息处理器 - 状态机业务逻辑测试")
@ExtendWith(MockitoExtension.class)
class DmlMessageHandlerTest {

    @Mock private DmlXmlConverter xmlConverter;
    @Mock private SampleDataService sampleDataService;
    @Mock private NovaSyncService syncService;

    private DmlMessageHandler handler;
    private EmbeddedChannel channel;
    private DmlSession session;

    @BeforeEach
    void setUp() {
        handler = new DmlMessageHandler(xmlConverter, sampleDataService, syncService);
        channel = new EmbeddedChannel();
        session = new DmlSession(channel);
        channel.attr(DmlSession.SESSION_KEY).set(session);
    }

    @Test
    @DisplayName("HEL.R01 → ACK 回显 control_id，状态推进到 ACK_HELLO")
    void testHelloMessage_shouldAckWithControlIdEcho() {
        DmlHelloMessage helloMsg = DmlHelloMessage.builder()
                .header(DmlHeader.builder().controlId("7135").build())
                .device(DmlDevice.builder()
                        .serialId("0600100736")
                        .modelId("StatStrip")
                        .deviceIdText("MGH^ICU")
                        .build())
                .build();
        when(xmlConverter.parseXml(anyString(), eq(DmlHelloMessage.class))).thenReturn(helloMsg);

        String xml = "<HEL.R01><HDR><HDR.control_id V=\"7135\"/></HDR></HEL.R01>";
        List<String> outbound = handler.handleMessage(xml, session);

        assertEquals(1, outbound.size(), "HEL should produce exactly one ACK");
        String ack = outbound.get(0);
        assertTrue(ack.contains("<ACK.R01>"), "Should be ACK.R01");
        assertTrue(ack.contains("<ACK.type_cd V=\"AA\""), "Should be success ACK");
        assertTrue(ack.contains("<ACK.ack_control_id V=\"7135\""),
                "ACK must echo received control_id");
        assertEquals(DmlState.ACK_HELLO, session.getState(),
                "State should advance to ACK_HELLO(3) after HEL");
        assertEquals("0600100736", session.getSerialId());
        assertEquals("MGH", session.getFacility());
        assertEquals("ICU", session.getLocation());
        verify(syncService).handleHello(helloMsg);
    }

    @Test
    @DisplayName("DST.R01 有新观察数据 → ACK + REQ(ROBS)")
    void testDeviceStatusWithObservations_shouldRequestObservations() {
        session.transitionTo(DmlState.ACK_HELLO);
        DmlDeviceStatusMessage statusMsg = DmlDeviceStatusMessage.builder()
                .header(DmlHeader.builder().controlId("8001").build())
                .status(DmlDeviceStatus.builder()
                        .newObservationsQty("5")
                        .newEventsQty("2")
                        .conditionCd("READY")
                        .build())
                .build();
        when(xmlConverter.parseXml(anyString(), eq(DmlDeviceStatusMessage.class))).thenReturn(statusMsg);

        String xml = "<DST.R01><HDR><HDR.control_id V=\"8001\"/></HDR></DST.R01>";
        List<String> outbound = handler.handleMessage(xml, session);

        assertEquals(2, outbound.size(), "DST with obs should produce ACK + REQ");
        assertTrue(outbound.get(0).contains("<ACK.R01>"), "First should be ACK");
        assertTrue(outbound.get(0).contains("V=\"8001\""), "ACK echoes control_id");
        assertTrue(outbound.get(1).contains("<REQ.R01>"), "Second should be REQ");
        assertTrue(outbound.get(1).contains("ROBS"), "REQ should request ROBS");
        assertEquals(5, session.getNewObservationsQty());
        assertEquals(2, session.getNewEventsQty());
        assertEquals(DmlState.REQ_OBS, session.getState());
    }

    @Test
    @DisplayName("DST.R01 无新数据 → 仅 ACK，跳过到下行同步")
    void testDeviceStatusNoData_shouldSkipToSetup() {
        session.transitionTo(DmlState.ACK_HELLO);
        DmlDeviceStatusMessage statusMsg = DmlDeviceStatusMessage.builder()
                .header(DmlHeader.builder().controlId("8002").build())
                .status(DmlDeviceStatus.builder()
                        .newObservationsQty("0")
                        .newEventsQty("0")
                        .build())
                .build();
        when(xmlConverter.parseXml(anyString(), eq(DmlDeviceStatusMessage.class))).thenReturn(statusMsg);

        List<String> outbound = handler.handleMessage(
                "<DST.R01><HDR><HDR.control_id V=\"8002\"/></HDR></DST.R01>", session);

        // ACK + 一路 fallthrough 到 DECISION → TRM（因不支持连续模式）
        assertFalse(outbound.isEmpty(), "Should produce responses");
        assertTrue(outbound.get(0).contains("<ACK.R01>"));
        // 最终应到达终止或连续决策
        assertTrue(session.getState().getCode() >= 38,
                "State should reach decision point, got " + session.getState());
    }

    @Test
    @DisplayName("OBS.R01 → ACK，状态保持 REQ_OBS 等待 EOT")
    void testObservation_shouldAckAndStayInReqObs() {
        session.transitionTo(DmlState.REQ_OBS);
        DmlPatientObservationMessage obsMsg = DmlPatientObservationMessage.builder()
                .header(DmlHeader.builder().controlId("10003").build())
                .build();
        when(xmlConverter.parseXml(anyString(), eq(DmlPatientObservationMessage.class))).thenReturn(obsMsg);

        List<String> outbound = handler.handleMessage(
                "<OBS.R01><HDR><HDR.control_id V=\"10003\"/></HDR></OBS.R01>", session);

        assertEquals(1, outbound.size());
        assertTrue(outbound.get(0).contains("<ACK.R01>"));
        assertTrue(outbound.get(0).contains("V=\"10003\""));
        assertEquals(DmlState.REQ_OBS, session.getState(), "State stays REQ_OBS until EOT");
        verify(sampleDataService).processPatientObservations(obsMsg);
    }

    @Test
    @DisplayName("EOT.R01 在 REQ_OBS 态 → 推进到 OBS_EOT，无 ACK")
    void testEndOfTopic_advancesStateWithoutAck() {
        session.transitionTo(DmlState.REQ_OBS);
        session.setNewEventsQty(0); // 无事件，应直接跳到下行

        List<String> outbound = handler.handleMessage(
                "<EOT.R01><HDR><HDR.control_id V=\"9000\"/></HDR></EOT.R01>", session);

        // EOT 不产生 ACK；状态机推进后会发送下行消息或终止
        assertTrue(outbound.isEmpty() || !outbound.get(0).contains("<ACK.R01>"),
                "EOT should not produce ACK");
        assertTrue(session.getState().getCode() >= 5,
                "State should advance past OBS_EOT, got " + session.getState());
    }

    @Test
    @DisplayName("END.R01 → ACK + 关闭会话")
    void testTerminateMessage_shouldAckAndShutDown() {
        List<String> outbound = handler.handleMessage(
                "<END.R01><HDR><HDR.control_id V=\"5000\"/></HDR></END.R01>", session);

        assertEquals(1, outbound.size());
        assertTrue(outbound.get(0).contains("<ACK.R01>"));
        assertTrue(outbound.get(0).contains("V=\"5000\""));
        assertTrue(session.isShutDown(), "Session should be marked shut down");
        assertEquals(DmlState.CLOSED, session.getState());
    }

    @Test
    @DisplayName("ESC.R01 → 关闭会话，无 ACK")
    void testEscapeMessage_shouldShutDownWithoutAck() {
        List<String> outbound = handler.handleMessage(
                "<ESC.R01><HDR><HDR.control_id V=\"5001\"/></HDR></ESC.R01>", session);

        assertTrue(session.isShutDown());
        // ESC 不回 ACK；可能产生空列表
        assertTrue(outbound.isEmpty() || !outbound.get(0).contains("<ACK.R01>"),
                "ESC should not produce ACK");
    }

    @Test
    @DisplayName("未知消息类型 → ESC + 关闭会话")
    void testUnknownMessage_shouldEscapeAndShutDown() {
        List<String> outbound = handler.handleMessage(
                "<FOO.R99><HDR><HDR.control_id V=\"5002\"/></HDR></FOO.R99>", session);

        assertFalse(outbound.isEmpty(), "Should produce ESC response");
        assertTrue(outbound.get(0).contains("<ESC.R01>"), "Should be ESC for unknown type");
        assertTrue(session.isShutDown());
    }

    @Test
    @DisplayName("KPA.R01 → ACK 回显 control_id")
    void testKeepAlive_shouldAckWithControlId() {
        List<String> outbound = handler.handleMessage(
                "<KPA.R01><HDR><HDR.control_id V=\"6000\"/></HDR></KPA.R01>", session);

        assertEquals(1, outbound.size());
        assertTrue(outbound.get(0).contains("<ACK.R01>"));
        assertTrue(outbound.get(0).contains("V=\"6000\""));
    }

    @Test
    @DisplayName("出站 control_id 自增序列")
    void testOutboundControlIdSequence() {
        DmlHelloMessage helloMsg = DmlHelloMessage.builder()
                .header(DmlHeader.builder().controlId("1").build())
                .device(DmlDevice.builder().serialId("SN001").build())
                .build();
        when(xmlConverter.parseXml(anyString(), eq(DmlHelloMessage.class))).thenReturn(helloMsg);

        // 第一次消息
        handler.handleMessage("<HEL.R01><HDR><HDR.control_id V=\"1\"/></HDR></HEL.R01>", session);
        String firstId = session.nextOutboundControlId();

        // 第二次消息
        handler.handleMessage("<KPA.R01><HDR><HDR.control_id V=\"2\"/></HDR></KPA.R01>", session);
        String secondId = session.nextOutboundControlId();

        assertNotEquals(firstId, secondId, "Control IDs must be unique and increasing");
        int first = Integer.parseInt(firstId);
        int second = Integer.parseInt(secondId);
        assertTrue(second > first, "Second control_id should be greater than first");
    }

    @Test
    @DisplayName("ACK.R01 来自设备 → 无响应，会话保持活跃")
    void testAckFromDevice_noResponse() {
        session.transitionTo(DmlState.SETUP_SENT);
        List<String> outbound = handler.handleMessage(
                "<ACK.R01><HDR><HDR.control_id V=\"7000\"/></HDR></ACK.R01>", session);

        // ACK 不产生即时响应；但状态机会推进到 SETUP_EOT 发送 EOT
        assertFalse(session.isShutDown(), "Session should remain active after ACK");
    }

    @Test
    @DisplayName("XML 含非法控制字符 → 自动清理后处理")
    void testInvalidXmlChars_shouldBeSanitized() {
        // 包含 \u0003 (ETX) 非法字符
        String xml = "<KPA.R01><HDR><HDR.control_id V=\"8000\"/></HDR>\u0003</KPA.R01>";
        List<String> outbound = handler.handleMessage(xml, session);

        assertEquals(1, outbound.size());
        assertTrue(outbound.get(0).contains("<ACK.R01>"), "Should sanitize and process message");
    }
}
