package com.nova.bioconnect.device.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DML XML 帧解码器单元测试
 *
 * <p>使用 Netty {@link EmbeddedChannel} 测试 {@link DmlXmlFrameDecoder} 的帧解码逻辑，
 * 覆盖以下场景：</p>
 * <ul>
 *   <li>单次读取完整消息</li>
 *   <li>消息分片到达（TCP 半包）</li>
 *   <li>多条消息一次性到达（TCP 粘包）</li>
 *   <li>消息前有 XML 声明</li>
 *   <li>超过最大消息大小时关闭连接</li>
 * </ul>
 */
@DisplayName("DML XML 帧解码器测试")
class DmlXmlFrameDecoderTest {

    private static final String HELLO_MSG = """
            <HEL.R01>
              <HDR>
                <HDR.message_type V="HEL.R01" />
              </HDR>
            </HEL.R01>""";

    private static final String OBS_MSG = """
            <OBS.R01>
              <HDR>
                <HDR.message_type V="OBS.R01" />
              </HDR>
            </OBS.R01>""";

    @Test
    @DisplayName("单次读取完整消息 → 正确解码")
    void testCompleteMessageInOneRead() {
        EmbeddedChannel channel = new EmbeddedChannel(new DmlXmlFrameDecoder(32768));

        ByteBuf buf = Unpooled.wrappedBuffer(HELLO_MSG.getBytes(StandardCharsets.UTF_8));
        channel.writeInbound(buf);

        String decoded = channel.readInbound();
        assertNotNull(decoded, "Should decode a complete message");
        assertTrue(decoded.contains("<HEL.R01>"), "Decoded message should contain root tag");
        assertTrue(decoded.endsWith("</HEL.R01>"), "Decoded message should end with closing tag");
    }

    @Test
    @DisplayName("消息分片到达 → 正确组装")
    void testFragmentedMessage() {
        EmbeddedChannel channel = new EmbeddedChannel(new DmlXmlFrameDecoder(32768));

        byte[] bytes = HELLO_MSG.getBytes(StandardCharsets.UTF_8);
        int mid = bytes.length / 2;

        // 发送前半部分 - 不应产生输出
        ByteBuf part1 = Unpooled.wrappedBuffer(bytes, 0, mid);
        channel.writeInbound(part1);
        assertNull(channel.readInbound(), "First half should not produce a message");

        // 发送后半部分 - 应产生完整消息
        ByteBuf part2 = Unpooled.wrappedBuffer(bytes, mid, bytes.length - mid);
        channel.writeInbound(part2);

        String decoded = channel.readInbound();
        assertNotNull(decoded, "Complete message should be decoded after second half");
        assertTrue(decoded.contains("<HEL.R01>"), "Decoded message should be the HEL.R01 message");
    }

    @Test
    @DisplayName("多条消息一次性到达 → 分别解码")
    void testMultipleMessagesInOneBuffer() {
        EmbeddedChannel channel = new EmbeddedChannel(new DmlXmlFrameDecoder(32768));

        String combined = HELLO_MSG + "\n" + OBS_MSG;
        ByteBuf buf = Unpooled.wrappedBuffer(combined.getBytes(StandardCharsets.UTF_8));
        channel.writeInbound(buf);

        // 第一条消息
        String msg1 = channel.readInbound();
        assertNotNull(msg1, "First message should be decoded");
        assertTrue(msg1.contains("<HEL.R01>"), "First message should be HEL.R01");

        // 第二条消息
        String msg2 = channel.readInbound();
        assertNotNull(msg2, "Second message should be decoded");
        assertTrue(msg2.contains("<OBS.R01>"), "Second message should be OBS.R01");

        // 没有更多消息
        assertNull(channel.readInbound(), "No more messages expected");
    }

    @Test
    @DisplayName("消息前有 XML 声明 → 正确跳过并解码")
    void testXmlDeclaration() {
        EmbeddedChannel channel = new EmbeddedChannel(new DmlXmlFrameDecoder(32768));

        String withDecl = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + HELLO_MSG;
        ByteBuf buf = Unpooled.wrappedBuffer(withDecl.getBytes(StandardCharsets.UTF_8));
        channel.writeInbound(buf);

        String decoded = channel.readInbound();
        assertNotNull(decoded, "Should decode message with XML declaration");
        assertTrue(decoded.contains("<HEL.R01>"), "Decoded message should contain root element");
        assertTrue(decoded.endsWith("</HEL.R01>"), "Decoded message should end with closing tag");
    }

    @Test
    @DisplayName("消息不完整 → 等待更多数据")
    void testIncompleteMessage() {
        EmbeddedChannel channel = new EmbeddedChannel(new DmlXmlFrameDecoder(32768));

        // 只有开头标签，没有闭合
        String partial = "<HEL.R01><HDR><HDR.message_type V=\"HEL.R01\" />";
        ByteBuf buf = Unpooled.wrappedBuffer(partial.getBytes(StandardCharsets.UTF_8));
        channel.writeInbound(buf);

        assertNull(channel.readInbound(), "Incomplete message should not produce output");
    }

    @Test
    @DisplayName("超过最大消息大小 → 关闭连接")
    void testMaxMessageSizeExceeded() {
        EmbeddedChannel channel = new EmbeddedChannel(new DmlXmlFrameDecoder(100));

        // 创建超过 100 字节的消息
        StringBuilder sb = new StringBuilder("<HEL.R01>");
        while (sb.length() < 200) {
            sb.append("<DEV.filler V=\"xxxxxxxxxx\" />");
        }
        sb.append("</HEL.R01>");

        ByteBuf buf = Unpooled.wrappedBuffer(sb.toString().getBytes(StandardCharsets.UTF_8));
        channel.writeInbound(buf);

        // 通道应该被关闭
        assertFalse(channel.isActive(), "Channel should be closed when max message size exceeded");
    }

    @Test
    @DisplayName("消息后跟换行符 → 正确跳过尾部空白")
    void testTrailingNewline() {
        EmbeddedChannel channel = new EmbeddedChannel(new DmlXmlFrameDecoder(32768));

        // 第一条消息后跟换行符，然后第二条消息
        String combined = HELLO_MSG + "\r\n" + OBS_MSG;
        ByteBuf buf = Unpooled.wrappedBuffer(combined.getBytes(StandardCharsets.UTF_8));
        channel.writeInbound(buf);

        String msg1 = channel.readInbound();
        assertNotNull(msg1);
        assertTrue(msg1.endsWith("</HEL.R01>"), "First message should not include trailing newline");

        String msg2 = channel.readInbound();
        assertNotNull(msg2);
        assertTrue(msg2.contains("<OBS.R01>"), "Second message should be decoded correctly");
    }
}
