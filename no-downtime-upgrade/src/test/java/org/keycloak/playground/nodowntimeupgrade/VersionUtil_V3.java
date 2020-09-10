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

import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectAdapter_V3;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3;
import org.junit.Assert;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_FACTOR;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_V3;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V4.formatName;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V4.timeoutOnV4Set;
import static org.keycloak.playground.nodowntimeupgrade.base.model.Constants.DEFAULT_V3_TIMEOUT;

/**
 *
 * @author hmlnarik
 */
public class VersionUtil_V3 implements VersionUtil<ObjectModel_V3> {

    public static final VersionUtil_V3 V3_UTIL = new VersionUtil_V3();

    @Override
    public void assertValid(ObjectModel_V3 model) {
        assertThat(model, notNullValue());
        int originalModelVersion = Integer.valueOf(model.getId()) / INITIAL_INDEX_FACTOR;
        int originalModelIndex = Integer.valueOf(model.getId()) % INITIAL_INDEX_FACTOR;

        assertThat(model.getName(), is(formatName(model.getId())));
        switch (originalModelVersion) {
            case 1:
                assertThat(model.getClientScopeId(), is("template-" + "ct" + (originalModelIndex / 10)));
                assertThat(model.getTimeout(), is(DEFAULT_V3_TIMEOUT));
                break;
            case 3:
                assertThat(model.getClientScopeId(), is("cs" + (originalModelIndex / 10)));
                assertThat(model.getTimeout(), is(getTimeout(Integer.valueOf(model.getId()))));
                break;
            case 4:
                assertThat(model.getClientScopeId(), is("cs" + (originalModelIndex / 30)));
                assertThat(model.getTimeout(), is(getTimeoutFromV4(Integer.valueOf(model.getId()))));
                break;
            default:
                Assert.fail("Unknown model version: " + model);
        }
    }

    @Override
    public ObjectModel_V3 newInstance(int localIndex) {
        ObjectAdapter_V3 model = new ObjectAdapter_V3(String.valueOf(INITIAL_INDEX_V3 + localIndex));
        model.setClientScopeId("cs" + (localIndex / 10));
        if (timeoutOnV3Set(localIndex)) {
            model.setTimeout(localIndex % 10);
        }
        model.setName(formatName(model.getId()));

        return model;
    }

    public static int getTimeout(Integer id) {
        return timeoutOnV3Set(id) ? (id % 10) : DEFAULT_V3_TIMEOUT;
    }

    public static int getTimeoutFromV4(int id) {
        int originalModelIndex = id % INITIAL_INDEX_FACTOR;
        return timeoutOnV4Set(originalModelIndex)
          ? Math.min(10 - (originalModelIndex % 10), (originalModelIndex + 5) % 10)
          : DEFAULT_V3_TIMEOUT;
    }

    public static boolean timeoutOnV3Set(int i) {
        return ((i % INITIAL_INDEX_FACTOR) % 50) < 30;
    }

}
