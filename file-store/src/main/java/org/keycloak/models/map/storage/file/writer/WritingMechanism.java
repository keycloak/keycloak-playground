package org.keycloak.models.map.storage.file.writer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.events.DocumentEndEvent;
import org.snakeyaml.engine.v2.events.DocumentStartEvent;
import org.snakeyaml.engine.v2.events.Event;
import org.snakeyaml.engine.v2.events.ImplicitTuple;
import org.snakeyaml.engine.v2.events.MappingEndEvent;
import org.snakeyaml.engine.v2.events.MappingStartEvent;
import org.snakeyaml.engine.v2.events.ScalarEvent;
import org.snakeyaml.engine.v2.events.SequenceEndEvent;
import org.snakeyaml.engine.v2.events.SequenceStartEvent;
import org.snakeyaml.engine.v2.events.StreamEndEvent;
import org.snakeyaml.engine.v2.events.StreamStartEvent;

/**
 * Mechanism which produces list of {@link Event}s. 
 * 
 * @author vramik
 */
public class WritingMechanism {

    private final List<Event> events = new LinkedList<>();
    private final ImplicitTuple implicitTuple = new ImplicitTuple(true, true);

    public void startMapping() {
        events.add(new MappingStartEvent(Optional.empty(), Optional.of("!!map"), true, FlowStyle.BLOCK));
    }

    public void endMapping() {
        events.add(new MappingEndEvent());
    }

    public void startSequence() {
        events.add(new SequenceStartEvent(Optional.empty(), Optional.of("!!seq"), true, FlowStyle.BLOCK));
    }

    public void endSequence() {
        events.add(new SequenceEndEvent());
    }

    public void addScalar(Object value) {
        events.add(new ScalarEvent(Optional.empty(), determineTag(value), implicitTuple, value == null ? "null" : value.toString(), determineStyle(value)));
    }

    public void startStream() {
        events.add(new StreamStartEvent());
    }

    public void endStream() {
        events.add(new StreamEndEvent());
    }

    public void startDocument() {
        events.add(new DocumentStartEvent(false, Optional.empty(), Collections.EMPTY_MAP));
    }

    public void endDocument() {
        events.add(new DocumentEndEvent(false));
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    private Optional<String> determineTag(Object value) {
        if (value instanceof String) {
            return Optional.of("!!str");
        } else if (value instanceof Boolean) {
            return Optional.of("!!bool");
        } else if (value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
            return Optional.of("!!int");
        } else if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            return Optional.of("!!float");
        } else if (value == null) {
            return Optional.of("!!null");
        } else {
            return Optional.empty();
        }
    }

    private ScalarStyle determineStyle(Object value) {
        if (value != null && value instanceof String && ((String) value).contains("\n")) {
            return ScalarStyle.FOLDED;
        }
        return ScalarStyle.PLAIN;
    }
}
