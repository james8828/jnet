package com.nova.bioconnect.rtm.dml;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DmlFrameDecoder extends ByteToMessageDecoder {

    private static final String MESSAGE_END = "</Message>";
    private static final String MESSAGE_START = "<Message";

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (in.readableBytes() >= MESSAGE_END.length()) {
            int endIdx = indexOf(in, MESSAGE_END);
            if (endIdx < 0) {
                break;
            }

            int startIdx = indexOf(in, MESSAGE_START);
            if (startIdx < 0 || startIdx > endIdx) {
                in.skipBytes(endIdx + MESSAGE_END.length() - in.readerIndex());
                continue;
            }

            int payloadLen = endIdx + MESSAGE_END.length() - startIdx;
            byte[] payload = new byte[payloadLen];
            in.getBytes(startIdx, payload);
            in.readerIndex(endIdx + MESSAGE_END.length());

            out.add(new String(payload, StandardCharsets.UTF_8));
        }

        if (in.readableBytes() > 0 && indexOf(in, MESSAGE_END) < 0) {
            if (in.readableBytes() > 1024 * 1024) {
                in.clear();
            }
        }
    }

    private int indexOf(ByteBuf haystack, String needle) {
        byte[] needleBytes = needle.getBytes(StandardCharsets.UTF_8);
        for (int i = haystack.readerIndex(); i <= haystack.writerIndex() - needleBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < needleBytes.length; j++) {
                if (haystack.getByte(i + j) != needleBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }
}