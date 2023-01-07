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

import java.util.Collection;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntityFields;
import org.keycloak.models.map.storage.file.YamlContext;
import org.keycloak.models.map.storage.file.MapEntityYamlContext;
import org.keycloak.models.map.storage.file.writer.WritingMechanism;

public class ProtocolMappersYamlContext extends MapEntityYamlContext.MapEntitySequenceYamlContext<MapProtocolMapperEntity> {

    public ProtocolMappersYamlContext() {
        super(MapProtocolMapperEntity.class);
    }

//    name1:
//      protocolMapper: pm
//      config:
//        pma: a
//        pmb: b
    @Override
    public void writeValue(Collection<Object> value, WritingMechanism mech) {
        mech.startMapping();
        YamlContext elementContext = getContext("");
        for (Object o : value) {
            MapProtocolMapperEntity e = (MapProtocolMapperEntity) o;
            mech.addScalar(e.getName()); // assuming name is specified, todo check name vs id

            mech.startMapping();
            mech.addScalar(MapProtocolMapperEntityFields.PROTOCOL_MAPPER.getNameCamelCase());
            elementContext.getContext(MapProtocolMapperEntityFields.PROTOCOL_MAPPER.getNameCamelCase()).writeValue(e.getProtocolMapper(), mech);

            mech.addScalar(MapProtocolMapperEntityFields.CONFIG.getNameCamelCase());
            elementContext.getContext(MapProtocolMapperEntityFields.CONFIG.getNameCamelCase()).writeValue(e.getConfig(), mech);
            mech.endMapping();
        }
        mech.endMapping();
    }
}
