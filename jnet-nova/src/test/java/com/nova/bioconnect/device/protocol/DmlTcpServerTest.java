package com.nova.bioconnect.device.protocol;

import com.nova.bioconnect.device.config.NovaDeviceProperties;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * DML TCP 服务器集成测试
 *
 * <p>验证基于 Netty 的 DML TCP 服务器的消息发送/接收完整业务流程，包括：</p>
 * <ul>
 *   <li>HEL.R01（设备注册）消息的发送与 ACK 响应接收</li>
 *   <li>OBS.R01（患者观察数据）消息的发送与 REQ 响应接收</li>
 *   <li>分片发送（模拟 TCP 粘包/半包）场景下的消息帧解码</li>
 *   <li>连续多消息收发</li>
 *   <li>大消息体处理</li>
 * </ul>
 *
 * <p>测试使用 Mock 的 {@link DmlMessageHandler}，隔离业务逻辑层，
 * 专注于验证 Netty Pipeline 的编解码和消息收发流程。</p>
 */
@DisplayName("DML TCP Server (Netty) - 消息收发集成测试")
class DmlTcpServerTest {

    private DmlTcpServer server;
    private DmlMessageHandler mockHandler;
    private int serverPort;

    /** HEL.R01 示例消息（来自 DML 接口规范 6.2.4） */
    private static final String HEL_R01_MESSAGE = """
            <HEL.R01>
            <HDR>
            <HDR.message_type V="HEL.R01" SN="POCT1" SV="1" />
            <HDR.control_id V="7135" />
            <HDR.version_id V="POCT1" />
            <HDR.creation_dttm V="2010-02-22T14:06:34.00-05:00" />
            </HDR>
            <DEV>
            <DEV.device_id V="">MGH^ICU</DEV.device_id>
            <DEV.vendor_id V="NOVABIO" />
            <DEV.model_id V="StatStrip" />
            <DEV.serial_id V="0600100736" />
            <DEV.manufacturer_name V="Nova Biomedical" />
            <DEV.hw_version V="K1" />
            <DEV.sw_version V="3.10.8.0">3.10.8.0_it-IT</DEV.sw_version>
            <DEV.device_name V="DLO-OV" />
            <DCP>
            <DCP.application_timeout V="120" />
            </DCP>
            <DSC>
            <DSC.connection_profile_cd V="SA" />
            <DSC.topics_supported_cd V="D_EV" SN="POCT1" SV="1" />
            <DSC.topics_supported_cd V="DTV" SN="POCT1" SV="1" />
            <DSC.max_message_sz V="32768" />
            </DSC>
            </DEV>
            </HEL.R01>""";

    /** OBS.R01 示例消息（来自 DML 接口规范 6.4.2，简化版） */
    private static final String OBS_R01_MESSAGE = """
            <OBS.R01>
             <HDR>
              <HDR.message_type V="OBS.R01"/>
              <HDR.control_id V="10003"/>
              <HDR.version_id V="POCT1"/>
              <HDR.creation_dttm V="2001-11-01T16:30:06-05:00"/>
             </HDR>
             <SVC>
              <SVC.role_cd V="OBS"/>
              <SVC.observation_dttm V="2001-11-01T16:29:54-05:00"/>
              <SVC.status_cd V="NRM"/>
              <PT>
               <PT.patient_id V="PT222-55-7777"/>
               <PT.location V="Cambridge Hosp^ICU-4"/>
               <OBS>
                <OBS.observation_id V="2341-6" DN="Glu" SN="LN"/>
                <OBS.value V="145" U="mg/dL"/>
                <OBS.method_cd V="M"/>
                <OBS.status_cd V="A"/>
                <OBS.interpretation_cd V="HH"/>
                <OBS.normal_lo-hi_limit V="[80;120]" U="mg/dL"/>
                <OBS.critical_lo-hi_limit V="[60;140]" U="mg/dL"/>
               </OBS>
              </PT>
              <OPR>
               <OPR.operator_id V="OP777-88-9999"/>
              </OPR>
             </SVC>
            </OBS.R01>""";

    /** DST.R01 示例消息（设备状态） */
    private static final String DST_R01_MESSAGE = """
            <DST.R01>
            <HDR>
            <HDR.message_type V="DST.R01" SN="POCT1" SV="1" />
            <HDR.control_id V="8001" />
            <HDR.version_id V="POCT1" />
            <HDR.creation_dttm V="2024-01-15T10:30:00.000+08:00" />
            </HDR>
            <DEV>
            <DEV.serial_id V="0600100736" />
            <DEV.device_id_text V="MGH^ICU" />
            </DEV>
            <DST>
            <DST.status_cd V="READY" />
            <DST.observation_cnt V="0" />
            </DST>
            </DST.R01>""";

