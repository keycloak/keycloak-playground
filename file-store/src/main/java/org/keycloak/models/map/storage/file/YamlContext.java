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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.keycloak.models.map.common.UndefinedValuesUtils;
import org.keycloak.models.map.storage.file.writer.WritingMechanism;

/**
 * A class implementing a {@code YamlContext} interface represents a transformer
 * from a primitive value / sequence / mapping representation as declared in YAML
 * format into a Java object of type {@code V}, with ability to produce
 * the {@link #getResult() resulting instance} of parsing.
 *
 * <p>
 * NOTE: In the future, this transformer might also cover the other direction:
 * conversion from Java object into YAML primitive value / sequence / mapping representation.
 *
 * <p>
 * This transformer handles only a single nesting level in YAML file. The root level
 * is at the beginning of YAML document. Every mapping key and every sequence then
 * represents next level of nesting.
 *
 * <h3>Examples</h3>
 *
 *
 * @author hmlnarik
 * @param <V> Type of the result
 */
public interface YamlContext<V> {

    /**
     * 
     * @param value
     * @param mech 
     */
    void writeValue(V value, WritingMechanism mech, Runnable preTask);

    /**
     * Called after reading a key of map entry in YAML file and before reading its value.
     * The key of the entry is represented as {@code nameOfSubcontext} parameter, and
     * provides means to specify a {@code YamlContext} for transforming the mapping value
     * into appropriate Java object.
     *
     * @param nameOfSubcontext Key of the map entry
     *
     * @return Context used for transforming the value,
     * or {@code null} if the default primitive / sequence / mapping context should be used instead.
     *
     * @see DefaultObjectContext
     * @see DefaultListContext
     * @see DefaultMapContext
     */
    default <C> YamlContext<C> getContext(String nameOfSubcontext) {
        return null;
    }

    /**
     * Modifies the {@link #getResult() result returned} from within this context by
     * providing the read mapping entry {@code name} to given {@code value}.
     * <p>
     * Called after reading a map entry (both key and value) from the YAML file is finished.
     * The entry is represented as {@code name} parameter (key part of the entry)
     * and {@code value} (value part of the entry).
     * <p>
     * The method is called in the same order as the mapping items appear in the source YAML mapping.
     *
     * @param name
     * @param value
     */
    default void add(String name, Object value) { };

    /**
     * Modifies the {@link #getResult() result returned} from within this context by
     * providing the read primitive value or a single sequence item in the {@code value} parameter.
     * <p>
     * Called after reading a primitive value or a single sequence item
     * from the YAML file is finished.
     * <p>
     * If the parsed YAML part was a sequence, this method is called in the same order
     * as the sequence items appear in the source YAML sequence.
     *
     * @param value
     */
    default void add(Object value) { };

    /**
     * Returns the result of parsing the given part of YAML file.
     * @return
     */
    V getResult();

    public static class DefaultObjectContext implements YamlContext<Object> {
        private Object result;

        @Override
        public void add(Object value) {
            result = value;
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void writeValue(Object value, WritingMechanism mech, Runnable addKeyEvent) {
            if (UndefinedValuesUtils.isUndefined(value)) return;
            addKeyEvent.run();
            mech.addScalar(value);
        }
    }

    public static class DefaultListContext implements YamlContext<Collection<Object>> {
        private final List<Object> result = new LinkedList<>();

        @Override
        public void add(Object value) {
            result.add(value);
        }

        @Override
        public Collection<Object> getResult() {
            return result;
        }

        @Override
        public void writeValue(Collection<Object> value, WritingMechanism mech, Runnable addKeyEvent) {
            if (UndefinedValuesUtils.isUndefined(value)) return;
            addKeyEvent.run();
            mech.startSequence();
            for (Object v : value) {
                mech.addScalar(v);
            }
            mech.endSequence();
        }
    }

    public static class DefaultMapContext implements YamlContext<Map<String, Object>> {
        private final Map<String, Object> result = new LinkedHashMap<>();

        @Override
        public void add(String name, Object value) {
            result.put(name, value);
        }

        @Override
        public Map<String, Object> getResult() {
            return result;
        }

        @Override
        public void writeValue(Map<String, Object> value, WritingMechanism mech, Runnable addKeyEvent) {
            if (UndefinedValuesUtils.isUndefined(value)) return;
            addKeyEvent.run();
            mech.startMapping();
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                mech.addScalar(entry.getKey());
                mech.addScalar(entry.getValue());
            }
            mech.endMapping();
        }
    }

}
