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
import org.snakeyaml.engine.v2.nodes.Tag;

/**
 * Mechanism which produces list of {@link Event}s. 
 * 
 * @author vramik
 */
public class WritingMechanismImpl implements WritingMechanism {

    private final List<Event> events = new LinkedList<>();
    private final ImplicitTuple implicitTuple = new ImplicitTuple(true, true);

    @Override
    public void startMapping() {
        events.add(new MappingStartEvent(Optional.empty(), Optional.of(Tag.MAP.getValue()), true, FlowStyle.BLOCK));
    }

    @Override
    public void endMapping() {
        events.add(new MappingEndEvent());
    }

    @Override
    public void startSequence() {
        events.add(new SequenceStartEvent(Optional.empty(), Optional.of(Tag.SEQ.getValue()), true, FlowStyle.BLOCK));
    }

    @Override
    public void endSequence() {
        events.add(new SequenceEndEvent());
    }

    @Override
    public void addScalar(Object value) {
        events.add(new ScalarEvent(Optional.empty(), determineTag(value), implicitTuple, value == null ? "null" : value.toString(), determineStyle(value)));
    }

    @Override
    public void startStream() {
        events.add(new StreamStartEvent());
    }

    @Override
    public void endStream() {
        events.add(new StreamEndEvent());
    }

    @Override
    public void startDocument() {
        events.add(new DocumentStartEvent(false, Optional.empty(), Collections.EMPTY_MAP));
    }

    @Override
    public void endDocument() {
        events.add(new DocumentEndEvent(false));
    }

    @Override
    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    private Optional<String> determineTag(Object value) {
        if (value instanceof String) {
            return Optional.of(Tag.STR.getValue());
        } else if (value instanceof Boolean) {
            return Optional.of(Tag.BOOL.getValue());
        } else if (value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
            return Optional.of(Tag.INT.getValue());
        } else if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            return Optional.of(Tag.FLOAT.getValue());
        } else if (value == null) {
            return Optional.of(Tag.NULL.getValue());
        } else {
            return Optional.empty();
        }
    }

    private ScalarStyle determineStyle(Object value) {
        if (value instanceof String && ((String) value).contains("\n")) {
            return ScalarStyle.FOLDED;
        }
        return ScalarStyle.PLAIN;
    }
}
