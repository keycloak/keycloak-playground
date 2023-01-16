package org.keycloak.models.map.storage.file.writer;

import org.snakeyaml.engine.v2.events.Event;

/**
 * Mechanism which produces list of {@link Event}s. 
 * 
 * @author vramik
 */
public interface WritingMechanism {

    WritingMechanism writeObject(Object value);

    WritingMechanism writeSequence(Runnable task);

    WritingMechanism writeMapping(Runnable task);
    WritingMechanism writePair(String key, Runnable task);


}
