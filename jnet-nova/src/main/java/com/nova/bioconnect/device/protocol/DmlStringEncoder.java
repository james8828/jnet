package com.nova.bioconnect.device.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * DML String to Byte Encoder
 * Encodes String messages to bytes for transmission
 */
public class DmlStringEncoder extends MessageToByteEncoder<String> {

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) {
        if (msg != null && !msg.isEmpty()) {
            out.writeBytes(msg.getBytes(StandardCharsets.UTF_8));
        }
    }
}