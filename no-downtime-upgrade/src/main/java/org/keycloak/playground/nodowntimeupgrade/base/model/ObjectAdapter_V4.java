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
public class ObjectAdapter_V4 implements ObjectModel_V4 {

    public final String id;
    public String name;
    public String clientScopeId;
    public Integer timeout1;
    public Integer timeout2;

    public ObjectAdapter_V4(String id) {
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
    public Integer getTimeout1() {
        return timeout1;
    }

    @Override
    public void setTimeout1(Integer timeout1) {
        this.timeout1 = timeout1;
    }

    @Override
    public Integer getTimeout2() {
        return timeout2;
    }

    @Override
    public void setTimeout2(Integer timeout2) {
        this.timeout2 = timeout2;
    }

    @Override
    public String toString() {
        return "ObjectModel_V4Impl{" + "id=" + id + ", name=" + name + ", clientScopeId=" + clientScopeId + ", timeout1=" + timeout1 + ", timeout2=" + timeout2 + '}';
    }

}