    @BeforeEach
    void setUp() throws Exception {
        // 创建 Mock 的 DmlMessageHandler
        mockHandler = Mockito.mock(DmlMessageHandler.class);

        // 配置 Mock 行为：根据消息内容返回不同响应（新签名返回 List<String>）
        when(mockHandler.handleMessage(anyString(), any(DmlSession.class))).thenAnswer(invocation -> {
            String msg = invocation.getArgument(0);
            if (msg.contains("HEL.R01")) {
                return List.of("""
                        <ACK.R01>
                          <HDR>
                            <HDR.message_type V="ACK.R01" SN="POCT1" SV="1" />
                            <HDR.control_id V="9001" />
                            <HDR.version_id V="POCT1" />
                            <HDR.creation_dttm V="2024-01-15T10:30:01.000+08:00" />
                          </HDR>
                          <ACK>
                            <ACK.type_cd V="AA"/>
                            <ACK.ack_control_id V="7135"/>
                          </ACK>
                        </ACK.R01>""");
            } else if (msg.contains("OBS.R01")) {
                return List.of("""
                        <REQ.R01>
                          <HDR>
                            <HDR.message_type V="REQ.R01" SN="POCT1" SV="1" />
                            <HDR.control_id V="9002" />
                            <HDR.version_id V="POCT1" />
                            <HDR.creation_dttm V="2024-01-15T10:30:02.000+08:00" />
                          </HDR>
                          <REQ.topic_cd V="EVS.R01" />
                        </REQ.R01>""");
            } else if (msg.contains("DST.R01")) {
                return List.of("""
                        <REQ.R01>
                          <HDR>
                            <HDR.message_type V="REQ.R01" SN="POCT1" SV="1" />
                            <HDR.control_id V="9003" />
                            <HDR.version_id V="POCT1" />
                            <HDR.creation_dttm V="2024-01-15T10:30:03.000+08:00" />
                          </HDR>
                          <REQ.topic_cd V="OBS.R01" />
                        </REQ.R01>""");
            }
            return List.of("""
                    <ACK.R01>
                      <HDR>
                        <HDR.message_type V="ACK.R01" SN="POCT1" SV="1" />
                      </HDR>
                    </ACK.R01>""");
        });

        // 配置测试用 NovaDeviceProperties
        NovaDeviceProperties properties = new NovaDeviceProperties();
        properties.setTcpPort(0);       // 端口 0 = 操作系统自动分配空闲端口
        properties.setTcpHost("");
        properties.setMaxMessageSize(32768);
        properties.setApplicationTimeout(30);
        properties.setKpaInterval(60);  // KPA 间隔 60s（测试快速完成，不会触发）
        properties.setKeepAlive(true);
        properties.setBacklog(10);

        // 创建并启动 DML TCP 服务器
        server = new DmlTcpServer(properties, mockHandler);
        server.start();

        serverPort = server.getPort();
        assertTrue(serverPort > 0, "Server should be bound to a valid port");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    // ==================== 测试用例 ====================

    @Test
    @DisplayName("HEL.R01 设备注册 → 接收 ACK.R01 响应")
    void testHelloMessage_shouldReturnAck() throws Exception {
        try (Socket client = createClient()) {
            String response = sendAndReceive(client, HEL_R01_MESSAGE);

            assertNotNull(response, "Response should not be null");
            assertTrue(response.contains("<ACK.R01>"), "Response should be ACK.R01");
            assertTrue(response.contains("HDR.message_type V=\"ACK.R01\""),
                    "Response should contain ACK.R01 message type");
        }
    }

    @Test
    @DisplayName("OBS.R01 患者观察数据 → 接收 REQ.R01 请求下一条数据")
    void testPatientObservation_shouldReturnRequestNext() throws Exception {
        try (Socket client = createClient()) {
            String response = sendAndReceive(client, OBS_R01_MESSAGE);

            assertNotNull(response, "Response should not be null");
            assertTrue(response.contains("<REQ.R01>"), "Response should be REQ.R01");
            assertTrue(response.contains("REQ.topic_cd V=\"EVS.R01\""),
                    "Response should request EVS.R01 (events)");
        }
    }

    @Test
    @DisplayName("DST.R01 设备状态 → 接收 REQ.R01 请求观察数据")
    void testDeviceStatus_shouldRequestObservations() throws Exception {
        try (Socket client = createClient()) {
            String response = sendAndReceive(client, DST_R01_MESSAGE);

            assertNotNull(response, "Response should not be null");
            assertTrue(response.contains("<REQ.R01>"), "Response should be REQ.R01");
            assertTrue(response.contains("REQ.topic_cd V=\"OBS.R01\""),
                    "Response should request OBS.R01 (observations)");
        }
    }

    @Test
    @DisplayName("分片发送（模拟 TCP 半包）→ 正确组装并响应")
    void testFragmentedMessage_shouldAssembleCorrectly() throws Exception {
        try (Socket client = createClient()) {
            // 将 HEL.R01 消息分成 3 个片段发送
            String message = HEL_R01_MESSAGE;
            int len = message.length();
            int part1End = len / 3;
            int part2End = (len / 3) * 2;

            OutputStream out = client.getOutputStream();
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

            // 发送第一片
            out.write(bytes, 0, part1End);
            out.flush();
            Thread.sleep(50);

            // 发送第二片
            out.write(bytes, part1End, part2End - part1End);
            out.flush();
            Thread.sleep(50);

            // 发送第三片
            out.write(bytes, part2End, len - part2End);
            out.flush();

            // 读取响应
            String response = readResponse(client);

            assertNotNull(response, "Response should not be null");
            assertTrue(response.contains("<ACK.R01>"),
                    "Server should correctly assemble fragmented message and return ACK.R01");
        }
    }

    @Test
    @DisplayName("连续多消息收发 → 每条消息独立响应")
    void testMultipleMessages_shouldRespondToEach() throws Exception {
        try (Socket client = createClient()) {
            // 第一条：HEL.R01
            String resp1 = sendAndReceive(client, HEL_R01_MESSAGE);
            assertTrue(resp1.contains("<ACK.R01>"), "First response should be ACK.R01");

            // 第二条：DST.R01（复用同一连接）
            String resp2 = sendAndReceive(client, DST_R01_MESSAGE);
            assertTrue(resp2.contains("<REQ.R01>"), "Second response should be REQ.R01");
            assertTrue(resp2.contains("OBS.R01"), "Second response should request OBS.R01");

            // 第三条：OBS.R01（复用同一连接）
            String resp3 = sendAndReceive(client, OBS_R01_MESSAGE);
            assertTrue(resp3.contains("<REQ.R01>"), "Third response should be REQ.R01");
            assertTrue(resp3.contains("EVS.R01"), "Third response should request EVS.R01");
        }
    }

    @Test
    @DisplayName("验证 Mock Handler 被正确调用")
    void testHandlerInvoked() throws Exception {
        try (Socket client = createClient()) {
            sendAndReceive(client, HEL_R01_MESSAGE);
        }

        // 验证 DmlMessageHandler.handleMessage 被调用了一次
        Mockito.verify(mockHandler, Mockito.times(1))
                .handleMessage(Mockito.contains("HEL.R01"), any(DmlSession.class));
    }

    @Test
    @DisplayName("服务器运行状态检查")
    void testServerRunningStatus() {
        assertTrue(server.isRunning(), "Server should be running after start");
        assertTrue(serverPort > 0, "Server port should be positive");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建连接到测试服务器的 Socket 客户端
     */
    private Socket createClient() throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", serverPort), 5000);
        socket.setSoTimeout(10000);
        assertTrue(socket.isConnected(), "Client should be connected to server");
        return socket;
    }

    /**
     * 发送 DML 消息并读取完整响应
     *
     * <p>响应是完整的 XML 文档（如 {@code <ACK.R01>...</ACK.R01>}），
     * 后跟一个换行符 {@code \n}。本方法通过检测根元素闭合标签来判断响应是否完整。</p>
     *
     * @param socket 已连接的 Socket
     * @param message 要发送的 DML XML 消息
     * @return 完整的响应 XML 字符串
     */
    private String sendAndReceive(Socket socket, String message) throws Exception {
        OutputStream out = socket.getOutputStream();
        out.write(message.getBytes(StandardCharsets.UTF_8));
        out.flush();

        return readResponse(socket);
    }

    /**
     * 从 Socket 读取完整的 DML XML 响应
     *
     * <p>持续读取字节直到检测到根元素的闭合标签，确保收到完整 XML 文档。</p>
     */
    private String readResponse(Socket socket) throws Exception {
        InputStream in = socket.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];

        long deadline = System.currentTimeMillis() + 10000; // 10 秒超时

        while (System.currentTimeMillis() < deadline) {
            int read = in.read(data);
            if (read == -1) {
                break;
            }
            buffer.write(data, 0, read);

            String content = buffer.toString(StandardCharsets.UTF_8);
            String rootElement = extractRootElement(content);

            if (rootElement != null) {
                String closingTag = "</" + rootElement + ">";
                int closingIdx = content.indexOf(closingTag);
                if (closingIdx != -1) {
                    // 完整响应已收到
                    int endIdx = closingIdx + closingTag.length();
                    return content.substring(0, endIdx);
                }
            }
        }

        String result = buffer.toString(StandardCharsets.UTF_8);
        if (result.isEmpty()) {
            throw new RuntimeException("No response received within timeout");
        }
        return result;
    }

    /**
     * 从 XML 内容中提取根元素名称
     */
    private String extractRootElement(String content) {
        int idx = 0;
        int len = content.length();

        // 跳过前导空白
        while (idx < len && Character.isWhitespace(content.charAt(idx))) {
            idx++;
        }

        if (idx >= len || content.charAt(idx) != '<') {
            return null;
        }

        int tagStart = idx + 1;
        if (tagStart < len && content.charAt(tagStart) == '/') {
            return null;
        }

        int tagEnd = tagStart;
        while (tagEnd < len) {
            char c = content.charAt(tagEnd);
            if (c == ' ' || c == '>' || c == '/' || c == '\t' || c == '\r' || c == '\n') {
                break;
            }
            tagEnd++;
        }

        if (tagEnd == tagStart || tagEnd >= len) {
            return null;
        }

        // 检查标签是否完整（是否有 '>'）
        if (content.indexOf('>', tagEnd) == -1) {
            return null;
        }

        return content.substring(tagStart, tagEnd);
    }
}
