package com.nova.bioconnect.novanet.client;

import com.nova.bioconnect.novanet.hl7.Hl7Constants;
import com.nova.bioconnect.novanet.hl7.Hl7Message;
import com.nova.bioconnect.novanet.hl7.Hl7Parser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inbound handler for an HL7 MLLP client. Correlates incoming ACK messages with pending
 * outbound requests via the message control id (MSA-2 echoes the original MSH-10).
 */
public class Hl7ClientHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(Hl7ClientHandler.class);

    private final Hl7Client client;

    public Hl7ClientHandler(Hl7Client client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String raw) {
        Hl7Message message = Hl7Parser.parse(raw);
        if (!Hl7Parser.isValid(message)) {
            log.warn("[{}] Received invalid HL7 message from server: {}", client.getName(), raw.replace('\r', '\n'));
            return;
        }
        if (Hl7Constants.MSG_ACK.equals(message.getMessageTypeCode())) {
            String ackedId = message.getSegment(Hl7Constants.MSA)
                    .map(s -> s.getField(2)).orElse("");
            client.completePending(ackedId, message);
            log.debug("[{}] ACK received for control id {}: {}",
                    client.getName(), ackedId, message.getSegment(Hl7Constants.MSA)
                            .map(s -> s.getField(1)).orElse(""));
        } else {
            log.warn("[{}] Unexpected non-ACK message from server: {} {}",
                    client.getName(), message.getMessageType(), message.getMessageControlId());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("[{}] Connection closed; scheduling reconnect", client.getName());
        client.scheduleReconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[{}] Channel exception", client.getName(), cause);
        ctx.close();
    }
}
