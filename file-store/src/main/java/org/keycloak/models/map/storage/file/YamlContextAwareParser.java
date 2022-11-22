/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.map.storage.file;

import org.keycloak.models.map.storage.file.YamlContext.DefaultListContext;
import org.keycloak.models.map.storage.file.YamlContext.DefaultMapContext;
import org.keycloak.models.map.storage.file.YamlContext.DefaultObjectContext;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 *
 * @author hmlnarik
 */
public class YamlContextAwareParser<E> {

    private static final Logger LOG = Logger.getLogger(YamlContextAwareParser.class);

    private static final Resolver RESOLVER = new Resolver();
    private final Parser parser;
    private final YamlContextStack contextStack;

    // Leverage SnakeYaml's translation of primitive values
    private static final class MiniConstructor extends SafeConstructor {

        // This has been based on SnakeYaml's own Constuctor.constructStandardJavaInstance
        @SuppressWarnings(value = "unchecked")
        public Object constructStandardJavaInstance(@SuppressWarnings(value = "rawtypes") Class type, ScalarNode node) {
            Object result;
            if (type == String.class) {
                Construct stringConstructor = yamlConstructors.get(Tag.STR);
                result = stringConstructor.construct(node);
            } else if (type == Boolean.class || type == Boolean.TYPE) {
                Construct boolConstructor = yamlConstructors.get(Tag.BOOL);
                result = boolConstructor.construct(node);
            } else if (type == Character.class || type == Character.TYPE) {
                Construct charConstructor = yamlConstructors.get(Tag.STR);
                String ch = (String) charConstructor.construct(node);
                if (ch.length() == 0) {
                    result = null;
                } else if (ch.length() != 1) {
                    throw new YAMLException("Invalid node Character: '" + ch + "'; length: " + ch.length());
                } else {
                    result = Character.valueOf(ch.charAt(0));
                }
            } else if (Date.class.isAssignableFrom(type)) {
                Construct dateConstructor = yamlConstructors.get(Tag.TIMESTAMP);
                Date date = (Date) dateConstructor.construct(node);
                if (type == Date.class) {
                    result = date;
                } else {
                    try {
                        java.lang.reflect.Constructor<?> constr = type.getConstructor(long.class);
                        result = constr.newInstance(date.getTime());
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new YAMLException("Cannot construct: '" + type + "'");
                    }
                }
            } else if (type == Float.class || type == Double.class || type == Float.TYPE || type == Double.TYPE || type == BigDecimal.class) {
                if (type == BigDecimal.class) {
                    result = new BigDecimal(node.getValue());
                } else {
                    Construct doubleConstructor = yamlConstructors.get(Tag.FLOAT);
                    result = doubleConstructor.construct(node);
                    if (type == Float.class || type == Float.TYPE) {
                        result = Float.valueOf(((Double) result).floatValue());
                    }
                }
            } else if (type == Byte.class || type == Short.class || type == Integer.class || type == Long.class || type == BigInteger.class || type == Byte.TYPE || type == Short.TYPE || type == Integer.TYPE || type == Long.TYPE) {
                Construct intConstructor = yamlConstructors.get(Tag.INT);
                result = intConstructor.construct(node);
                if (type == Byte.class || type == Byte.TYPE) {
                    result = Byte.valueOf(result.toString());
                } else if (type == Short.class || type == Short.TYPE) {
                    result = Short.valueOf(result.toString());
                } else if (type == Integer.class || type == Integer.TYPE) {
                    result = Integer.valueOf(result.toString());
                } else if (type == Long.class || type == Long.TYPE) {
                    result = Long.valueOf(result.toString());
                } else {
                    // only BigInteger left
                    result = new BigInteger(result.toString());
                }
            } else if (Enum.class.isAssignableFrom(type)) {
                String enumValueName = node.getValue();
                try {
                    result = Enum.valueOf(type, enumValueName);
                } catch (Exception ex) {
                    throw new YAMLException("Unable to find enum value '" + enumValueName + "' for enum class: " + type.getName());
                }
            } else if (Calendar.class.isAssignableFrom(type)) {
                ConstructYamlTimestamp contr = new ConstructYamlTimestamp();
                contr.construct(node);
                result = contr.getCalendar();
            } else if (Number.class.isAssignableFrom(type)) {
                //since we do not know the exact type we create Float
                ConstructYamlFloat contr = new ConstructYamlFloat();
                result = contr.construct(node);
            } else if (UUID.class == type) {
                result = UUID.fromString(node.getValue());
            } else {
                if (yamlConstructors.containsKey(node.getTag())) {
                    result = yamlConstructors.get(node.getTag()).construct(node);
                } else {
                    throw new YAMLException("Unsupported class: " + type);
                }
            }
            return result;
        }
        public static final MiniConstructor INSTANCE = new MiniConstructor();
    }

    public static <E> E parse(InputStream is, YamlContext<E> initialContext) {
        Objects.requireNonNull(is, "Input stream invalid");
        Parser p = new ParserImpl(new StreamReader(new UnicodeReader(is)));
        return new YamlContextAwareParser<>(p, initialContext).parse();
    }

