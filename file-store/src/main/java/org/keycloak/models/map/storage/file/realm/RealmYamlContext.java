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
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.MapRealmEntityFields;
import org.keycloak.models.map.storage.file.entity.FileRealmEntity;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.models.map.storage.file.YamlContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Supplier;

/**
 *
 * @author hmlnarik
 */
public class RealmYamlContext implements YamlContext<FileRealmEntity> {

    private static final Logger LOG = Logger.getLogger(RealmYamlContext.class);

    private static final Map<String, EntityField<MapRealmEntity>> NAME_TO_ENTITY_FIELD = Stream.of(MapRealmEntityFields.values())
      .collect(Collectors.toMap(MapRealmEntityFields::getNameDashed, Function.identity()));

    private static final Map<String, Supplier<YamlContext<?>>> CONTEXT_CREATORS = new HashMap<>();
    static {
        CONTEXT_CREATORS.put(MapRealmEntityFields.ATTRIBUTES.getNameDashed(), AttributesYamlContext::new);
        CONTEXT_CREATORS.put(MapRealmEntityFields.COMPONENTS.getNameDashed(), ComponentsYamlContext::new);
    }

    private final FileRealmEntity result = new FileRealmEntity.Impl();

    @Override
    public void add(String name, Object value) {
        final EntityField<MapRealmEntity> ef = NAME_TO_ENTITY_FIELD.get(name);
        if (ef != null) {
            final Object origValue = ef.get(result);
            if (ef.getCollectionElementClass() != Void.class && value instanceof Collection) {
                ((Collection) value).forEach(v -> ef.collectionAdd(result, v));
            } else {
                if (origValue != null) {
                    LOG.warnf("Overwriting value of %s field", name);
                }
                ef.set(result, value);
            }
        } else {
            LOG.warnf("Ignoring field %s", name);
        }
    }

    @Override
    public FileRealmEntity getResult() {
        return this.result;
    }

    @Override
    public YamlContext<?> getContext(String nameOfSubcontext) {
        Supplier<YamlContext<?>> cc = CONTEXT_CREATORS.get(nameOfSubcontext);
        if (cc != null) {
            return cc.get();
        }

        EntityField<?> ef = NAME_TO_ENTITY_FIELD.get(nameOfSubcontext);
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
