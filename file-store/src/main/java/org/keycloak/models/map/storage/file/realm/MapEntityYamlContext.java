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
package org.keycloak.models.map.storage.file.realm;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.storage.file.YamlContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public abstract class MapEntityYamlContext<T> implements YamlContext<T> {

    private static final Logger LOG = Logger.getLogger(MapEntityYamlContext.class);
    
    private final Map<String, EntityField<? super T>> nameToEntityField;
    private final Map<String, Supplier<? extends YamlContext<?>>> contextCreators;

    protected final T result;

    public MapEntityYamlContext(
      T result,
      Map<String, EntityField<? super T>> nameToEntityField,
      Map<String, Supplier<? extends YamlContext<?>>> contextCreators) {
        this.result = result;
        this.nameToEntityField = nameToEntityField;
        this.contextCreators = contextCreators;
    }

    public static <T> Map<String, EntityField<? super T>> fieldsToEntityField(EntityField<T>[] e) {
        return Stream.of(e).collect(Collectors.toMap(EntityField::getNameDashed, Function.identity()));
    }

    public static <T> boolean setEntityField(T result, EntityField<? super T> ef, Object value) {
        if (ef == null) {
            return false;
        }

        if (ef.getCollectionElementClass() != Void.class && value instanceof Collection) {
            ((Collection) value).forEach(v -> ef.collectionAdd(result, v));
        } else if (ef.getMapKeyClass() != Void.class && value instanceof Map) {
            ((Map) value).forEach((k, v) -> ef.mapPut(result, k, v));
        } else {
            final Object origValue = ef.get(result);
            if (origValue != null) {
                LOG.warnf("Overwriting value of %s field", ef.getNameDashed());
            }
            ef.set(result, value);
        }
        return true;
    }

    @Override
    public void add(String name, Object value) {
        EntityField<? super T> ef = nameToEntityField.get(name);

        if (! setEntityField(result, ef, value)) {
            LOG.warnf("Ignoring field %s", name);
        }
    }

    @Override
    public T getResult() {
        return this.result;
    }

    @Override
    public YamlContext<?> getContext(String nameOfSubcontext) {
        Supplier<? extends YamlContext<?>> cc = contextCreators.get(nameOfSubcontext);
        if (cc != null) {
            return cc.get();
        }
        EntityField<?> ef = nameToEntityField.get(nameOfSubcontext);
        if (ef != null) {
            if (ef.getCollectionElementClass() != Void.class) {
                return new DefaultListContext();
            } else if (ef.getMapValueClass() == List.class) {
                return new DefaultMapContext();
            }
            return null;
        }
        LOG.warnf("No special context set for field %s", nameOfSubcontext);
        return null;
    }

}
