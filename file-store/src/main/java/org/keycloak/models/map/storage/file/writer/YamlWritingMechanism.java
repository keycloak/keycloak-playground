package org.keycloak.models.map.storage.file.writer;

import org.keycloak.models.map.storage.file.RunOnlyOnce;
import java.io.Closeable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Consumer;
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
 * Mechanism which produces {@link Event}s for SnakeYaml v2 {@code Emitter}.
 * 
 * @author vramik
 */
public class YamlWritingMechanism implements WritingMechanism, Closeable {

    private final ImplicitTuple implicitTuple = new ImplicitTuple(true, true);
    private final Consumer<Event> consumer;
    private boolean runningPreTasks = false;
    private final LinkedList<RunOnlyOnce> preTasks = new LinkedList<RunOnlyOnce>() {
        @Override
        public RunOnlyOnce removeLast() {
            final RunOnlyOnce res = super.removeLast();
            res.runPostTask();
            return res;
        }
    };

    public YamlWritingMechanism(Consumer<Event> consumer) {
        this.consumer = consumer;
        this.preTasks.add(new RunOnlyOnce(this::startDocument, this::endDocument));
    }

    @Override
    public void close() {
        endDocument();
    }

    private YamlWritingMechanism startMapping() {
        this.consumer.accept(new MappingStartEvent(Optional.empty(), Optional.of(Tag.MAP.getValue()), true, FlowStyle.BLOCK));
        return this;
    }

    private YamlWritingMechanism endMapping() {
        this.consumer.accept(new MappingEndEvent());
        return this;
    }

    private YamlWritingMechanism writeObject(Runnable taskWithOptionalWrite, Runnable preWriteTask, Runnable postWriteTask) {
        RunOnlyOnce roo = new RunOnlyOnce(preWriteTask, postWriteTask);
        try {
            preTasks.addLast(roo);
            taskWithOptionalWrite.run();
        } finally {
            preTasks.removeLast();
        }
        return this;
    }


    @Override
    public YamlWritingMechanism writeMapping(Runnable task) {
        return writeObject(task, this::startMapping, this::endMapping);
    }

    @Override
    public YamlWritingMechanism writeSequence(Runnable task) {
        return writeObject(task, this::startSequence, this::endSequence);
    }

    @Override
    public YamlWritingMechanism writePair(String key, Runnable task) {
        return writeObject(task, () -> writeObject(key), null);
    }

    private YamlWritingMechanism startSequence() {
        this.consumer.accept(new SequenceStartEvent(Optional.empty(), Optional.of(Tag.SEQ.getValue()), true, FlowStyle.BLOCK));
        return this;
    }

    private YamlWritingMechanism endSequence() {
        this.consumer.accept(new SequenceEndEvent());
        return this;
    }

    @Override
    public YamlWritingMechanism writeObject(Object value) {
        if (! runningPreTasks) {
            runningPreTasks = true;
            preTasks.forEach(RunOnlyOnce::run);
            runningPreTasks = false;
        }
        this.consumer.accept(new ScalarEvent(Optional.empty(), determineTag(value), implicitTuple, value == null ? "null" : value.toString(), determineStyle(value)));
        return this;
    }

    private YamlWritingMechanism startDocument() {
        this.consumer.accept(new StreamStartEvent());
        this.consumer.accept(new DocumentStartEvent(false, Optional.empty(), Collections.EMPTY_MAP));
        return this;
    }

    private YamlWritingMechanism endDocument() {
        this.consumer.accept(new DocumentEndEvent(false));
        this.consumer.accept(new StreamEndEvent());
        return this;
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
