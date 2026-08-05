package com.nova.bioconnect.novanet.hl7;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A single HL7 segment, e.g. {@code PID|1||N12345||MAN^FIRSTNAME|...}.
 *
 * <p>Fields are accessed by their 1-based HL7 field number (e.g. {@code getField(3)} returns
 * PID-3). Components within a field are accessed by 1-based component number using the
 * component separator {@code ^}; subcomponents use {@code &}.
 *
 * <p>The {@code MSH} segment is handled specially: MSH-1 is the field separator {@code |}
 * and MSH-2 is the encoding characters {@code ^~\&}. All other segments are parsed by
 * splitting on the field separator and dropping the leading segment name.
 */
public class Hl7Segment {

    private final String name;
    private final boolean msh;
    private final List<String> fields; // index 0 == field 1

    private Hl7Segment(String name, boolean msh, List<String> fields) {
        this.name = name;
        this.msh = msh;
        this.fields = fields;
    }

    /** Parse a raw segment line (without the segment terminator). */
    public static Hl7Segment parse(String line) {
        if (line == null || line.isEmpty()) {
            return new Hl7Segment("", false, new ArrayList<>());
        }
        String trimmed = line.replaceAll("[\\r\\n]", "");
        if (trimmed.isEmpty()) {
            return new Hl7Segment("", false, new ArrayList<>());
        }
        String sep = Hl7Constants.FIELD_SEPARATOR;
        String[] parts = splitPreserveAll(trimmed, sep.charAt(0));
        String segName = parts[0];
        if (Hl7Constants.MSH.equals(segName)) {
            // MSH-1 = "|", MSH-2 = encoding chars (parts[1]), MSH-3 = parts[2], ...
            List<String> f = new ArrayList<>();
            f.add(sep); // field 1 = field separator
            for (int i = 1; i < parts.length; i++) {
                f.add(parts[i]);
            }
            return new Hl7Segment(segName, true, f);
        }
        List<String> f = new ArrayList<>(parts.length - 1);
        for (int i = 1; i < parts.length; i++) {
            f.add(parts[i]);
        }
        return new Hl7Segment(segName, false, f);
    }

    /** Create an empty segment of the given name (non-MSH). */
    public static Hl7Segment of(String name) {
        return new Hl7Segment(name, false, new ArrayList<>());
    }

    /** Create a non-MSH segment from explicit field values (field1, field2, ...). */
    public static Hl7Segment of(String name, String... fieldValues) {
        return new Hl7Segment(name, false, new ArrayList<>(Arrays.asList(fieldValues)));
    }

    /** Create an MSH segment with the given encoding characters and fields (field3 onward). */
    public static Hl7Segment msh(String encodingChars, String... fieldsFrom3) {
        List<String> f = new ArrayList<>();
        f.add(Hl7Constants.FIELD_SEPARATOR); // MSH-1
        f.add(encodingChars);                // MSH-2
        Collections.addAll(f, fieldsFrom3);  // MSH-3..
        return new Hl7Segment(Hl7Constants.MSH, true, f);
    }

    public String getName() { return name; }
    public boolean isMsh() { return msh; }

    /** Get field value by 1-based field number; empty string when absent. */
    public String getField(int fieldNum) {
        int idx = fieldNum - 1;
        if (idx < 0 || idx >= fields.size()) {
            return "";
        }
        return fields.get(idx);
    }

    /** Get component (1-based) of a field (1-based). */
    public String getComponent(int fieldNum, int componentNum) {
        return component(getField(fieldNum), componentNum);
    }

    /** Get subcomponent (1-based) of a component (1-based) of a field (1-based). */
    public String getSubcomponent(int fieldNum, int componentNum, int subcomponentNum) {
        return subcomponent(getComponent(fieldNum, componentNum), subcomponentNum);
    }

    /** Set a field value (1-based); grows the list as needed. */
    public Hl7Segment setField(int fieldNum, String value) {
        int idx = fieldNum - 1;
        while (fields.size() <= idx) {
            fields.add("");
        }
        fields.set(idx, value == null ? "" : value);
        return this;
    }

    /** Set a single component (1-based) within a field (1-based), preserving other components. */
    public Hl7Segment setComponent(int fieldNum, int componentNum, String value) {
        String existing = getField(fieldNum);
        String[] comps = splitPreserveAll(existing, Hl7Constants.COMPONENT_SEP);
        while (comps.length < componentNum) {
            comps = Arrays.copyOf(comps, componentNum);
            comps[comps.length - 1] = "";
        }
        comps[componentNum - 1] = value == null ? "" : value;
        return setField(fieldNum, String.join(String.valueOf(Hl7Constants.COMPONENT_SEP), comps));
    }

    public int fieldCount() { return fields.size(); }

    /** Encode this segment back to its wire form (without segment terminator). */
    public String encode() {
        String sep = Hl7Constants.FIELD_SEPARATOR;
        if (msh) {
            // "MSH" + field1(|) + field2(enc) + "|" + join(fields[2..], "|")
            int last = lastNonEmptyFrom(3);
            StringBuilder sb = new StringBuilder(name);
            sb.append(getField(1));                 // "|"
            sb.append(getField(2));                 // encoding chars
            for (int i = 3; i <= last; i++) {
                sb.append(sep).append(getField(i));
            }
            return sb.toString();
        }
        int last = lastNonEmptyFrom(1);
        if (last == 0) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name);
        for (int i = 1; i <= last; i++) {
            sb.append(sep).append(getField(i));
        }
        return sb.toString();
    }

    /** Index of the last non-empty field at or after {@code from} (1-based); 0 if none. */
    private int lastNonEmptyFrom(int from) {
        int last = 0;
        for (int i = from; i <= fields.size(); i++) {
            String v = fields.get(i - 1);
            if (v != null && !v.isEmpty()) {
                last = i;
            }
        }
        return last;
    }

    @Override
    public String toString() { return encode(); }

    // ---- helpers ----

    static String component(String fieldValue, int componentNum) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return "";
        }
        String[] comps = splitPreserveAll(fieldValue, Hl7Constants.COMPONENT_SEP);
        if (componentNum < 1 || componentNum > comps.length) {
            return "";
        }
        return comps[componentNum - 1];
    }

    static String subcomponent(String componentValue, int subcomponentNum) {
        if (componentValue == null || componentValue.isEmpty()) {
            return "";
        }
        String[] subs = splitPreserveAll(componentValue, Hl7Constants.SUBCOMPONENT_SEP);
        if (subcomponentNum < 1 || subcomponentNum > subs.length) {
            return "";
        }
        return subs[subcomponentNum - 1];
    }

    /** Split preserving trailing empty fields (Java's String.split drops them by default). */
    static String[] splitPreserveAll(String value, char sep) {
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == sep) {
                out.add(value.substring(start, i));
                start = i + 1;
            }
        }
        out.add(value.substring(start));
        return out.toArray(new String[0]);
    }
}
