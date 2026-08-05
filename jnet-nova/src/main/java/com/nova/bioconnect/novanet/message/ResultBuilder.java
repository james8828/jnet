package com.nova.bioconnect.novanet.message;

import com.nova.bioconnect.novanet.hl7.Hl7Constants;
import com.nova.bioconnect.novanet.hl7.Hl7Encoder;
import com.nova.bioconnect.novanet.hl7.Hl7Message;
import com.nova.bioconnect.novanet.hl7.Hl7Segment;
import com.nova.bioconnect.novanet.model.Note;
import com.nova.bioconnect.novanet.model.ObservationResult;
import com.nova.bioconnect.novanet.model.PatientInfo;
import com.nova.bioconnect.novanet.model.QcInfo;
import com.nova.bioconnect.novanet.model.ResultMessage;
import com.nova.bioconnect.novanet.model.VisitInfo;
import com.nova.bioconnect.novanet.util.Hl7DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds unsolicited observation result messages (ORU^R01 for patient results, OUL^R21 for QC
 * results) and extracts a {@link ResultMessage} from a parsed message, per the Bio-Connect
 * interface.
 *
 * <p>Result message structure:
 * <pre>
 *   MSH
 *   [PID]            (patient results only)
 *   [PV1]            (optional)
 *   [SAC] [SID]      (QC results only)
 *   ORC
 *   OBR
 *   {[NTE]}          (order-level notes)
 *   {OBX {[NTE]}}    (observation results)
 * </pre>
 */
public final class ResultBuilder {

    private ResultBuilder() {}

    /** Build an outbound result message. */
    public static Hl7Message build(ResultMessage r, String sendingApp, String sendingFacility,
                                   String controlId, String processingId, String version) {
        String messageType = r.isQc() ? Hl7Constants.MSG_OUL_R21 : Hl7Constants.MSG_ORU_R01;
        Hl7Segment msh = Hl7Segment.msh(Hl7Constants.ENCODING_CHARS,
                sendingApp, sendingFacility, "", "",
                Hl7DateUtils.nowDateTime(), "",
                messageType, controlId, processingId, version);
        Hl7Message msg = new Hl7Message().addSegment(msh);

        if (!r.isQc() && r.patient() != null) {
            msg.addSegment(AdtBuilder.buildPid(r.patient()));
        }
        if (r.visit() != null) {
            msg.addSegment(AdtBuilder.buildPv1(r.visit()));
        }
        if (r.isQc() && r.qcInfo() != null) {
            msg.addSegment(buildSac(r.qcInfo()));
            msg.addSegment(buildSid(r.qcInfo()));
        }
        msg.addSegment(buildOrc(r));
        msg.addSegment(buildObr(r));
        if (r.notes() != null) {
            for (Note n : r.notes()) {
                msg.addSegment(buildNte(n));
            }
        }
        if (r.results() != null) {
            for (ObservationResult o : r.results()) {
                msg.addSegment(buildObx(o));
            }
        }
        return msg;
    }

    /** Build an ORC segment (common order). */
    public static Hl7Segment buildOrc(ResultMessage r) {
        Hl7Segment orc = Hl7Segment.of(Hl7Constants.ORC);
        orc.setField(1, Hl7Constants.ORDER_CONTROL_NEW);             // ORC-1 always 'NW'
        orc.setField(2, Hl7Constants.PLACER_ORDER_HIS);              // ORC-2 always '^HIS'
        orc.setField(3, esc(r.fillerOrderNumber()));                 // ORC-3 filler order #
        orc.setField(4, "");                                         // ORC-4 placer group (NU)
        orc.setField(5, Hl7Constants.ORDER_STATUS_COMPLETE);         // ORC-5 always 'CM'
        orc.setField(6, "");                                         // ORC-6 response flag (NU)
        orc.setField(7, "");                                         // ORC-7 quantity/timing (NU)
        orc.setField(8, "");                                         // ORC-8 parent (NU)
        orc.setField(9, Hl7DateUtils.formatDateTime(r.transactionDateTime())); // ORC-9
        return orc;
    }

