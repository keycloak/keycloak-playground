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

import org.keycloak.playground.nodowntimeupgrade.base.model.Constants;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectAdapter_V4;
import org.keycloak.playground.nodowntimeupgrade.base.model.ModelVersion;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V4;

/**
 *
 * @author hmlnarik
 */
public class ObjectEntity_V4 {

    public int entityVersion = ModelVersion.VERSION_4.getVersion();

    public String id;
    public String name;
    public String clientScopeId;

    public Integer timeout1;
    public Integer timeout2;

    /**
     * @deprecated TODO: For backwards compatibility with version 3, remove in version 5
     */
    @Deprecated
    public Integer timeout;     // Constants.DEFAULT_V3_TIMEOUT default is now in getV3Timeout()

    public ObjectModel_V4 toModel() {
        final ObjectAdapter_V4 res = new ObjectAdapter_V4(id);
        res.setName(name);
        res.setClientScopeId(clientScopeId);
        res.setTimeout1(timeout1);
        res.setTimeout2(timeout2);
        return res;
    }

    public static ObjectEntity_V4 fromModel(ObjectModel_V4 object) {
        ObjectEntity_V4 res = new ObjectEntity_V4();
        res.id = object.getId();
        res.name = object.getName();
        res.clientScopeId = object.getClientScopeId();
        res.timeout1 = object.getTimeout1();
        res.timeout2 = object.getTimeout2();

        // Backwards compatibility
        res.timeout = getV3Timeout(object.getTimeout1(), object.getTimeout2());

        return res;
    }

    private static int getV3Timeout(Integer timeout1, Integer timeout2) {
        if (timeout1 == null) {
            if (timeout2 == null) {
                return Constants.DEFAULT_V3_TIMEOUT;
            } else {
                return timeout2;
            }
        }
        return timeout2 == null ? timeout1 : Math.min(timeout1, timeout2);
    }

}
