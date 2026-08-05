package com.nova.bioconnect.novanet.mllp;

import com.nova.bioconnect.novanet.hl7.Hl7Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Minimal Lower Layer Protocol (MLLP) frame decoder.
 *
 * <p>MLLP frames an HL7 message as:
 * <pre>
 *   0x0B &lt;HL7 message&gt; 0x1C 0x0D
 * </pre>
 * where 0x0B is the start-block (VT), 0x1C is the end-block (FS) and 0x0D is the
 * trailing carriage return (CR). This decoder extracts the HL7 message payload as a
 * {@link String} (UTF-8 by default) and passes it downstream.
 *
 * <p>It tolerates a missing trailing CR after the end-block for interoperability.
 */
public class MllpFrameDecoder extends ByteToMessageDecoder {

    private final Charset charset;

    public MllpFrameDecoder() {
        this(Charset.forName("UTF-8"));
    }

    public MllpFrameDecoder(Charset charset) {
        this.charset = charset;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // Locate the start-block character.
        int startIdx = in.indexOf(in.readerIndex(), in.writerIndex(), Hl7Constants.MLLP_START);
        if (startIdx < 0) {
            // Discard everything before a possible future start byte to avoid unbounded growth.
            if (in.readableBytes() > 1) {
                in.skipBytes(in.readableBytes() - 1);
            }
            return;
        }
        // Skip to (and past) the start-block.
        in.skipBytes(startIdx - in.readerIndex() + 1);

        // Locate the end-block character from the current reader index.
        int endIdx = in.indexOf(in.readerIndex(), in.writerIndex(), Hl7Constants.MLLP_END);
        if (endIdx < 0) {
            // Not yet fully received; wait for more data. (The bytes skipped above are now in
            // the readable region; ByteToMessageDecoder will accumulate.)
            return;
        }

        int payloadLen = endIdx - in.readerIndex();
        if (payloadLen < 0) {
            payloadLen = 0;
        }
        byte[] payload = new byte[payloadLen];
        in.readBytes(payload);

        // Consume the end-block.
        in.skipBytes(1);

        // Consume the trailing CR if present.
        if (in.isReadable() && in.getByte(in.readerIndex()) == Hl7Constants.MLLP_CR) {
            in.skipBytes(1);
        }

        out.add(new String(payload, charset));
    }
}
