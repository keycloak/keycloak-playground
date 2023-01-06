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
package org.keycloak.models.map.storage.file.client;

import java.util.Map;
import java.util.function.Supplier;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.storage.file.Mech;
import org.keycloak.models.map.storage.file.YamlContext;
import org.keycloak.models.map.storage.file.realm.MapEntityYamlContext;


/**
 *
 * @author hmlnarik
 */
public class ClientYamlContext extends MapEntityYamlContext<MapClientEntity> {

    private static final Map<String, EntityField<? super MapClientEntity>> NAME_TO_ENTITY_FIELD = fieldsToEntityField(MapClientEntity.class);
    private static final Map<String, Supplier<? extends YamlContext<?>>> CONTEXT_CREATORS = fieldsToContextCreators(MapClientEntity.class);

    static {
        CONTEXT_CREATORS.put(MapClientEntityFields.PROTOCOL_MAPPERS.getNameCamelCase(), ProtocolMappersYamlContext::new);

        // To-be-generated by @Shortcut(name="displayName", key="displayName")
//        CONTEXT_CREATORS.put("displayName", () -> AttributesLikeYamlContext.singletonAttributesMap("displayName"));
//        NAME_TO_ENTITY_FIELD.put("displayName", MapClientEntityFields.ATTRIBUTES);

        // To-be-generated by @Shortcut(name="browserHeaders", processor=FileClientBrowserHeaders.class)
//        CONTEXT_CREATORS.put("browserHeaders", FileClientBrowserHeaders::produceYamlContext);
//        NAME_TO_ENTITY_FIELD.put("browserHeaders", MapClientEntityFields.ATTRIBUTES);

        // To-be-generated by @Shortcut(name="keys", processor=FileClientBrowserHeaders.class)
//        CONTEXT_CREATORS.put("keys", KeysYamlContext::new);
//        NAME_TO_ENTITY_FIELD.put("keys", MapClientEntityFields.COMPONENTS);
    }

    public ClientYamlContext() {
        super(MapClientEntity.class, NAME_TO_ENTITY_FIELD, CONTEXT_CREATORS);
    }


    public void writeValue(MapClientEntity entity, Mech mech) {
        
    }
}
