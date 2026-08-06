package com.nova.bioconnect.icpmgr.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * DML字符串编码器
 * 将String编码为ByteBuf发送
 */
@Slf4j
public class DmlStringEncoder extends MessageToByteEncoder<String> {

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
        if (msg != null && !msg.isEmpty()) {
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            out.writeBytes(bytes);
            log.debug("Encoded message: {} bytes", bytes.length);
        }
    }
}