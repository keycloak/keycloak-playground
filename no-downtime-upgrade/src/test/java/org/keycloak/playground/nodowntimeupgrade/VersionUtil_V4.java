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

import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectAdapter_V4;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V4;
import org.junit.Assert;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_FACTOR;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_V4;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V3.getTimeout;

/**
 *
 * @author hmlnarik
 */
public class VersionUtil_V4 implements VersionUtil<ObjectModel_V4> {

    public static final VersionUtil_V4 V4_UTIL = new VersionUtil_V4();

    @Override
    public ObjectModel_V4 newInstance(int localIndex) {
        ObjectAdapter_V4 model = new ObjectAdapter_V4(String.valueOf(INITIAL_INDEX_V4 + localIndex));
        model.setClientScopeId("cs" + (localIndex / 30));
        if (timeoutOnV4Set(localIndex)) {
            model.setTimeout1(10 - (localIndex % 10));
            model.setTimeout2((localIndex + 5) % 10);
        }
        model.setName(formatName(model.getId()));

        return model;
    }

    @Override
    public void assertValid(ObjectModel_V4 model) {
        assertThat(model, notNullValue());
        int originalModelVersion = Integer.valueOf(model.getId()) / INITIAL_INDEX_FACTOR;
        int originalModelIndex = Integer.valueOf(model.getId()) % INITIAL_INDEX_FACTOR;

        assertThat(model.getName(), is(formatName(model.getId())));
        switch (originalModelVersion) {
            case 1:
                assertThat(model.getClientScopeId(), is("template-" + "ct" + (originalModelIndex / 10)));
                assertThat(model.getTimeout1(), is(nullValue()));
                assertThat(model.getTimeout2(), is(nullValue()));
                break;

            case 3:
                assertThat(model.getClientScopeId(), is("cs" + (originalModelIndex / 10)));
                assertThat(model.getTimeout1(), is(getTimeout(Integer.valueOf(model.getId()))));
                assertThat(model.getTimeout2(), is(getTimeout(Integer.valueOf(model.getId()))));
                break;

            case 4:
                assertThat(model.getClientScopeId(), is("cs" + (originalModelIndex / 30)));
                assertThat(model.getTimeout1(),
                  timeoutOnV4Set(originalModelIndex)
                    ? is(10 - (originalModelIndex % 10))
                    : nullValue()
                );
                assertThat(model.getTimeout2(),
                  timeoutOnV4Set(originalModelIndex)
                  ? is((originalModelIndex + 5) % 10)
                  : nullValue()
                );
                break;
            default:
                Assert.fail("Unknown model version: " + model);
        }
    }

    public static String formatName(Integer id) {
        int originalModelVersion = id / INITIAL_INDEX_FACTOR;
        int originalModelIndex = id % INITIAL_INDEX_FACTOR;

        return String.format("model%01d # %06d", originalModelVersion, originalModelIndex);
    }

    public static String formatName(String id) {
        return formatName(Integer.valueOf(id));
    }

    public static boolean timeoutOnV4Set(int i) {
        return ((i % INITIAL_INDEX_FACTOR) % 100) < 50;
    }

}
