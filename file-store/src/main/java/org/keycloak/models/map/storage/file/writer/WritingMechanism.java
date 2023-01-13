package org.keycloak.models.map.storage.file.writer;

import java.util.List;
import org.snakeyaml.engine.v2.events.Event;

/**
 * Mechanism which produces list of {@link Event}s. 
 * 
 * @author vramik
 */
public interface WritingMechanism {

    void addScalar(Object value);

    void endDocument();

    void endMapping();

    void endSequence();

    void endStream();

    List<Event> getEvents();

    void startDocument();

    void startMapping();

    void startSequence();

    void startStream();

}
