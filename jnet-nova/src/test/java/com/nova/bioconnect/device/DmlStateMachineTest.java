package com.nova.bioconnect.device;

import com.nova.bioconnect.device.protocol.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DML StateMachine Integration Tests
 */
@SpringBootTest
class DmlStateMachineTest {

    @Autowired
    private StateMachineFactory<DmlState, DmlEvent> stateMachineFactory;

    private StateMachine<DmlState, DmlEvent> stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = stateMachineFactory.getStateMachine("test");
        stateMachine.startReactively().block();
    }

    @Test
    @DisplayName("StateMachine should start in EXCEPTION state")
    void testInitialState() {
        assertEquals(DmlState.EXCEPTION, stateMachine.getState().getId());
    }

    @Test
    @DisplayName("HEL_RECEIVED event should transition EXCEPTION to HELLO_RECEIVED")
    void testHelloTransition() {
        boolean accepted = stateMachine.sendEvent(DmlEvent.HEL_RECEIVED);
        assertTrue(accepted, "HEL_RECEIVED event should be accepted");
        assertEquals(DmlState.HELLO_RECEIVED, stateMachine.getState().getId());
    }

    @Test
    @DisplayName("ACK_RECEIVED in HELLO_RECEIVED should transition to ACK_HELLO")
    void testAckHelloTransition() {
        stateMachine.sendEvent(DmlEvent.HEL_RECEIVED);
        boolean accepted = stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        assertTrue(accepted, "ACK_RECEIVED event should be accepted");
        assertEquals(DmlState.ACK_HELLO, stateMachine.getState().getId());
    }

    @Test
    @DisplayName("Full protocol flow: HEL → ACK → DST → OBS → EVS → SET_TIME → SETUP → WIFI → ...")
    void testFullProtocolFlow() {
        // HEL → ACK_HELLO
        assertTrue(stateMachine.sendEvent(DmlEvent.HEL_RECEIVED));
        assertTrue(stateMachine.sendEvent(DmlEvent.ACK_RECEIVED));
        assertEquals(DmlState.ACK_HELLO, stateMachine.getState().getId());

        // ACK_HELLO → REQ_OBS
        assertTrue(stateMachine.sendEvent(DmlEvent.DST_RECEIVED));
        assertEquals(DmlState.REQ_OBS, stateMachine.getState().getId());

        // REQ_OBS → OBS_EOT
        assertTrue(stateMachine.sendEvent(DmlEvent.OBS_EOT_RECEIVED));
        assertEquals(DmlState.OBS_EOT, stateMachine.getState().getId());

        // OBS_EOT → REQ_EVS
        assertTrue(stateMachine.sendEvent(DmlEvent.SEND_EVS_REQUEST));
        assertEquals(DmlState.REQ_EVS, stateMachine.getState().getId());

        // REQ_EVS → EVS_EOT
        assertTrue(stateMachine.sendEvent(DmlEvent.EVS_EOT_RECEIVED));
        assertEquals(DmlState.EVS_EOT, stateMachine.getState().getId());

        // EVS_EOT → SET_TIME
        assertTrue(stateMachine.sendEvent(DmlEvent.SEND_SET_TIME));
        assertEquals(DmlState.SET_TIME, stateMachine.getState().getId());

        // SET_TIME → SET_TIME_ACK
        assertTrue(stateMachine.sendEvent(DmlEvent.ACK_RECEIVED));
        assertEquals(DmlState.SET_TIME_ACK, stateMachine.getState().getId());

        // SET_TIME_ACK → SETUP_SENT
        assertTrue(stateMachine.sendEvent(DmlEvent.SEND_SETUP));
        assertEquals(DmlState.SETUP_SENT, stateMachine.getState().getId());

        // SETUP_SENT → SETUP_SENT_WAITING_ACK
        assertTrue(stateMachine.sendEvent(DmlEvent.SETUP_SENT_COMPLETE));
        assertEquals(DmlState.SETUP_SENT_WAITING_ACK, stateMachine.getState().getId());
    }

    @Test
    @DisplayName("ESC_RECEIVED should transition to TERMINATE from REQ_OBS")
    void testEscapeTransition() {
        stateMachine.sendEvent(DmlEvent.HEL_RECEIVED);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.DST_RECEIVED);
        assertEquals(DmlState.REQ_OBS, stateMachine.getState().getId());

        boolean accepted = stateMachine.sendEvent(DmlEvent.ESC_RECEIVED);
        assertTrue(accepted);
        assertEquals(DmlState.TERMINATE, stateMachine.getState().getId());
    }

    @Test
    @DisplayName("CONTINUOUS mode: REAG_EOT → CONTINUOUS → SET_TIME_ACK loop")
    void testContinuousMode() {
        // Navigate to REAG_EOT
        navigateToReagEot();

        // REAG_EOT → CONTINUOUS
        assertTrue(stateMachine.sendEvent(DmlEvent.SEND_CONTINUOUS));
        assertEquals(DmlState.CONTINUOUS, stateMachine.getState().getId());

        // CONTINUOUS → SET_TIME_ACK
        assertTrue(stateMachine.sendEvent(DmlEvent.SEND_EOT));
        assertEquals(DmlState.SET_TIME_ACK, stateMachine.getState().getId());
    }

    @Test
    @DisplayName("TERMINATE: REAG_EOT → TERMINATE")
    void testTerminate() {
        navigateToReagEot();

        assertTrue(stateMachine.sendEvent(DmlEvent.SEND_TERMINATE));
        assertEquals(DmlState.TERMINATE, stateMachine.getState().getId());
    }

    @Test
    @DisplayName("System status flow: REAG_EOT → SYSTEM_STATUS → SYSTEM_STATUS_WAITING_ACK")
    void testSystemStatusFlow() {
        navigateToReagEot();

        assertTrue(stateMachine.sendEvent(DmlEvent.SYSTEM_STATUS_RECEIVED));
        assertEquals(DmlState.SYSTEM_STATUS_RCV, stateMachine.getState().getId());

        assertTrue(stateMachine.sendEvent(DmlEvent.ACK_RECEIVED));
        assertEquals(DmlState.SYSTEM_STATUS_WAITING_ACK, stateMachine.getState().getId());
    }

    private void navigateToReagEot() {
        stateMachine.sendEvent(DmlEvent.HEL_RECEIVED);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.DST_RECEIVED);
        stateMachine.sendEvent(DmlEvent.OBS_EOT_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_EVS_REQUEST);
        stateMachine.sendEvent(DmlEvent.EVS_EOT_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_SET_TIME);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_SETUP);
        stateMachine.sendEvent(DmlEvent.SETUP_SENT_COMPLETE);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_WIFI_SETUP);
        stateMachine.sendEvent(DmlEvent.WIFI_SETUP_SENT_COMPLETE);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_WIFI_CERT);
        stateMachine.sendEvent(DmlEvent.WIFI_CERT_SENT_COMPLETE);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_LOCATION);
        stateMachine.sendEvent(DmlEvent.LOC_SENT_COMPLETE);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_OPERATOR);
        stateMachine.sendEvent(DmlEvent.OPL_SENT_COMPLETE);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_PATIENT);
        stateMachine.sendEvent(DmlEvent.PTL_SENT_COMPLETE);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_PHYSICIAN);
        stateMachine.sendEvent(DmlEvent.PHYS_SENT_COMPLETE);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_FIRMWARE);
        stateMachine.sendEvent(DmlEvent.FIRM_SENT_COMPLETE);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        stateMachine.sendEvent(DmlEvent.SEND_REAGENT);
        stateMachine.sendEvent(DmlEvent.REAG_SENT_COMPLETE);
        stateMachine.sendEvent(DmlEvent.ACK_RECEIVED);
        assertEquals(DmlState.REAG_EOT, stateMachine.getState().getId());
    }
}