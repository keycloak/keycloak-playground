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

import org.keycloak.models.map.storage.file.YamlContext.DefaultMapContext;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.keycloak.models.map.storage.file.writer.WritingMechanism;

/**
 * YAML parser context which suitable for properties stored in a {@code Map<String, List<String>>}
 * which accepts
 *
 * @author hmlnarik
 */
public class AttributesLikeYamlContext extends DefaultMapContext {

    /**
     * Returns a YAML attribute-like context where key of each element
     * is stored in YAML file without a given prefix, and in the internal
     * representation each key has that prefix.
     *
     * @param prefix
     * @return
     */
    public static AttributesLikeYamlContext prefixed(String prefix) {
        return new Prefixed(prefix);
    }

    public static DefaultMapContext singletonAttributesMap(String key) {
        return new SingletonAttributesMapYamlContext(key);
    }

    @Override
    public AttributeValueYamlContext getContext(String nameOfSubcontext) {
        // regardless of the key name, the values need to be converted into Set<String> which is the purpose of AttributeValueYamlContext
        return new AttributeValueYamlContext();
    }

    @Override
    public void writeValue(Map<String, Object> value, WritingMechanism mech, Runnable addKeyEvent) {
        if (value == null || value.isEmpty()) return;
        addKeyEvent.run();
        mech.startMapping();
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            Collection<Object> attrValues = (Collection<Object>) entry.getValue();

            getContext("").writeValue(attrValues, mech, () -> {
                mech.addScalar(entry.getKey());
            });
        }
        mech.endMapping();
    }

    private static class Prefixed extends AttributesLikeYamlContext {

        protected final String prefix;

        public Prefixed(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void add(String name, Object value) {
            super.add(prefix + name, value);
        }
    }

    private static class SingletonAttributesMapYamlContext extends DefaultMapContext {

        protected final String key;

        public SingletonAttributesMapYamlContext(String key) {
            this.key = key;
        }

        @Override
        public void add(Object value) {
            if (value != null) {
                LinkedList<String> stringList = (LinkedList<String>) getResult().computeIfAbsent(key, s -> new LinkedList<>());
                stringList.add(String.valueOf(value));
            }
        }
    }

    public static class AttributeValueYamlContext extends DefaultListContext {

        @Override
        public void writeValue(Collection<Object> value, WritingMechanism mech, Runnable addKeyEvent) {
            if (value == null || value.isEmpty()) return;
            addKeyEvent.run();
            if (value.size() == 1) {
                mech.addScalar(value.stream().findAny().orElseThrow());
            } else {
                //sequence
                super.writeValue(value, mech, () -> {});
            }
        }

        @Override
        public void add(Object value) {
            if (value != null) {
                super.add(String.valueOf(value));
            }
        }
    }

}
