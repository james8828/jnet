package com.nova.bioconnect.icpmgr.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * DML XML Frame Decoder
 * Decodes incoming byte stream into complete XML messages
 * DML messages are delimited by XML structure boundaries
 */
@Slf4j
public class DmlXmlFrameDecoder extends ByteToMessageDecoder {

    private static final int MAX_FRAME_LENGTH = 32768;
    private static final int INITIAL_BUFFER_SIZE = 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 5) {
            return;
        }

        in.markReaderIndex();
        int readerIndex = in.readerIndex();

        // Find the start of XML message
        byte firstByte = in.readByte();
        if (firstByte != '<') {
            log.warn("Expected '<' but got '{}' at position {}", (char) firstByte, readerIndex);
            in.resetReaderIndex();
            // Skip non-XML bytes
            in.skipBytes(1);
            return;
        }

        // Look for the end of the XML message
        // DML messages are self-contained XML documents
        int depth = 0;
        boolean inTag = false;
        boolean inEndTag = false;
        int startPos = readerIndex;
        boolean foundComplete = false;

        while (in.isReadable()) {
            if (in.readableBytes() > MAX_FRAME_LENGTH) {
                log.warn("Frame exceeds max length of {} bytes", MAX_FRAME_LENGTH);
                in.resetReaderIndex();
                return;
            }

            byte b = in.readByte();

            if (b == '<') {
                inTag = true;
                inEndTag = false;
                if (in.isReadable() && in.getByte(in.readerIndex()) == '/') {
                    inEndTag = true;
                }
            } else if (b == '>' && inTag) {
                inTag = false;
                if (inEndTag) {
                    depth--;
                } else {
                    // Check if it's a self-closing tag
                    int currentPos = in.readerIndex();
                    if (currentPos > 0 && in.getByte(currentPos - 2) != '/') {
                        depth++;
                    }
                }

                // When depth reaches 0, we have a complete message
                if (depth == 0 && !inTag) {
                    int messageLength = in.readerIndex() - startPos;
                    if (messageLength > 0) {
                        ByteBuf frame = ctx.alloc().buffer(messageLength);
                        in.getBytes(startPos, frame, messageLength);
                        frame.writerIndex(messageLength);
                        out.add(frame.toString(StandardCharsets.UTF_8));
                        foundComplete = true;
                    }
                    break;
                }
            }
        }

        if (!foundComplete) {
            in.resetReaderIndex();
        }
    }
}