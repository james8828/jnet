package com.nova.bioconnect.novanet.hl7;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An HL7 v2.4 message: an ordered list of segments. The first segment is always {@link Hl7Constants#MSH}.
 *
 * <p>Segments can be looked up by name. When multiple segments share a name (e.g. multiple
 * {@code OBX} or {@code NTE} segments), {@link #getSegments(String)} returns all of them in
 * document order and {@link #getSegment(String)} returns the first.
 */
public class Hl7Message {

    private final List<Hl7Segment> segments = new ArrayList<>();

    public Hl7Message() {}

    public Hl7Message(List<Hl7Segment> segments) {
        if (segments != null) {
            this.segments.addAll(segments);
        }
    }

    public List<Hl7Segment> getSegments() { return segments; }

    public Hl7Message addSegment(Hl7Segment segment) {
        segments.add(segment);
        return this;
    }

    /** First segment with the given name, or empty Optional. */
    public Optional<Hl7Segment> getSegment(String name) {
        for (Hl7Segment s : segments) {
            if (name.equals(s.getName())) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /** All segments with the given name, in document order. */
    public List<Hl7Segment> getSegments(String name) {
        List<Hl7Segment> out = new ArrayList<>();
        for (Hl7Segment s : segments) {
            if (name.equals(s.getName())) {
                out.add(s);
            }
        }
        return out;
    }

    /** All segments grouped by name, in encounter order. */
    public Map<String, List<Hl7Segment>> groupByName() {
        Map<String, List<Hl7Segment>> map = new LinkedHashMap<>();
        for (Hl7Segment s : segments) {
            map.computeIfAbsent(s.getName(), k -> new ArrayList<>()).add(s);
        }
        return map;
    }

    /** The MSH segment (must be present for a valid message). */
    public Hl7Segment getMsh() {
        return segments.isEmpty() ? null : segments.get(0);
    }

    /** MSH-9 message type, e.g. {@code ADT^A08} or {@code ORU^R01}. */
    public String getMessageType() {
        Hl7Segment msh = getMsh();
        return msh == null ? "" : msh.getField(9);
    }

    /** The trigger event component of MSH-9, e.g. {@code A08} for {@code ADT^A08}. */
    public String getTriggerEvent() {
        Hl7Segment msh = getMsh();
        if (msh == null) {
            return "";
        }
        return msh.getComponent(9, 2);
    }

    /** The base message type component of MSH-9, e.g. {@code ADT} for {@code ADT^A08}. */
    public String getMessageTypeCode() {
        Hl7Segment msh = getMsh();
        if (msh == null) {
            return "";
        }
        return msh.getComponent(9, 1);
    }

    /** MSH-10 message control id. */
    public String getMessageControlId() {
        Hl7Segment msh = getMsh();
        return msh == null ? "" : msh.getField(10);
    }

    /** Encoding characters (MSH-2), defaults to {@code ^~\&}. */
    public String getEncodingChars() {
        Hl7Segment msh = getMsh();
        if (msh == null) {
            return Hl7Constants.ENCODING_CHARS;
        }
        String enc = msh.getField(2);
        return enc.isEmpty() ? Hl7Constants.ENCODING_CHARS : enc;
    }

    /** Encode the message to its wire form: segments joined by {@code \r}, no trailing terminator. */
    public String encode() {
        return Hl7Encoder.encode(this);
    }

    @Override
    public String toString() {
        return encode().replace(Hl7Constants.SEGMENT_TERMINATOR, "\n");
    }
}
