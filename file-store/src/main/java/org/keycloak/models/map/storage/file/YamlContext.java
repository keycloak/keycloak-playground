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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author hmlnarik
 */
public interface YamlContext<V> {

    /**
     * Called after reading a key of map entry in YAML file and before reading its value.
     * The key of the entry is represented as {@code nameOfSubcontext} parameter, and
     * provides means to in switch the parser context.
     * @param nameOfSubcontext Key of the map entry
     */
    default YamlContext<?> getContext(String nameOfSubcontext) {
        return null;
    }

    /**
     * Called after reading a map entry from the yaml file is finished. The entry is represented as
     * {@code name} parameter (key part of the entry) and {@code value} (value part of the entry).
     * @param name
     * @param value
     */
    default void add(String name, Object value) { };

    /**
     * Called after reading an array item from the yaml file is finished. The value is represented as
     * the {@code value} parameter.
     * @param value
     */
    default void add(Object value) { };

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

    }

    public static class DefaultListContext implements YamlContext<List<Object>> {
        private final List<Object> result = new LinkedList<>();

        @Override
        public void add(Object value) {
            result.add(value);
        }

        @Override
        public List<Object> getResult() {
            return result;
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

    }

}
