package com.nova.bioconnect.device.protocol;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DML Server Handler
 * Core handler that processes incoming DML messages and manages sessions
 * Marked as @Sharable equivalent via prototype scope
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DmlServerHandler extends SimpleChannelInboundHandler<String> {

    private final StateMachineFactory<DmlState, DmlEvent> stateMachineFactory;
    private final DmlMessageHandler messageHandler;
    private final DmlMessageBuilder messageBuilder;

    private static final Map<String, DmlSession> sessions = new ConcurrentHashMap<>();
    private static final io.netty.util.AttributeKey<DmlSession> SESSION_KEY =
            io.netty.util.AttributeKey.valueOf("dml_session");

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.info("Channel active: {}", channel.remoteAddress());

        // Create new session for this connection
        DmlSession session = new DmlSession(channel, stateMachineFactory);
        channel.attr(SESSION_KEY).set(session);
        sessions.put(session.getSessionId(), session);

        log.info("Session created: {} for {}", session.getSessionId(), channel.remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        DmlSession session = channel.attr(SESSION_KEY).get();
        if (session != null) {
            log.info("Channel inactive, closing session: {}", session.getSessionId());
            sessions.remove(session.getSessionId());
            session.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String message) {
        DmlSession session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null) {
            log.error("No session found for channel: {}", ctx.channel().remoteAddress());
            return;
        }

        log.info("Received message for session {}: {}",
                session.getSessionId(),
                message.length() > 100 ? message.substring(0, 100) + "..." : message);

        try {
            // Handle the message
            List<String> responses = messageHandler.handleMessage(message, session);

            // Send responses back to device
            if (responses != null && !responses.isEmpty()) {
                for (String response : responses) {
                    session.sendMessage(response);
                }
            }

        } catch (Exception e) {
            log.error("Error processing message for session {}", session.getSessionId(), e);
            // Try to send error response
            try {
                String errorResponse = messageBuilder.buildEscapeMessage(
                        "Internal error: " + e.getMessage(), session);
                session.sendMessage(errorResponse);
            } catch (Exception ex) {
                log.error("Error sending escape message", ex);
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        DmlSession session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null) {
            return;
        }

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            IdleState state = idleEvent.state();

            switch (state) {
                case READER_IDLE:
                    log.warn("Read idle for session {}, resetting KPA", session.getSessionId());
                    session.resetKpaTimeout();
                    break;

                case WRITER_IDLE:
                    handleWriterIdle(session);
                    break;

                case ALL_IDLE:
                    log.warn("All idle for session {}", session.getSessionId());
                    break;
            }
        }
    }

    /**
     * Handle writer idle - send keep alive or check timeout
     */
    private void handleWriterIdle(DmlSession session) {
        if (session.isContinuous() && !session.isWaiting() && !session.isBusy()) {
            // In continuous mode, send keep alive
            if (session.isKpaEnabled()) {
                String kpaMsg = messageBuilder.buildKeepAliveMessage(session);
                session.sendMessage(kpaMsg);
                session.setWaiting(true);
                session.setWaitingForResponse(true);
            }
        } else {
            // Check KPA timeout
            session.incrementKpaTimeout();
            if (session.isKpaTimeoutExceeded()) {
                log.warn("KPA timeout exceeded for session {}, shutting down", session.getSessionId());
                session.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        DmlSession session = ctx.channel().attr(SESSION_KEY).get();
        String sessionId = session != null ? session.getSessionId() : "unknown";
        log.error("Exception in session {}: {}", sessionId, cause.getMessage(), cause);

        // Close the channel on error
        ctx.close();
    }

    /**
     * Get session by ID
     */
    public DmlSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Get all active sessions
     */
    public Map<String, DmlSession> getAllSessions() {
        return new ConcurrentHashMap<>(sessions);
    }

    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Close session by ID
     */
    public boolean closeSession(String sessionId) {
        DmlSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
            return true;
        }
        return false;
    }
}