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

import org.keycloak.playground.nodowntimeupgrade.base.model.HasId;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import org.keycloak.playground.nodowntimeupgrade.base.storage.Storage;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractMapStorage<ModelType extends HasId<String>> implements Storage<ModelType> {

    protected final ConcurrentMap<String, byte[]> store;

    public AbstractMapStorage(ConcurrentMap<String, byte[]> store) {
        this.store = store;
    }

    @Override
    public String create(ModelType object) {
        try {
            String id = object.getId();
            while (id == null || store.putIfAbsent(id, modelToByteArray(object)) != null) {
                id = Integer.toString(0x40000000 + new Random().nextInt(0x40000000 - 1));
            }
            return id;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ModelType read(String id) {
        try {
            return id == null ? null : byteArrayToModel(store.get(id));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void write(ModelType object) {
        try {
            store.put(object.getId(), modelToByteArray(object));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void delete(String id) {
        if (id != null) {
            store.remove(id);
        }
    }

    @Override
    public Set<String> keys() {
        return store.keySet();
    }

    protected abstract byte[] modelToByteArray(ModelType object) throws Exception;

    protected abstract ModelType byteArrayToModel(byte[] bytes) throws Exception;
}
