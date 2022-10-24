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
import org.keycloak.models.map.realm.MapRealmEntityImpl;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.models.map.storage.file.YamlContext;
import org.keycloak.models.map.storage.file.entity.shortcut.FileRealmBrowserHeaders;
import org.keycloak.models.map.storage.file.entity.shortcut.FileRealmKeys.KeysYamlContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 *
 * @author hmlnarik
 */
public class RealmYamlContext implements YamlContext<MapRealmEntity> {

    private static final Logger LOG = Logger.getLogger(RealmYamlContext.class);

    private static final Map<String, EntityField<MapRealmEntity>> NAME_TO_ENTITY_FIELD = Stream.of(MapRealmEntityFields.values())
      .collect(Collectors.toMap(MapRealmEntityFields::getNameDashed, Function.identity()));

    private static final Map<String, Supplier<? extends YamlContext<?>>> CONTEXT_CREATORS = new HashMap<>();
    private static final Map<String, BiConsumer<MapRealmEntity, Object>> ALIASES = new HashMap<>();

    static {
        CONTEXT_CREATORS.put(MapRealmEntityFields.ATTRIBUTES.getNameDashed(), AttributesLikeYamlContext::new);
        CONTEXT_CREATORS.put(MapRealmEntityFields.COMPONENTS.getNameDashed(), ComponentsYamlContext::new);

        // To-be-generated by @Shortcut(name="displayName", key="displayName")
        ALIASES.put("displayName", (result, v) -> MapRealmEntityFields.ATTRIBUTES.mapPut(result, "displayName", Arrays.asList(String.valueOf(v))));

        // To-be-generated by @Shortcut(name="browserHeaders", processor=FileRealmBrowserHeaders.class)
        CONTEXT_CREATORS.put("browserHeaders", FileRealmBrowserHeaders::produceYamlContext);
        ALIASES.put("browserHeaders", (result, v) -> setEntityField(result, MapRealmEntityFields.ATTRIBUTES.getNameDashed(), v));

        // To-be-generated by @Shortcut(name="keys", processor=FileRealmBrowserHeaders.class)
        CONTEXT_CREATORS.put("keys", KeysYamlContext::new);
        ALIASES.put("keys", (result, v) -> setEntityField(result, MapRealmEntityFields.COMPONENTS.getNameDashed(), v));
    }

    private final MapRealmEntity result = new MapRealmEntityImpl();

    @Override
    public void add(String name, Object value) {
        if (ALIASES.containsKey(name)) {
            ALIASES.get(name).accept(result, value);
        } else if (! setEntityField(result, name, value)) {
            LOG.warnf("Ignoring field %s", name);
        }
    }

    @Override
    public MapRealmEntity getResult() {
        return this.result;
    }

    @Override
    public YamlContext<?> getContext(String nameOfSubcontext) {
        Supplier<? extends YamlContext<?>> cc = CONTEXT_CREATORS.get(nameOfSubcontext);
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

    private static boolean setEntityField(MapRealmEntity result, String name, Object value) {
        final EntityField<MapRealmEntity> ef = NAME_TO_ENTITY_FIELD.get(name);
        if (ef == null) {
            return false;
        }
        return setEntityField(result, ef, value);
    }

    private static boolean setEntityField(MapRealmEntity result, EntityField<MapRealmEntity> ef, Object value) {
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

}
