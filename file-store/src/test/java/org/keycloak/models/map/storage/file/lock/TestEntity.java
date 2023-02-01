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
package org.keycloak.models.map.storage.file.lock;

import java.io.Serializable;
import java.util.UUID;

/**
 * A simple test entity that is meant to be stored in its serialized form. The version is set to the file's last modified time.
 */
public class TestEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient long version;

    private UUID id;

    private String name;

    public TestEntity(final String name) {
        this(name, UUID.randomUUID());
    }

    public TestEntity(final String name, final UUID id) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return this.id.toString();
    }

    public long getVersion() {
        return this.version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "TestEntity, id=" + id + ", name=" + name + ", version=" + version;
    }
}
