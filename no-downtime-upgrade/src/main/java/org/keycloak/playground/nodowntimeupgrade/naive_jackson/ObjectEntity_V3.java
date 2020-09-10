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

import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectAdapter_V3;
import org.keycloak.playground.nodowntimeupgrade.base.model.ModelVersion;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3;
import static org.keycloak.playground.nodowntimeupgrade.base.model.Constants.DEFAULT_V3_TIMEOUT;

/**
 *
 * @author hmlnarik
 */
public class ObjectEntity_V3 {

    public int entityVersion = ModelVersion.VERSION_3.getVersion();

    public String id;
    public String name;
    public String clientScopeId;
    public int timeout = DEFAULT_V3_TIMEOUT;    // Here it is intentionally _int_, not Integer

    public ObjectModel_V3 toModel() {
        final ObjectAdapter_V3 res = new ObjectAdapter_V3(id);
        res.setName(name);
        res.setClientScopeId(clientScopeId);
        res.setTimeout(timeout);
        return res;
    }

    public static ObjectEntity_V3 fromModel(ObjectModel_V3 object) {
        ObjectEntity_V3 res = new ObjectEntity_V3();
        res.id = object.getId();
        res.name = object.getName();
        res.clientScopeId = object.getClientScopeId();
        res.timeout = object.getTimeout();
        return res;
    }

}
