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
package org.keycloak.playground.nodowntimeupgrade.base.storage;

import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V1;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V4;

/**
 *
 * @author hmlnarik
 */
public interface VersionedStorage {

    /**
     * Returns storage that stores records in version 1.
     * @return
     */
    Storage<ObjectModel_V1> getStorageV1();
    /**
     * Returns storage that stores records in version 3.
     * @return
     */
    Storage<ObjectModel_V3> getStorageV3();
    /**
     * Returns storage that stores records in version 4.
     * @return
     */
    Storage<ObjectModel_V4> getStorageV4();
    /**
     * Cleans all the data stored in the database.
     */
    public void cleanup();
}
