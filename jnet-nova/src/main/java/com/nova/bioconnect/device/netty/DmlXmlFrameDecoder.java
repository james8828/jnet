package com.nova.bioconnect.device.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * DML XML帧解码器
 * 处理TCP流中的XML消息边界
 * DML消息格式：XML消息以'<'开始，以'</XXX.R01>'或类似标签结束
 */
@Slf4j
public class DmlXmlFrameDecoder extends ByteToMessageDecoder {

    // 最大消息长度（防止内存溢出）
    private static final int MAX_MESSAGE_LENGTH = 1024 * 1024; // 1MB

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 至少需要1个字节才能判断是否是XML开始
        if (in.readableBytes() < 1) {
            return;
        }

        // 标记读取位置
        in.markReaderIndex();

        // 查找XML开始标记 '<'
        int startIndex = findXmlStart(in);
        if (startIndex == -1) {
            // 没有找到开始标记，丢弃所有数据
            in.skipBytes(in.readableBytes());
            return;
        }

        // 移动到开始位置
        if (startIndex > 0) {
            in.skipBytes(startIndex);
        }

        // 检查消息长度限制
        if (in.readableBytes() > MAX_MESSAGE_LENGTH) {
            log.error("Message too long: {} bytes", in.readableBytes());
            in.skipBytes(in.readableBytes());
            return;
        }

        // 读取完整的XML消息
        byte[] messageBytes = readCompleteXmlMessage(in);
        if (messageBytes == null) {
            // 消息不完整，重置读取位置等待更多数据
            in.resetReaderIndex();
            return;
        }

        // 解析消息
        String message = new String(messageBytes, StandardCharsets.UTF_8);
        log.debug("Decoded DML message: {}", message.length() > 200 ? message.substring(0, 200) + "..." : message);

        out.add(message);
    }

    /**
     * 查找XML开始标记
     */
    private int findXmlStart(ByteBuf buf) {
        int len = buf.readableBytes();
        for (int i = buf.readerIndex(); i < len; i++) {
            byte b = buf.getByte(i);
            if (b == '<') {
                return i - buf.readerIndex();
            }
        }
        return -1;
    }

    /**
     * 读取完整的XML消息
     * DML消息格式：<MSG.TYPE>...</MSG.TYPE>
     */
    private byte[] readCompleteXmlMessage(ByteBuf buf) {
        int startIndex = buf.readerIndex();
        int endIndex = -1;
        int depth = 0;
        String rootElement = null;
        boolean inString = false;

        for (int i = startIndex; i < buf.writerIndex(); i++) {
            byte b = buf.getByte(i);

            // 处理引号内的内容（不解析标签）
            if (b == '"' || b == '\'') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            // 处理标签
            if (b == '<') {
                // 检查是否是结束标签 </
                if (i + 1 < buf.writerIndex() && buf.getByte(i + 1) == '/') {
                    depth--;
                    if (depth == 0 && rootElement != null) {
                        // 找到根元素结束标签
                        String endTag = "</" + rootElement + ">";
                        int tagLen = endTag.length();
                        if (i + tagLen <= buf.writerIndex()) {
                            byte[] tagBytes = new byte[tagLen];
                            buf.getBytes(i, tagBytes);
                            if (endTag.equals(new String(tagBytes, StandardCharsets.UTF_8))) {
                                endIndex = i + tagLen;
                                break;
                            }
                        }
                    }
                } else {
                    // 开始标签
                    if (depth == 0) {
                        // 提取根元素名称
                        rootElement = extractElementName(buf, i + 1);
                    }
                    depth++;
                }
            } else if (b == '>') {
                // 自闭合标签 <xxx/>
                if (i > 0 && buf.getByte(i - 1) == '/') {
                    depth--;
                }
            }
        }

        if (endIndex > 0) {
            int length = endIndex - startIndex;
            byte[] messageBytes = new byte[length];
            buf.getBytes(startIndex, messageBytes);
            return messageBytes;
        }

        return null;
    }

    /**
     * 提取元素名称
     */
    private String extractElementName(ByteBuf buf, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < buf.writerIndex(); i++) {
            byte b = buf.getByte(i);
            if (b == '>' || b == ' ' || b == '/' || b == '\t' || b == '\n' || b == '\r') {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }
}