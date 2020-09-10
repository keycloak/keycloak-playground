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
public class ObjectAdapter_V3 implements ObjectModel_V3 {

    public final String id;
    public String name;
    public String clientScopeId;

    // This is examplary wrong pattern - by using constant in the initializer, the value is stored in the database
    // Hence it is not possible to distinguish default and entered values by recognizing "null" value for the former.
    // This makes migration and changing the default values challenging.
    public int timeout = Constants.DEFAULT_V3_TIMEOUT;

    public ObjectAdapter_V3(String id) {
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
    public String getClientScopeId() {
        return clientScopeId;
    }

    @Override
    public void setClientScopeId(String clientScopeId) {
        this.clientScopeId = clientScopeId;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "ObjectModel_V3Impl{" + "id=" + id + ", name=" + name + ", clientScopeId=" + clientScopeId + ", timeout=" + timeout + '}';
    }

}
