package com.nova.bioconnect.icpmgr.protocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;

import java.util.EnumSet;

/**
 * DML Protocol State Machine Configuration
 * Based on C# DMLProtocol.StepProtocolState() 45-state machine
 *
 * State Machine Flow:
 * EXCEPTION → HELLO_RECEIVED → ACK_HELLO → REQ_OBS → OBS_EOT → REQ_EVS → EVS_EOT
 *   → SET_TIME → SET_TIME_ACK → SETUP_SENT → SETUP_SENT_WAITING_ACK → SETUP_EOT
 *   → WIFI_SETUP_SENT → WIFI_SETUP_SENT_WAITING_ACK → WIFI_SETUP_EOT
 *   → WIFI_CERT_SENT → WIFI_CERT_SENT_WAITING_ACK → WIFI_CERT_EOT
 *   → LOC_SENT → LOC_SENT_WAITING_ACK → LOC_EOT
 *   → OPL_SENT → OPL_SENT_WAITING_ACK → OPL_EOT
 *   → PTL_SENT → PTL_SENT_WAITING_ACK → PTL_EOT
 *   → PHYS_SENT → PHYS_SENT_WAITING_ACK → PHYS_EOT
 *   → FIRM_SENT → FIRM_SENT_WAITING_ACK → FIRM_EOT
 *   → REAG_SENT → REAG_SENT_WAITING_ACK → REAG_EOT
 *   → [CONTINUOUS | TERMINATE]
 */
