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

import org.keycloak.playground.nodowntimeupgrade.base.model.ModelVersion;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V2;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3;

import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author hmlnarik
 */
public class NaiveJacksonStorage_V2 extends AbstractNaiveVersionedJacksonStorage<ObjectModel_V2, ObjectEntity_V2> {

    public NaiveJacksonStorage_V2(ConcurrentMap<String, byte[]> store) {
        super(store, ModelVersion.VERSION_3,
          ObjectModel_V2.class, ObjectEntity_V2::fromModel,
          ObjectEntity_V2.class, ObjectEntity_V2::toModel,
          new NaiveJacksonCriteriaBuilder_V3()
        );
    }
}
