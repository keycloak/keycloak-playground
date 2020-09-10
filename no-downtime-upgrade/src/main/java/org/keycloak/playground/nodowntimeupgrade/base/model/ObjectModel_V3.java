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
 * Object model of a version {@link ModelVersion#VERSION_3}, one version older than the newest version.
 * The newest version is supposed to be backward compatible with this version.
 * @author hmlnarik
 */
public interface ObjectModel_V3 extends HasId<String> {

    public class SearchableFields {
        public static final String NAME = "name";
        public static final String TIMEOUT = "timeout";
    }

    String getName();
    void setName(String name);

    String getClientScopeId();
    void setClientScopeId(String clientScopeId);

    /**
     * Timeout field. During the usage it turned out that the timeout is too coarse and
     * will need to be refined in the next version into two timeouts. This was not known
     * in version {@link ModelVersion#VERSION_3} though.
     * @return
     */
    int getTimeout();
    void setTimeout(int timeout);
}
