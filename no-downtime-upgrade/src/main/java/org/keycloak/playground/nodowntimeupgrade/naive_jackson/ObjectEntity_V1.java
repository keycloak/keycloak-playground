/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.playground.nodowntimeupgrade.naive_jackson;

import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectAdapter_V1;
import org.keycloak.playground.nodowntimeupgrade.base.model.ModelVersion;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V1;

/**
 *
 * @author hmlnarik
 */
public class ObjectEntity_V1 {

    public int entityVersion = ModelVersion.VERSION_1.getVersion();

    public String id;
    public String name;
    public String clientTemplateId;
    /**
     * @deprecated TODO: For backwards compatibility with version 0, remove in version 2
     */
    @Deprecated
    public String node2;

    public ObjectModel_V1 toModel() {
        final ObjectAdapter_V1 res = new ObjectAdapter_V1(id);
        res.setName(name);
        res.setClientTemplateId(clientTemplateId);
        return res;
    }

    public static ObjectEntity_V1 fromModel(ObjectModel_V1 object) {
        ObjectEntity_V1 res = new ObjectEntity_V1();
        res.id = object.getId();
        res.name = object.getName();
        res.clientTemplateId = object.getClientTemplateId();
        return res;
    }

}
