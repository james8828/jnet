package com.nova.bioconnect.rtm.mllp;

import com.nova.bioconnect.rtm.hl7.Hl7Constants;
import com.nova.bioconnect.rtm.hl7.Hl7Encoder;
import com.nova.bioconnect.rtm.hl7.Hl7Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Minimal Lower Layer Protocol (MLLP) frame encoder.
 *
 * <p>Wraps an {@link Hl7Message} with the MLLP start-block (0x0B) and end-block (0x1C 0x0D):
 * <pre>
 *   0x0B &lt;HL7 message&gt; 0x1C 0x0D
 * </pre>
 */
public class MllpEncoder extends MessageToByteEncoder<Hl7Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Hl7Message msg, ByteBuf out) {
        out.writeByte(Hl7Constants.MLLP_START);
        ByteBufUtil.writeUtf8(out, Hl7Encoder.encodeTerminated(msg));
        out.writeByte(Hl7Constants.MLLP_END);
        out.writeByte(Hl7Constants.MLLP_CR);
    }
}
