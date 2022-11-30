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

import org.keycloak.models.map.realm.MapRealmEntity;
import java.io.InputStream;
import org.keycloak.models.map.storage.file.YamlContextAwareParser;
import java.util.Objects;

/**
 *
 * @author hmlnarik
 */
public class RealmParser {

    public MapRealmEntity parse(InputStream is) {
        return YamlContextAwareParser.parse(is, new RealmYamlContext());
    }

    public MapRealmEntity getRealmById(String realmId) {
        MapRealmEntity res = parse(getClass().getResourceAsStream("/testdir/" + realmId + "/realm.yaml"));
        if (res == null) {
            return null;
        }
        if (res.getId() == null || Objects.equals(res.getId(), realmId)) {
            res.setId(realmId);
        }
        return res;
    }
}
