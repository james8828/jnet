package com.nova.bioconnect.icpmgr.protocol;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DmlSession {

    @Getter
    private final String sessionId;

    @Getter
    private final Channel channel;

    @Getter
    private StateMachine<DmlState, DmlEvent> stateMachine;

    @Getter
    @Setter
    private String serialId;

    @Getter
    @Setter
    private String controlId;

    @Getter
    @Setter
    private String vendorId;

    @Getter
    @Setter
    private String locationNum;

    @Getter
    @Setter
    private String facility;

    @Getter
    @Setter
    private String deviceName;

    @Getter
    @Setter
    private String hwVersion;

    @Getter
    @Setter
    private String swVersion;

    @Getter
    @Setter
    private String deviceType;

    @Getter
    @Setter
    private String deviceClass;

    @Getter
    @Setter
    private String instNum;

    @Getter
    @Setter
    private String fromInstId;

    @Getter
    @Setter
    private String portType;

    @Getter
    @Setter
    private int portNum;

    @Getter
    @Setter
    private String instrumentIp;

    @Getter
    @Setter
    private String lastControlId;

    @Getter
    @Setter
    private int newObservationsQty;

    @Getter
    @Setter
    private int newEventsQty;

    @Getter
    @Setter
    private boolean continuous;

    @Getter
    @Setter
    private boolean setTimeSupported = true;

    @Getter
    @Setter
    private boolean continuousSupported;

    @Getter
    @Setter
    private boolean kpaEnabled = true;

    @Getter
    @Setter
    private boolean waiting;

    @Getter
    @Setter
    private boolean busy;

    @Getter
    @Setter
    private boolean isPartial;

    @Getter
    private final AtomicInteger kpaTimeoutCount = new AtomicInteger(0);

    @Getter
    @Setter
    private boolean waitingForResponse;

    private final StateMachineFactory<DmlState, DmlEvent> stateMachineFactory;

    public DmlSession(Channel channel, StateMachineFactory<DmlState, DmlEvent> stateMachineFactory) {
        this.sessionId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        this.channel = channel;
        this.stateMachineFactory = stateMachineFactory;
        initStateMachine();
        log.info("DML Session created: {}", sessionId);
    }

    @SuppressWarnings("unchecked")
    private void initStateMachine() {
        this.stateMachine = stateMachineFactory.getStateMachine(this.sessionId);
        configureStateMachine(this.stateMachine);
        this.stateMachine.startReactively().block();
    }

    private void configureStateMachine(StateMachine<DmlState, DmlEvent> sm) {
        sm.getStateMachineAccessor().doWithAllRegions(access -> {
            access.addStateMachineInterceptor(new DmlStateInterceptor(this));
        });
        sm.addStateListener(new DmlStateMachineListener(this));
    }

    public boolean sendEvent(DmlEvent event) {
        boolean accepted = stateMachine.sendEvent(event);
        log.debug("Session {} - Event {} accepted: {}", sessionId, event, accepted);
        return accepted;
    }

    public boolean sendEvent(DmlEvent event, java.util.Map<String, Object> variables) {
        stateMachine.getExtendedState().getVariables().putAll(variables);
        boolean accepted = stateMachine.sendEvent(event);
        log.debug("Session {} - Event {} (with vars) accepted: {}", sessionId, event, accepted);
        return accepted;
    }

    public DmlState getCurrentState() {
        return stateMachine.getState().getId();
    }

    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        Object value = stateMachine.getExtendedState().getVariables().get(key);
        return value != null ? (T) value : null;
    }

    public void setVariable(String key, Object value) {
        stateMachine.getExtendedState().getVariables().put(key, value);
    }

    public void incrementKpaTimeout() {
        kpaTimeoutCount.incrementAndGet();
    }

    public void resetKpaTimeout() {
        kpaTimeoutCount.set(0);
    }

    public boolean isKpaTimeoutExceeded() {
        return kpaTimeoutCount.get() >= 4;
    }

    public void sendMessage(String message) {
        if (channel.isActive()) {
            channel.writeAndFlush(message);
            log.debug("Session {} - Sent: {}", sessionId, message.length() > 200 ? message.substring(0, 200) + "..." : message);
        } else {
            log.warn("Session {} - Channel inactive, cannot send message", sessionId);
        }
    }

    public void close() {
        log.info("DML Session closing: {}", sessionId);
        try {
            stateMachine.stopReactively().block();
        } catch (Exception e) {
            log.error("Error stopping state machine for session {}", sessionId, e);
        }
        if (channel.isActive()) {
            channel.close();
        }
    }

    public boolean isActive() {
        return channel.isActive() && stateMachine.getState() != null;
    }

    public void setKpaTimeoutCount(int count) {
        this.kpaTimeoutCount.set(count);
    }
}