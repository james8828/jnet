package com.nova.bioconnect.device.protocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;

@Slf4j
public class DmlStateMachineListener extends org.springframework.statemachine.listener.StateMachineListenerAdapter<DmlState, DmlEvent> {

    private final DmlSession session;

    public DmlStateMachineListener(DmlSession session) {
        this.session = session;
    }

    @Override
    public void stateChanged(State<DmlState, DmlEvent> from, State<DmlState, DmlEvent> to) {
        log.info("Session {} - State: {} → {}",
                session.getSessionId(),
                from != null ? from.getId() : "INIT",
                to.getId());
    }

    @Override
    public void transition(Transition<DmlState, DmlEvent> transition) {
        DmlEvent event = transition.getTrigger() != null ? transition.getTrigger().getEvent() : null;
        log.debug("Session {} - Transition: {} → {} via {}",
                session.getSessionId(),
                transition.getSource().getId(),
                transition.getTarget().getId(),
                event);
    }

    @Override
    public void stateMachineStarted(StateMachine<DmlState, DmlEvent> stateMachine) {
        log.info("Session {} - StateMachine started", session.getSessionId());
    }

    @Override
    public void stateMachineStopped(StateMachine<DmlState, DmlEvent> stateMachine) {
        log.info("Session {} - StateMachine stopped", session.getSessionId());
    }

    @Override
    public void stateMachineError(StateMachine<DmlState, DmlEvent> stateMachine, Exception exception) {
        log.error("Session {} - StateMachine error", session.getSessionId(), exception);
    }
}