package com.nova.bioconnect.device.protocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;

@Slf4j
public class DmlStateInterceptor extends StateMachineInterceptorAdapter<DmlState, DmlEvent> {

    private final DmlSession session;

    public DmlStateInterceptor(DmlSession session) {
        this.session = session;
    }

    @Override
    public StateContext<DmlState, DmlEvent> preTransition(
            StateContext<DmlState, DmlEvent> context) {
        DmlState source = context.getSource() != null ? context.getSource().getId() : null;
        DmlState target = context.getTarget() != null ? context.getTarget().getId() : null;
        DmlEvent event = context.getEvent();

        log.debug("Session {} - Pre-transition: {} → {} via {}",
                session.getSessionId(), source, target, event);

        return context;
    }

    @Override
    public StateContext<DmlState, DmlEvent> postTransition(
            StateContext<DmlState, DmlEvent> context) {
        DmlState target = context.getTarget() != null ? context.getTarget().getId() : null;

        log.debug("Session {} - Post-transition to state: {}", session.getSessionId(), target);
        return context;
    }

    @Override
    public Exception stateMachineError(
            StateMachine<DmlState, DmlEvent> stateMachine,
            Exception exception) {
        DmlState currentState = stateMachine.getState() != null ? stateMachine.getState().getId() : null;
        log.error("Session {} - StateMachine error in state {}",
                session.getSessionId(), currentState, exception);
        return exception;
    }
}