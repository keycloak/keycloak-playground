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
package org.keycloak.playground.nodowntimeupgrade;

import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectAdapter_V1;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V1;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_V1;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V4.formatName;

/**
 *
 * @author hmlnarik
 */
public class VersionUtil_V1 implements VersionUtil<ObjectModel_V1> {

    public static final VersionUtil_V1 V1_UTIL = new VersionUtil_V1();

    @Override
    public void assertValid(ObjectModel_V1 model) {
        
    }

    @Override
    public ObjectModel_V1 newInstance(int localIndex) {
        ObjectAdapter_V1 model = new ObjectAdapter_V1(String.valueOf(INITIAL_INDEX_V1 + localIndex));
        model.setClientTemplateId("ct" + (localIndex / 10));
        model.setName(formatName(model.getId()));

        return model;
    }

}
