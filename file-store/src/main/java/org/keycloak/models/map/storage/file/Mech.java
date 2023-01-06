package org.keycloak.models.map.storage.file;

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
public class Mech {

    private final List<Event> events;
    private final ImplicitTuple implicitTuple = new ImplicitTuple(true, true);

    public Mech(List<Event> events) {
        this.events = events;
    }
    
    public void startMapping() {
        events.add(new MappingStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK));
    }

    public void endMapping() {
        events.add(new MappingEndEvent());
    }

    public void startSequence() {
        events.add(new SequenceStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK));
    }

    public void endSequence() {
        events.add(new SequenceEndEvent());
    }

    public void addScalar(String value) {
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, value, ScalarStyle.PLAIN));
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }
}
