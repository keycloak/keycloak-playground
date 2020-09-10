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

import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V1;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V4;
import org.keycloak.playground.nodowntimeupgrade.base.storage.Storage;
import org.keycloak.playground.nodowntimeupgrade.base.storage.VersionedStorage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author hmlnarik
 */
public class NaiveJacksonStorage implements VersionedStorage {

    private final ConcurrentMap<String, byte[]> store = new ConcurrentHashMap<>();
    protected Storage<ObjectModel_V1> storageV1 = new NaiveJacksonStorage_V1(store);
    protected Storage<ObjectModel_V3> storageV3 = new NaiveJacksonStorage_V3(store);
    protected Storage<ObjectModel_V4> storageV4 = new NaiveJacksonStorage_V4(store);

    @Override
    public Storage<ObjectModel_V1> getStorageV1() {
        return storageV1;
    }

    @Override
    public Storage<ObjectModel_V3> getStorageV3() {
        return storageV3;
    }

    @Override
    public Storage<ObjectModel_V4> getStorageV4() {
        return storageV4;
    }

    @Override
    public void cleanup() {
        store.clear();
    }
}