    /** Build an OBR segment (observation request). */
    public static Hl7Segment buildObr(ResultMessage r) {
        Hl7Segment obr = Hl7Segment.of(Hl7Constants.OBR);
        obr.setField(1, nz(r.obrSetId(), "1"));                                  // OBR-1 set id (always 1)
        obr.setField(2, esc(r.placerOrder()));                                   // OBR-2 placer order (sample/accession id)
        obr.setField(3, "");                                                     // OBR-3 filler order (NU)
        String svc = nz(r.serviceIdentifier(), Hl7Constants.DEFAULT_SERVICE_IDENTIFIER);
        obr.setField(4, esc(svc));                                               // OBR-4 profile name (default WGLUC)
        obr.setField(5, "");                                                     // OBR-5 priority (NU)
        obr.setField(6, "");                                                     // OBR-6 requested date-time (O)
        obr.setField(7, Hl7DateUtils.formatDateTime(r.observationDateTime()));   // OBR-7 observation date/time (R2)
        obr.setField(8, Hl7DateUtils.formatDateTime(r.observationEndDateTime()));// OBR-8 observation end date/time (O)
        // OBR-9 .. OBR-13 not in use
        obr.setField(14, Hl7DateUtils.formatDateTime(r.specimenReceivedDateTime())); // OBR-14 (R2)
        obr.setField(15, esc(r.specimenSource()));                               // OBR-15 specimen source (O)
        obr.setField(16, esc(r.orderingProvider()));                             // OBR-16 ordering provider (NU)
        // OBR-17 .. OBR-24 not in use
        obr.setField(25, nz(r.resultStatus(), Hl7Constants.RESULT_STATUS_FINAL));// OBR-25 result status (always 'F')
        return obr;
    }

    /** Build an OBX segment (observation result). */
    public static Hl7Segment buildObx(ObservationResult o) {
        Hl7Segment obx = Hl7Segment.of(Hl7Constants.OBX);
        obx.setField(1, esc(o.sequenceNumber()));                  // OBX-1 sequence number
        obx.setField(2, nz(o.valueType(), Hl7Constants.VALUE_TYPE_NM)); // OBX-2 value type (NM/ST)
        obx.setField(3, comp(o.testCode(), o.testName()));         // OBX-3 test code^test name (R2)
        obx.setField(4, "");                                       // OBX-4 observation sub-id (NU)
        obx.setField(5, esc(o.resultValue()));                     // OBX-5 result value (R2)
        obx.setField(6, esc(o.units()));                           // OBX-6 units (O)
        obx.setField(7, esc(o.referenceRange()));                  // OBX-7 reference range (O)
        obx.setField(8, esc(o.abnormalFlags()));                   // OBX-8 abnormal flags (O)
        obx.setField(9, "");                                       // OBX-9 probability (NU)
        obx.setField(10, "");                                      // OBX-10 nature of abnormal (NU)
        obx.setField(11, esc(o.resultStatus()));                   // OBX-11 result status (F/P/V/W)
        obx.setField(12, "");                                      // OBX-12 date last obs normal (NU)
        obx.setField(13, "");                                      // OBX-13 user defined access (NU)
        obx.setField(14, "");                                      // OBX-14 datetime of observation (NU)
        obx.setField(15, "");                                      // OBX-15 producer id (NU)
        obx.setField(16, esc(o.responsibleObserver()));            // OBX-16 responsible observer (O)
        return obx;
    }

    /** Build an NTE segment (note/comment). */
    public static Hl7Segment buildNte(Note n) {
        return Hl7Segment.of(Hl7Constants.NTE,
                esc(n.sequenceNumber()),                                            // NTE-1
                nz(n.commentSource(), Hl7Constants.NTE_SOURCE_INSTRUMENT),          // NTE-2
                esc(n.comment()),                                                   // NTE-3
                nz(n.commentType(), Hl7Constants.NTE_COMMENT_TYPE)                  // NTE-4 (always 'G')
        );
    }

    /** Build a SAC segment (specimen container, QC only). */
    public static Hl7Segment buildSac(QcInfo q) {
        Hl7Segment sac = Hl7Segment.of(Hl7Constants.SAC);
        sac.setField(1, "");                          // SAC-1 set id
        sac.setField(2, "");                          // SAC-2
        sac.setField(3, esc(q.containerIdentifier()));// SAC-3 container identifier
        sac.setField(4, "");                          // SAC-4
        sac.setField(5, "");                          // SAC-5
        sac.setField(6, esc(q.specimenSource()));     // SAC-6 specimen source
        return sac;
    }

    /** Build a SID segment (substance identifier, QC only). */
    public static Hl7Segment buildSid(QcInfo q) {
        Hl7Segment sid = Hl7Segment.of(Hl7Constants.SID);
        sid.setField(1, "");                              // SID-1 set id
        sid.setField(2, esc(q.lotNumber()));              // SID-2 lot number
        sid.setField(3, esc(q.lotLevel()));               // SID-3 lot level
        sid.setField(4, esc(q.manufacturerIdentifier())); // SID-4 manufacturer identifier
        return sid;
    }

