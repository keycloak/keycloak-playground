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
package org.keycloak.playground.nodowntimeupgrade.base.model;

/**
 *
 * @author hmlnarik
 */
public class ObjectAdapter_V1 implements ObjectModel_V1 {

    public final String id;
    public String name;
    public String clientTemplateId;

    public ObjectAdapter_V1(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getClientTemplateId() {
        return clientTemplateId;
    }

    @Override
    public void setClientTemplateId(String clientTemplateId) {
        this.clientTemplateId = clientTemplateId;
    }

    @Override
    public String toString() {
        return "ObjectModel_V1Impl{" + "id=" + id + ", name=" + name + ", clientTemplateId=" + clientTemplateId + '}';
    }

}
