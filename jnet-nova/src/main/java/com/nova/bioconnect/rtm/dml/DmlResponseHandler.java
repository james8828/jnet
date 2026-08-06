package com.nova.bioconnect.rtm.dml;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DmlResponseHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(DmlResponseHandler.class);

    private final DmlMessageParser parser;
    private final Map<String, CompletableFuture<String>> pendingResponses;

    public DmlResponseHandler(DmlMessageParser parser, Map<String, CompletableFuture<String>> pendingResponses) {
        this.parser = parser;
        this.pendingResponses = pendingResponses;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String message) {
        log.debug("DML response received: {}", message.length() > 200 ? message.substring(0, 200) + "..." : message);
        DmlMessage parsed = parser.parse(message);
        String messageId = parsed.messageId();
        CompletableFuture<String> future = pendingResponses.remove(messageId);
        if (future != null) {
            future.complete(message);
        } else {
            log.warn("No pending request for messageId: {}", messageId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("DML response error", cause);
    }
}