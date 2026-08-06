package com.nova.bioconnect.rtm.message;

import com.nova.bioconnect.rtm.hl7.Hl7Constants;
import com.nova.bioconnect.rtm.hl7.Hl7Encoder;
import com.nova.bioconnect.rtm.hl7.Hl7Message;
import com.nova.bioconnect.rtm.hl7.Hl7Segment;
import com.nova.bioconnect.rtm.model.MergeInfo;
import com.nova.bioconnect.rtm.model.PatientInfo;
import com.nova.bioconnect.rtm.model.VisitInfo;
import com.nova.bioconnect.rtm.util.Hl7DateUtils;

import java.util.Optional;

/**
 * Builds ADT messages (MSH + EVN + PID + PV1 [+ MRG]) and extracts patient/visit/merge data
 * from parsed ADT messages, per the Bio-Connect HL7 interface.
 *
 * <p>Supported trigger events include A01 (admit), A02 (transfer), A03 (discharge), A04 (register),
 * A05 (pre-admit), A06/A07 (change/merge), A08 (update), A11 (cancel admit), A12 (cancel transfer),
 * A13 (cancel discharge), A17 (swap), etc. Trigger events in
 * {@link Hl7Constants#IGNORED_ADT_TRIGGERS} are ignored by the interface.
 */
public final class AdtBuilder {

    private AdtBuilder() {}

    /** Build an outbound ADT message. */
    public static Hl7Message build(String triggerEvent, PatientInfo patient, VisitInfo visit, MergeInfo merge,
                                   String sendingApp, String sendingFacility,
                                   String controlId, String processingId, String version) {
        Hl7Segment msh = Hl7Segment.msh(Hl7Constants.ENCODING_CHARS,
                sendingApp,                 // MSH-3
                sendingFacility,            // MSH-4
                "",                         // MSH-5 receiving application
                "",                         // MSH-6 receiving facility
                Hl7DateUtils.nowDateTime(), // MSH-7
                "",                         // MSH-8 security
                Hl7Constants.MSG_ADT + "^" + triggerEvent, // MSH-9
                controlId,                  // MSH-10
                processingId,               // MSH-11
                version                     // MSH-12
        );
        Hl7Message msg = new Hl7Message().addSegment(msh);
        msg.addSegment(buildEvn(triggerEvent));
        if (patient != null) {
            msg.addSegment(buildPid(patient));
        }
        if (visit != null) {
            msg.addSegment(buildPv1(visit));
        }
        if (merge != null) {
            msg.addSegment(buildMrg(merge));
        }
        return msg;
    }

    /** Build an EVN segment. */
    public static Hl7Segment buildEvn(String triggerEvent) {
        return Hl7Segment.of(Hl7Constants.EVN,
                triggerEvent,               // EVN-1 event type code
                Hl7DateUtils.nowDateTime()  // EVN-2 date/time of event
        );
    }

    /** Build a PID segment from patient info. */
    public static Hl7Segment buildPid(PatientInfo p) {
        Hl7Segment pid = Hl7Segment.of(Hl7Constants.PID);
        pid.setField(1, "1");                                            // SET ID
        pid.setField(2, esc(p.externalPatientId()));                     // PID-2 external id
        pid.setField(3, esc(p.internalPatientId()));                     // PID-3 internal id (MRN)
        pid.setField(4, "");                                             // PID-4 alternate id (NU)
        pid.setField(5, comp(p.lastName(), p.firstName(), p.middleName(), p.prefix(), p.suffix())); // PID-5
        pid.setField(6, "");                                             // PID-6 mother's maiden (NU)
        pid.setField(7, Hl7DateUtils.formatDate(p.dateOfBirth()));       // PID-7 DOB
        pid.setField(8, esc(p.gender()));                                // PID-8 sex
        pid.setField(9, esc(p.race()));                                  // PID-9 race
        pid.setField(10, "");                                            // PID-10 ethnic group (NU)
        pid.setField(11, esc(p.address()));                              // PID-11 address
        pid.setField(12, "");                                            // PID-12 county code (NU)
        pid.setField(13, esc(p.phoneHome()));                            // PID-13 phone home
        pid.setField(14, "");                                            // PID-14 phone business (NU)
        pid.setField(15, "");                                            // PID-15 language (NU)
        pid.setField(16, "");                                            // PID-16 marital status (O)
        pid.setField(17, "");                                            // PID-17 religion (O)
        pid.setField(18, esc(p.accountNumber()));                        // PID-18 account number
        return pid;
    }

