package com.nova.bioconnect.rtm.server;

import com.nova.bioconnect.rtm.config.BioConnectProperties;
import com.nova.bioconnect.rtm.hl7.Hl7Constants;
import com.nova.bioconnect.rtm.hl7.Hl7Message;
import com.nova.bioconnect.rtm.hl7.Hl7Parser;
import com.nova.bioconnect.rtm.message.AckBuilder;
import com.nova.bioconnect.rtm.service.AdtService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inbound HL7 MLLP server handler.
 *
 * <p>Routes ADT messages (from HIS) to {@link AdtService} for RTMADTP patient data exchange.
 * Device observation/QC results are received via the DML protocol ({@code DmlTcpServer}), not
 * through this HL7 server, so ORU/OUL messages are not handled here.
 *
 * <p>Sharable and stateless: a single instance is reused across all channels.
 */
@ChannelHandler.Sharable
public class Hl7ServerHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(Hl7ServerHandler.class);

    private final AdtService adtService;
    private final BioConnectProperties properties;

    public Hl7ServerHandler(AdtService adtService, BioConnectProperties properties) {
        this.adtService = adtService;
        this.properties = properties;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String raw) {
        Hl7Message message = Hl7Parser.parse(raw);
        String processingId = properties.getProcessingId();
        String version = properties.getVersion();

        if (!Hl7Parser.isValid(message)) {
            log.warn("Inbound: invalid HL7 message, returning AE: {}", raw.replace('\r', '\n'));
            Hl7Message ack = AckBuilder.build(message, Hl7Constants.ACK_AE, processingId, version);
            ctx.writeAndFlush(ack);
            return;
        }

        String typeCode = message.getMessageTypeCode();
        String fullType = message.getMessageType();
        log.info("Inbound {} (control id {})", fullType, message.getMessageControlId());

        Hl7Message ack;
        switch (typeCode) {
            case Hl7Constants.MSG_ADT -> ack = adtService.handleInbound(message);
            case Hl7Constants.MSG_ACK -> {
                log.info("Inbound ACK received on server (control id {}); no response sent",
                        message.getMessageControlId());
                return;
            }
            default -> {
                log.warn("Inbound: unsupported message type '{}' on ADT server; returning AE", fullType);
                ack = AckBuilder.build(message, Hl7Constants.ACK_AE, processingId, version);
            }
        }
        ctx.writeAndFlush(ack);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Inbound connection from {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Inbound connection closed: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Inbound channel exception from {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}