    // ---- extraction (inbound parsing) ----

    /** Extract a {@link ResultMessage} from a parsed ORU^R01 / OUL^R21 message. */
    public static ResultMessage extract(Hl7Message msg) {
        boolean qc = Hl7Constants.MSG_OUL_R21.equals(msg.getMessageType())
                || msg.getSegment(Hl7Constants.SAC).isPresent();
        PatientInfo patient = AdtBuilder.extractPatient(msg);
        VisitInfo visit = AdtBuilder.extractVisit(msg);
        QcInfo qcInfo = extractQc(msg);

        Optional<Hl7Segment> orcOpt = msg.getSegment(Hl7Constants.ORC);
        String fillerOrderNumber = orcOpt.map(s -> unesc(s.getField(3))).orElse("");
        String transactionDateTimeRaw = orcOpt.map(s -> s.getField(9)).orElse("");

        Optional<Hl7Segment> obrOpt = msg.getSegment(Hl7Constants.OBR);
        Hl7Segment obr = obrOpt.orElse(null);
        String obrSetId = obr == null ? "1" : unesc(obr.getField(1));
        String placerOrder = obr == null ? "" : unesc(obr.getField(2));
        String serviceIdentifier = obr == null ? "" : unesc(obr.getField(4));
        String specimenSource = obr == null ? "" : unesc(obr.getField(15));
        String orderingProvider = obr == null ? "" : unesc(obr.getField(16));
        String resultStatus = obr == null ? Hl7Constants.RESULT_STATUS_FINAL : nz(unesc(obr.getField(25)), Hl7Constants.RESULT_STATUS_FINAL);

        List<Note> notes = new ArrayList<>();
        for (Hl7Segment nte : msg.getSegments(Hl7Constants.NTE)) {
            notes.add(new Note(
                    unesc(nte.getField(1)),
                    unesc(nte.getField(2)),
                    unesc(nte.getField(3)),
                    unesc(nte.getField(4))));
        }

        List<ObservationResult> results = new ArrayList<>();
        for (Hl7Segment obx : msg.getSegments(Hl7Constants.OBX)) {
            results.add(new ObservationResult(
                    unesc(obx.getField(1)),
                    unesc(obx.getField(2)),
                    unesc(obx.getComponent(3, 1)),
                    unesc(obx.getComponent(3, 2)),
                    unesc(obx.getField(5)),
                    unesc(obx.getField(6)),
                    unesc(obx.getField(7)),
                    unesc(obx.getField(8)),
                    unesc(obx.getField(11)),
                    unesc(obx.getField(16))));
        }

        return new ResultMessage(
                qc,
                patient,
                visit,
                qcInfo,
                fillerOrderNumber,
                Hl7DateUtils.parseDateTime(transactionDateTimeRaw),
                obrSetId,
                placerOrder,
                serviceIdentifier,
                obr == null ? null : Hl7DateUtils.parseDateTime(obr.getField(7)),
                obr == null ? null : Hl7DateUtils.parseDateTime(obr.getField(8)),
                obr == null ? null : Hl7DateUtils.parseDateTime(obr.getField(14)),
                specimenSource,
                orderingProvider,
                resultStatus,
                notes,
                results
        );
    }

    /** Extract QC info from SAC + SID segments; null if neither present. */
    public static QcInfo extractQc(Hl7Message msg) {
        Optional<Hl7Segment> sac = msg.getSegment(Hl7Constants.SAC);
        Optional<Hl7Segment> sid = msg.getSegment(Hl7Constants.SID);
        if (sac.isEmpty() && sid.isEmpty()) {
            return null;
        }
        return new QcInfo(
                sac.map(s -> unesc(s.getField(3))).orElse(""),
                sac.map(s -> unesc(s.getField(6))).orElse(""),
                sid.map(s -> unesc(s.getField(2))).orElse(""),
                sid.map(s -> unesc(s.getField(3))).orElse(""),
                sid.map(s -> unesc(s.getField(4))).orElse("")
        );
    }

    // ---- helpers ----

    private static String esc(String v) { return Hl7Encoder.escape(v); }
    private static String unesc(String v) { return Hl7Encoder.unescape(v); }

    private static String nz(String v, String def) {
        return (v == null || v.isEmpty()) ? def : v;
    }

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
