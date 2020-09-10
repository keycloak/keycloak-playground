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

import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3;
import java.util.Map;
import static org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3.SearchableFields.NAME;
import static org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3.SearchableFields.TIMEOUT;

/**
 *
 * @author hmlnarik
 */
public class NaiveJacksonCriteriaBuilder_V3 extends NaiveJacksonCriteriaBuilder<ObjectModel_V3> {

    private static final Map<String, TriConsumer<NaiveJacksonCriteriaBuilder<ObjectModel_V3>, Operator, Object>> PREDICATES = NaiveJacksonCriteriaBuilder.basePredicates();
    static {
        PREDICATES.put(NAME,    (o, op, value) -> o.fieldCompare(op, value, ObjectModel_V3::getName));
        PREDICATES.put(TIMEOUT, (o, op, value) -> o.fieldCompare(op, value, ObjectModel_V3::getTimeout));
    }

    public NaiveJacksonCriteriaBuilder_V3() {
        super(PREDICATES);
    }

}
