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
package org.keycloak.models.map.storage.file.entity;

import org.keycloak.models.map.common.EntityField;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.introspector.Property;

/**
 *
 * @author hmlnarik
 */
public class MapFieldProperty<E> extends Property {

    private static final Map<Class<?>, Class<?>> INTERFACE_TO_IMPL = new HashMap<>();
    static {
        INTERFACE_TO_IMPL.put(List.class, ArrayList.class);
        INTERFACE_TO_IMPL.put(Map.class, LinkedHashMap.class);
        INTERFACE_TO_IMPL.put(Set.class, LinkedHashSet.class);
    }

    private final EntityField<E> ef;

    public MapFieldProperty(EntityField<E> ef) {
        super(ef.getNameDashed(), ef.getFieldClass());
        this.ef = ef;
    }

    @Override
    public Class<?>[] getActualTypeArguments() {
        if (ef.getCollectionElementClass() != Void.class) {
            return new Class<?>[] { 
                INTERFACE_TO_IMPL.getOrDefault(ef.getCollectionElementClass(), ef.getCollectionElementClass())
            };
        }
        if (ef.getMapKeyClass() != Void.class) {
            return new Class<?>[] {
                INTERFACE_TO_IMPL.getOrDefault(ef.getMapKeyClass(), ef.getMapKeyClass()),
                INTERFACE_TO_IMPL.getOrDefault(ef.getMapValueClass(), ef.getMapValueClass())
            };
        }
        return null;
    }

    @Override
    public void set(Object object, Object value) {
        ef.set((E) object, value);
    }

    @Override
    public Object get(Object object) {
        return ef.get((E) object);
    }

    @Override
    public List<Annotation> getAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return null;
    }

}