@Slf4j
@Configuration
@EnableStateMachineFactory
public class DmlStateMachineConfig extends StateMachineConfigurerAdapter<DmlState, DmlEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<DmlState, DmlEvent> states) throws Exception {
        states
            .withStates()
                .initial(DmlState.EXCEPTION)
                .states(EnumSet.allOf(DmlState.class))
                .end(DmlState.TERMINATE);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<DmlState, DmlEvent> transitions) throws Exception {
        transitions
            // === HELLO Sequence ===
            .withExternal()
                .source(DmlState.EXCEPTION).target(DmlState.HELLO_RECEIVED)
                .event(DmlEvent.HEL_RECEIVED)
                .and()
            .withExternal()
                .source(DmlState.HELLO_RECEIVED).target(DmlState.ACK_HELLO)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === OBS Sequence ===
            .withExternal()
                .source(DmlState.ACK_HELLO).target(DmlState.REQ_OBS)
                .event(DmlEvent.DST_RECEIVED)
                .and()
            .withExternal()
                .source(DmlState.REQ_OBS).target(DmlState.OBS_EOT)
                .event(DmlEvent.OBS_EOT_RECEIVED)
                .and()

            // === EVS Sequence ===
            .withExternal()
                .source(DmlState.OBS_EOT).target(DmlState.REQ_EVS)
                .event(DmlEvent.SEND_EVS_REQUEST)
                .and()
            .withExternal()
                .source(DmlState.REQ_EVS).target(DmlState.EVS_EOT)
                .event(DmlEvent.EVS_EOT_RECEIVED)
                .and()

            // === SET_TIME Sequence ===
            .withExternal()
                .source(DmlState.EVS_EOT).target(DmlState.SET_TIME)
                .event(DmlEvent.SEND_SET_TIME)
                .and()
            .withExternal()
                .source(DmlState.SET_TIME).target(DmlState.SET_TIME_ACK)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === SETUP Sequence ===
            .withExternal()
                .source(DmlState.SET_TIME_ACK).target(DmlState.SETUP_SENT)
                .event(DmlEvent.SEND_SETUP)
                .and()
            .withExternal()
                .source(DmlState.SETUP_SENT).target(DmlState.SETUP_SENT_WAITING_ACK)
                .event(DmlEvent.SETUP_SENT_COMPLETE)
                .and()
            .withExternal()
                .source(DmlState.SETUP_SENT_WAITING_ACK).target(DmlState.SETUP_EOT)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === WIFI_SETUP Sequence ===
            .withExternal()
                .source(DmlState.SETUP_EOT).target(DmlState.WIFI_SETUP_SENT)
                .event(DmlEvent.SEND_WIFI_SETUP)
                .and()
            .withExternal()
                .source(DmlState.WIFI_SETUP_SENT).target(DmlState.WIFI_SETUP_SENT_WAITING_ACK)
                .event(DmlEvent.WIFI_SETUP_SENT_COMPLETE)
                .and()
            .withExternal()
                .source(DmlState.WIFI_SETUP_SENT_WAITING_ACK).target(DmlState.WIFI_SETUP_EOT)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === WIFI_CERT Sequence ===
            .withExternal()
                .source(DmlState.WIFI_SETUP_EOT).target(DmlState.WIFI_CERT_SENT)
                .event(DmlEvent.SEND_WIFI_CERT)
                .and()
            .withExternal()
                .source(DmlState.WIFI_CERT_SENT).target(DmlState.WIFI_CERT_SENT_WAITING_ACK)
                .event(DmlEvent.WIFI_CERT_SENT_COMPLETE)
                .and()
            .withExternal()
                .source(DmlState.WIFI_CERT_SENT_WAITING_ACK).target(DmlState.WIFI_CERT_EOT)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === LOC Sequence ===
            .withExternal()
                .source(DmlState.WIFI_CERT_EOT).target(DmlState.LOC_SENT)
                .event(DmlEvent.SEND_LOCATION)
                .and()
            .withExternal()
                .source(DmlState.LOC_SENT).target(DmlState.LOC_SENT_WAITING_ACK)
                .event(DmlEvent.LOC_SENT_COMPLETE)
                .and()
            .withExternal()
                .source(DmlState.LOC_SENT_WAITING_ACK).target(DmlState.LOC_EOT)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === OPL Sequence ===
            .withExternal()
                .source(DmlState.LOC_EOT).target(DmlState.OPL_SENT)
                .event(DmlEvent.SEND_OPERATOR)
                .and()
            .withExternal()
                .source(DmlState.OPL_SENT).target(DmlState.OPL_SENT_WAITING_ACK)
                .event(DmlEvent.OPL_SENT_COMPLETE)
                .and()
            .withExternal()
                .source(DmlState.OPL_SENT_WAITING_ACK).target(DmlState.OPL_EOT)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === PTL Sequence ===
            .withExternal()
                .source(DmlState.OPL_EOT).target(DmlState.PTL_SENT)
                .event(DmlEvent.SEND_PATIENT)
                .and()
            .withExternal()
                .source(DmlState.PTL_SENT).target(DmlState.PTL_SENT_WAITING_ACK)
                .event(DmlEvent.PTL_SENT_COMPLETE)
                .and()
            .withExternal()
                .source(DmlState.PTL_SENT_WAITING_ACK).target(DmlState.PTL_EOT)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === PHYS Sequence ===
            .withExternal()
                .source(DmlState.PTL_EOT).target(DmlState.PHYS_SENT)
                .event(DmlEvent.SEND_PHYSICIAN)
                .and()
            .withExternal()
                .source(DmlState.PHYS_SENT).target(DmlState.PHYS_SENT_WAITING_ACK)
                .event(DmlEvent.PHYS_SENT_COMPLETE)
                .and()
            .withExternal()
                .source(DmlState.PHYS_SENT_WAITING_ACK).target(DmlState.PHYS_EOT)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === FIRM Sequence ===
            .withExternal()
                .source(DmlState.PHYS_EOT).target(DmlState.FIRM_SENT)
                .event(DmlEvent.SEND_FIRMWARE)
                .and()
            .withExternal()
                .source(DmlState.FIRM_SENT).target(DmlState.FIRM_SENT_WAITING_ACK)
                .event(DmlEvent.FIRM_SENT_COMPLETE)
                .and()
            .withExternal()
                .source(DmlState.FIRM_SENT_WAITING_ACK).target(DmlState.FIRM_EOT)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === REAG Sequence ===
            .withExternal()
                .source(DmlState.FIRM_EOT).target(DmlState.REAG_SENT)
                .event(DmlEvent.SEND_REAGENT)
                .and()
            .withExternal()
                .source(DmlState.REAG_SENT).target(DmlState.REAG_SENT_WAITING_ACK)
                .event(DmlEvent.REAG_SENT_COMPLETE)
                .and()
            .withExternal()
                .source(DmlState.REAG_SENT_WAITING_ACK).target(DmlState.REAG_EOT)
                .event(DmlEvent.ACK_RECEIVED)
                .and()

            // === DECISION: REAG_EOT → CONTINUOUS or TERMINATE ===
            .withExternal()
                .source(DmlState.REAG_EOT).target(DmlState.CONTINUOUS)
                .event(DmlEvent.SEND_CONTINUOUS)
                .and()
            .withExternal()
                .source(DmlState.REAG_EOT).target(DmlState.TERMINATE)
                .event(DmlEvent.SEND_TERMINATE)
                .and()

            // === CONTINUOUS Loop: back to SET_TIME ===
            .withExternal()
                .source(DmlState.CONTINUOUS).target(DmlState.SET_TIME_ACK)
                .event(DmlEvent.SEND_EOT)
                .and()

            // === QUERY states ===
            .withExternal()
                .source(DmlState.REQ_OBS).target(DmlState.QUERY_RCV)
                .event(DmlEvent.QUERY_RECEIVED)
                .and()
            .withExternal()
                .source(DmlState.QUERY_RCV).target(DmlState.REQ_OBS)
                .event(DmlEvent.SEND_OBS_REQUEST)
                .and()

            // === SYSTEM_STATUS states ===
            .withExternal()
                .source(DmlState.REAG_EOT).target(DmlState.SYSTEM_STATUS_RCV)
                .event(DmlEvent.SYSTEM_STATUS_RECEIVED)
                .and()
            .withExternal()
                .source(DmlState.SYSTEM_STATUS_RCV).target(DmlState.SYSTEM_STATUS_WAITING_ACK)
                .event(DmlEvent.ACK_RECEIVED)
                .and()
            .withExternal()
                .source(DmlState.SYSTEM_STATUS_WAITING_ACK).target(DmlState.CONTINUOUS_ACK)
                .event(DmlEvent.SEND_EOT)
                .and()

            // === RC_COMMAND ===
            .withExternal()
                .source(DmlState.REQ_OBS).target(DmlState.RC_COMMAND_SENT)
                .event(DmlEvent.QUERY_RECEIVED)
                .and()

            // === ESC / END → TERMINATE ===
            .withExternal()
                .source(DmlState.REQ_OBS).target(DmlState.TERMINATE)
                .event(DmlEvent.ESC_RECEIVED)
                .and()
            .withExternal()
                .source(DmlState.REQ_EVS).target(DmlState.TERMINATE)
                .event(DmlEvent.ESC_RECEIVED)
                .and()
            .withExternal()
                .source(DmlState.HELLO_RECEIVED).target(DmlState.TERMINATE)
                .event(DmlEvent.END_RECEIVED);
    }

    /**
     * Guard: Check if there are observations to send
     */
    public Guard<DmlState, DmlEvent> hasObservations() {
        return context -> {
            Integer obsCount = context.getExtendedState().get("new_observations_qty", Integer.class);
            return obsCount != null && obsCount > 0;
        };
    }

    /**
     * Guard: Check if there are events to send
     */
    public Guard<DmlState, DmlEvent> hasEvents() {
        return context -> {
            Integer evsCount = context.getExtendedState().get("new_events_qty", Integer.class);
            return evsCount != null && evsCount > 0;
        };
    }

    /**
     * Guard: Check if device supports set time
     */
    public Guard<DmlState, DmlEvent> isSetTimeSupported() {
        return context -> {
            Boolean supported = context.getExtendedState().get("set_time_supported", Boolean.class);
            return supported != null && supported;
        };
    }

    /**
     * Guard: Check if device supports continuous mode
     */
    public Guard<DmlState, DmlEvent> isContinuousSupported() {
        return context -> {
            Boolean supported = context.getExtendedState().get("continuous_supported", Boolean.class);
            return supported != null && supported;
        };
    }
}