package org.keycloak.models.map.storage.file.writer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.events.Event;
import org.snakeyaml.engine.v2.events.ImplicitTuple;
import org.snakeyaml.engine.v2.events.MappingEndEvent;
import org.snakeyaml.engine.v2.events.MappingStartEvent;
import org.snakeyaml.engine.v2.events.ScalarEvent;
import org.snakeyaml.engine.v2.events.SequenceEndEvent;
import org.snakeyaml.engine.v2.events.SequenceStartEvent;

/**
 *
 * @author vramik
 */
public class WritingMechanism {

    private final List<Event> events;
    private final ImplicitTuple implicitTuple = new ImplicitTuple(true, true);

    public WritingMechanism(List<Event> events) {
        this.events = events;
    }

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

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    private Optional<String> determineTag(Object value) {
        if (value instanceof String) {
            return Optional.of("!!str");
        } else if (value instanceof Boolean) {
            return Optional.of("!!bool");
        } else if (value instanceof Integer) {
            return Optional.of("!!int");
        } else if (value instanceof Float) {
            return Optional.of("!!float");
        } else if (value == null) {
            return Optional.of("!!null");
        } else {
            return Optional.empty();
        }
    }

    //todo
    private ScalarStyle determineStyle(Object value) {
        if (value != null && value.toString().contains("\n")) {
            return ScalarStyle.FOLDED;
        }
        return ScalarStyle.PLAIN;
    }
}
