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

import java.util.List;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntityFields;
import org.keycloak.models.map.storage.file.Mech;
import org.keycloak.models.map.storage.file.realm.MapEntityYamlContext;

public class ProtocolMappersYamlContext extends MapEntityYamlContext.MapEntitySequenceYamlContext<MapProtocolMapperEntity> {

    public ProtocolMappersYamlContext() {
        super(MapClientEntityFields.PROTOCOL_MAPPERS.getNameCamelCase(), MapProtocolMapperEntity.class);
    }

    @Override
    public void writeValue(Object value, Mech mech) {
        mech.addScalar(getName());
        mech.startMapping();
        for (MapProtocolMapperEntity e : (List<MapProtocolMapperEntity>) value) {
            mech.addScalar(e.getName()); // assuming name is specified, todo check name vs id

            mech.startMapping();
            getContext(MapProtocolMapperEntityFields.PROTOCOL_MAPPER.getNameCamelCase()).writeValue(e.getProtocolMapper(), mech);
            getContext(MapProtocolMapperEntityFields.CONFIG.getNameCamelCase()).writeValue(e.getConfig(), mech);
            mech.endMapping();
        }
        mech.endMapping();
    }
}
