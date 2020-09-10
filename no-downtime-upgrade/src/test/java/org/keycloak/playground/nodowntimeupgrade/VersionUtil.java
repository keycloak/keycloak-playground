package org.keycloak.playground.nodowntimeupgrade;

import org.keycloak.playground.nodowntimeupgrade.base.model.ModelVersion;

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

/**
 *
 * @author hmlnarik
 */
public interface VersionUtil<M> {

    public static class Constants {
        public static final int INITIAL_INDEX_FACTOR = 10000000;

        public static final int INITIAL_INDEX_V1 = ModelVersion.VERSION_1.getVersion() * INITIAL_INDEX_FACTOR;
        public static final int INITIAL_INDEX_V3 = ModelVersion.VERSION_3.getVersion() * INITIAL_INDEX_FACTOR;
        public static final int INITIAL_INDEX_V4 = ModelVersion.VERSION_4.getVersion() * INITIAL_INDEX_FACTOR;
    }

    public void assertValid(M model);
    public M newInstance(int localIndex);
}