    protected YamlContextAwareParser(Parser p, YamlContext<E> initialContext) {
        this.parser = p;
        this.contextStack = new YamlContextStack(initialContext);
    }

    protected <E> E parse() {
        consumeEvent(Event.ID.StreamStart, "Expected a stream");

        if (!parser.checkEvent(Event.ID.StreamEnd)) {
            consumeEvent(Event.ID.DocumentStart, "Expected a document in the stream");
            parseNode();
            consumeEvent(Event.ID.DocumentEnd, "Expected a single document in the stream");
        }

        consumeEvent(Event.ID.StreamEnd, "Expected a single document in the stream");

        return (E) contextStack.pop().getResult();
    }

    protected Object parseNode() {
        if (parser.checkEvent(Event.ID.Alias)) {
            throw new IllegalStateException("Aliases are not handled at this moment");
        }
        Event ev = parser.getEvent();
//        System.out.println("  Parsing " + ev);
        if (!(ev instanceof NodeEvent)) {
            throw new IllegalArgumentException("Invalid event " + ev);
        }
//        if (anchor != null) {
//            node.setAnchor(anchor);
//            anchors.put(anchor, node);
//        }
//        try {
        switch (ev.getEventId()) {
            case Scalar:
                ScalarEvent se = (ScalarEvent) ev;
                boolean implicit = se.getImplicit().canOmitTagInPlainScalar();
                Tag nodeTag = constructTag(se.getTag(), NodeId.scalar, se.getValue(), implicit);
                return parseScalar(nodeTag, se);
            case SequenceStart:
                return parseSequence();
            case MappingStart:
                return parseMapping();
            default:
                throw new IllegalStateException("Event not expected " + ev);
        }
//        } finally {
//            anchors.remove(anchor);
//        }
    }

    protected Object parseSequence() {
        LOG.debugf("Parsing sequence");
        YamlContext context = contextStack.peek();
        while (! parser.checkEvent(Event.ID.SequenceEnd)) {
            context.add(parseNodeInFreshContext("[]"));
        }
        consumeEvent(Event.ID.SequenceEnd, "Expected end of sequence");
        return context.getResult();
    }

    protected Object parseMapping() {
        LOG.debugf("Parsing mapping");
        YamlContext context = contextStack.peek();
        while (! parser.checkEvent(Event.ID.MappingEnd)) {
            // TODO: add support for key Tag.MERGE tag if needed
            Object key = parseNodeInFreshContext();
            LOG.debugf("Parsed mapping key: %s", key);
            if (! (key instanceof String)) {
                throw new IllegalStateException("Invalid key in map: " + key);
            }
            Object value = parseNodeInFreshContext(key);
            LOG.debugf("Parsed mapping value: %s", value);
            context.add((String) key, value);
        }
        consumeEvent(Event.ID.MappingEnd, "Expected end of mapping");
        return context.getResult();
    }

    protected Object parseScalar(Tag nodeTag, ScalarEvent se) {
        YamlContext context = contextStack.peek();
//        System.out.println("value: " + se.getValue() + ", context: " + context + ", type: " + nodeTag.getClassName());
        ScalarNode node = new ScalarNode(nodeTag, se.getValue(), se.getStartMark(), se.getEndMark(), se.getScalarStyle());
        final Object value = MiniConstructor.INSTANCE.constructStandardJavaInstance(node.getType(), node);
        context.add(value);
        return context.getResult();
    }
    private static final EnumMap<Event.ID, Supplier<YamlContext<?>>> CONTEXT_CONSTRUCTORS = new EnumMap<>(Event.ID.class);
    static {
        CONTEXT_CONSTRUCTORS.put(ID.Scalar, DefaultObjectContext::new);
        CONTEXT_CONSTRUCTORS.put(ID.SequenceStart, DefaultListContext::new);
        CONTEXT_CONSTRUCTORS.put(ID.MappingStart, DefaultMapContext::new);
    }

    /**
     * Ensure that the next event is the expectedEventId, otherwise throw an exception, and consume that event
     */
    private Event consumeEvent(ID expectedEventId, String message) throws IllegalArgumentException {
        if (!parser.checkEvent(expectedEventId)) {
            Event event = parser.getEvent();
            throw new IllegalArgumentException(message + " at " + event.getStartMark());
        }
        return parser.getEvent();
    }

    private static Tag constructTag(String tag, NodeId nodeId, String value, boolean implicit) {
        Tag nodeTag;
        if (tag == null || tag.equals("!")) {
            nodeTag = RESOLVER.resolve(nodeId, value, implicit);
        } else {
            nodeTag = new Tag(tag);
        }
        return nodeTag;
    }

    private Object parseNodeInFreshContext(Object key) throws IllegalStateException {
        Supplier<YamlContext<?>> cc = CONTEXT_CONSTRUCTORS.get(parser.peekEvent().getEventId());
        if (cc == null) {
            throw new IllegalStateException("Invalid value in map with key " + key);
        }
        contextStack.push((String) key, cc);
        Object value = parseNode();
        contextStack.pop();
        return value;
    }

    private Object parseNodeInFreshContext() throws IllegalStateException {
        contextStack.push(new DefaultObjectContext());
        Object value = parseNode();
        contextStack.pop();
        return value;
    }

}
