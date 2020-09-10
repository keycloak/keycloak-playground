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
 * Object model of a version {@link ModelVersion#VERSION_4}, the newest version.
 * The newest version is supposed to be backward compatible with this version.
 * It is expected that the versions {@link ModelVersion#VERSION_3} and
 * {@link ModelVersion#VERSION_4} can be run simultaneously in the cluster.
 *
 * @author hmlnarik
 */
public interface ObjectModel_V4 extends HasId<String> {

    public class SearchableFields {
        public static final String NAME = "name";
        public static final String TIMEOUT1 = "timeout1";
        public static final String TIMEOUT2 = "timeout2";
    }

    String getName();
    void setName(String name);

    String getClientScopeId();
    void setClientScopeId(String clientScopeId);

    /**
     * Timeout from version {@link ModelVersion#VERSION_3} was split into timeout1
     * and timeout2 to capture two different usages.
     * Version {@link ModelVersion#VERSION_3} should read the minimal of the two.
     * <p>
     * <b>Migration from previous versions:</b> get value from {@code timeout}
     * <p>
     * <b>Migration to previous version:</b> get value from {@code timeout}
     * @return timeout1
     */
    Integer getTimeout1();
    void setTimeout1(Integer timeout1);

    /**
     * Timeout from version {@link ModelVersion#VERSION_3} was split into timeout1
     * and timeout2 to capture two different usages.
     * Version {@link ModelVersion#VERSION_3} should read the minimal of the two.
     * <p>
     * <b>Migration from previous versions:</b> get value from {@code timeout}
     * <p>
     * <b>Migration to previous version:</b> get value from {@code timeout}
     * @return timeout2
     */
    Integer getTimeout2();
    void setTimeout2(Integer timeout2);

}