    /** Build a PV1 segment from visit info. */
    public static Hl7Segment buildPv1(VisitInfo v) {
        Hl7Segment pv1 = Hl7Segment.of(Hl7Constants.PV1);
        pv1.setField(1, "1");                                            // SET ID
        pv1.setField(2, esc(v.patientClass()));                          // PV1-2 patient class
        pv1.setField(3, comp(v.assignedLocation(), v.room(), v.bed(), v.facility())); // PV1-3
        pv1.setField(4, "");                                             // PV1-4 admission type (O)
        pv1.setField(5, "");                                             // PV1-5 pre-admit number (O)
        pv1.setField(6, esc(v.priorPatientLocation()));                  // PV1-6 prior location
        pv1.setField(7, esc(v.attendingPhysician()));                    // PV1-7 attending doctor
        pv1.setField(8, "");                                             // PV1-8 referring doctor (O)
        pv1.setField(9, "");                                             // PV1-9 consulting doctor (O)
        pv1.setField(10, esc(v.hospitalService()));                      // PV1-10 hospital service
        // PV1-11 .. PV1-17 unused/optional
        pv1.setField(18, esc(v.patientType()));                          // PV1-18 patient type
        pv1.setField(19, esc(v.visitNumber()));                          // PV1-19 visit number
        // PV1-20 .. PV1-43 not in use
        pv1.setField(44, Hl7DateUtils.formatDateTime(v.admitDateTime()));      // PV1-44 admit date/time
        pv1.setField(45, Hl7DateUtils.formatDateTime(v.dischargeDateTime()));  // PV1-45 discharge date/time
        return pv1;
    }

    /** Build an MRG segment from merge info (ADT merge only). */
    public static Hl7Segment buildMrg(MergeInfo m) {
        Hl7Segment mrg = Hl7Segment.of(Hl7Constants.MRG);
        mrg.setField(1, esc(m.priorInternalPatientId()));  // MRG-1
        mrg.setField(2, "");                               // MRG-2 prior alternate id (O)
        mrg.setField(3, esc(m.priorAccountNumber()));      // MRG-3
        mrg.setField(4, esc(m.priorExternalPatientId()));  // MRG-4
        return mrg;
    }

    // ---- extraction (inbound parsing) ----

    /** Extract patient info from a parsed ADT message; null if no PID segment. */
    public static PatientInfo extractPatient(Hl7Message msg) {
        Optional<Hl7Segment> opt = msg.getSegment(Hl7Constants.PID);
        if (opt.isEmpty()) {
            return null;
        }
        Hl7Segment pid = opt.get();
        return new PatientInfo(
                unesc(pid.getField(2)),
                unesc(pid.getField(3)),
                unesc(pid.getComponent(5, 1)),
                unesc(pid.getComponent(5, 2)),
                unesc(pid.getComponent(5, 3)),
                unesc(pid.getComponent(5, 4)),
                unesc(pid.getComponent(5, 5)),
                Hl7DateUtils.parseDate(pid.getField(7)),
                unesc(pid.getField(8)),
                unesc(pid.getField(9)),
                unesc(pid.getField(11)),
                unesc(pid.getField(13)),
                unesc(pid.getField(18))
        );
    }

    /** Extract visit info from a parsed ADT message; null if no PV1 segment. */
    public static VisitInfo extractVisit(Hl7Message msg) {
        Optional<Hl7Segment> opt = msg.getSegment(Hl7Constants.PV1);
        if (opt.isEmpty()) {
            return null;
        }
        Hl7Segment pv1 = opt.get();
        return new VisitInfo(
                unesc(pv1.getField(2)),
                unesc(pv1.getComponent(3, 1)),
                unesc(pv1.getComponent(3, 2)),
                unesc(pv1.getComponent(3, 3)),
                unesc(pv1.getComponent(3, 4)),
                unesc(pv1.getField(6)),
                unesc(pv1.getField(7)),
                unesc(pv1.getField(10)),
                unesc(pv1.getField(18)),
                unesc(pv1.getField(19)),
                Hl7DateUtils.parseDateTime(pv1.getField(44)),
                Hl7DateUtils.parseDateTime(pv1.getField(45))
        );
    }

    /** Extract merge info from a parsed ADT message; null if no MRG segment. */
    public static MergeInfo extractMerge(Hl7Message msg) {
        Optional<Hl7Segment> opt = msg.getSegment(Hl7Constants.MRG);
        if (opt.isEmpty()) {
            return null;
        }
        Hl7Segment mrg = opt.get();
        return new MergeInfo(
                unesc(mrg.getField(1)),
                unesc(mrg.getField(3)),
                unesc(mrg.getField(4))
        );
    }

    // ---- helpers ----

    private static String esc(String v) { return Hl7Encoder.escape(v); }
    private static String unesc(String v) { return Hl7Encoder.unescape(v); }

    /** Escape and join components with {@code ^}. */
    private static String comp(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(Hl7Constants.COMPONENT_SEP);
            }
            sb.append(parts[i] == null ? "" : Hl7Encoder.escape(parts[i]));
        }
        return sb.toString();
    }
}
