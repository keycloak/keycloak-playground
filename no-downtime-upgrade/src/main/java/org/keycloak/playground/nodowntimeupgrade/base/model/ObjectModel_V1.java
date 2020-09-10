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
 * Object model of an ancient version that is no longer supported to be run in the cluster
 * along with the current versions.
 * <p>
 * This interface is here mainly for illustration purposes, and to define which fields
 * have to be stored with version 1 used in tests.
 * 
 * @author hmlnarik
 */
public interface ObjectModel_V1 extends HasId<String> {

    public class SearchableFields {
        public static final String NAME = "name";
    }

    String getName();
    void setName(String name);

    /**
     * Client templates were deprecated and replaced by client scopes in version 2
     * (the interface for {@code ObjectModelVersion2} is intentionally missing).
     * <p>
     * Migration path was defined only in version 2. Because this file is missing, the
     * description is here: To get client scope ID from client template ID,
     * the template ID needs to be prefixed by {@code "template-"}.
     * @return
     */
    String getClientTemplateId();
    void setClientTemplateId(String clientTemplateId);
}
