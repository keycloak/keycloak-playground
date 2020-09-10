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

import org.keycloak.playground.nodowntimeupgrade.base.model.HasId;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3;
import org.keycloak.playground.nodowntimeupgrade.base.storage.ModelCriteriaBuilder;
import org.keycloak.playground.nodowntimeupgrade.base.storage.ModelCriteriaBuilder.Operator;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_FACTOR;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_V3;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_V4;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V3.getTimeout;
import static org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3.SearchableFields.TIMEOUT;

/**
 *
 * @author hmlnarik
 */
public class CriteriaOnTimeoutField_V3Test extends AbstractNoDowntimeUpgradeTest {

    private static final int INITIAL_COUNT_V1 = 100;
    private static final int INITIAL_COUNT_V3 = 400;
    private static final int INITIAL_COUNT_V4 = 200;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void init() {
        createInstances(INITIAL_COUNT_V1, INITIAL_COUNT_V3, INITIAL_COUNT_V4);
    }

    @Test
    public void testFieldEquals() {
        Integer key = INITIAL_INDEX_V3 + 4;

        List<ObjectModel_V3> objects;
        final int timeout = getTimeout(key);

        ModelCriteriaBuilder cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.EQ, timeout);

        objects = storageV3.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(
            (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i) == timeout).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) == timeout).count()
        ));
        for (ObjectModel_V3 m : objects) {
            VersionUtil_V3.V3_UTIL.assertValid(m);
            assertThat(m.getTimeout(), is(timeout));
        }
        assertThat(objects.stream().map(HasId::getId).collect(Collectors.toSet()), hasItem(String.valueOf(key)));

        cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.EQ, timeout)
          .compare(TIMEOUT, Operator.EQ, getTimeout(key + 1));
        assertThat(storageV3.read(cb).collect(Collectors.toList()), hasSize(0));
    }

    @Test
    public void testFieldNotEquals() {
        Integer key = INITIAL_INDEX_FACTOR + 4;

        List<ObjectModel_V3> objects;

        final int timeout = getTimeout(key);
        final int timeout1 = getTimeout(key + 1);
        ModelCriteriaBuilder cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.NE, timeout);

        objects = storageV3.read(cb).sorted(Comparator.comparing(HasId::getId)).collect(Collectors.toList());
        assertThat(objects, hasSize(
            INITIAL_COUNT_V1
          + (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i) != timeout).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) != timeout).count()
        ));
        for (ObjectModel_V3 m : objects) {
            VersionUtil_V3.V3_UTIL.assertValid(m);
            assertThat(m.getTimeout(), not(equals(timeout)));
        }
        assertThat(objects.stream().map(HasId::getId).collect(Collectors.toSet()), not(contains(key)));

        cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.NE, timeout)
          .compare(TIMEOUT, Operator.NE, timeout1);
        assertThat(storageV3.read(cb).collect(Collectors.toList()), hasSize(
            INITIAL_COUNT_V1
          + (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i)       != timeout && VersionUtil_V3.getTimeout(i)       != timeout1).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) != timeout && VersionUtil_V3.getTimeoutFromV4(i) != timeout1).count()
        ));
    }

    @Test
    public void testFieldGreaterThan() {
        Integer key = INITIAL_INDEX_V3 + 4;

        List<ObjectModel_V3> objects;
        final int timeout = getTimeout(key);
        final int timeout1 = getTimeout(key + 1);

        ModelCriteriaBuilder cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.GT, timeout);

        objects = storageV3.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(
            INITIAL_COUNT_V1
          + (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i) > timeout).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) > timeout).count()
        ));
        for (ObjectModel_V3 m : objects) {
            VersionUtil_V3.V3_UTIL.assertValid(m);
        }

        cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.GT, timeout)
          .compare(TIMEOUT, Operator.GT, timeout1);
        assertThat(storageV3.read(cb).collect(Collectors.toList()), hasSize(
            INITIAL_COUNT_V1
          + (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i)       > timeout && VersionUtil_V3.getTimeout(i)       > timeout1).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) > timeout && VersionUtil_V3.getTimeoutFromV4(i) > timeout1).count()
        ));
    }

    @Test
    public void testFieldGreaterThanEquals() {
        Integer key = INITIAL_INDEX_V3 + 4;

        List<ObjectModel_V3> objects;
        final int timeout = getTimeout(key);
        final int timeout1 = getTimeout(key + 1);

        ModelCriteriaBuilder cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.GE, timeout);

        objects = storageV3.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(
            INITIAL_COUNT_V1
          + (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i) >= timeout).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) >= timeout).count()
        ));
        for (ObjectModel_V3 m : objects) {
            VersionUtil_V3.V3_UTIL.assertValid(m);
        }

        cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.GE, timeout)
          .compare(TIMEOUT, Operator.GE, timeout1);
        assertThat(storageV3.read(cb).collect(Collectors.toList()), hasSize(
            INITIAL_COUNT_V1
          + (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i)       >= timeout && VersionUtil_V3.getTimeout(i)       >= timeout1).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) >= timeout && VersionUtil_V3.getTimeoutFromV4(i) >= timeout1).count()
        ));
    }

    @Test
    public void testFieldLessThan() {
        Integer key = INITIAL_INDEX_V3 + 4;

        List<ObjectModel_V3> objects;
        final int timeout = getTimeout(key);
        final int timeout1 = getTimeout(key + 1);

        ModelCriteriaBuilder cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.LT, timeout);

        objects = storageV3.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(
            (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i) < timeout).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) < timeout).count()
        ));
        for (ObjectModel_V3 m : objects) {
            VersionUtil_V3.V3_UTIL.assertValid(m);
        }

        cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.LT, timeout)
          .compare(TIMEOUT, Operator.LT, timeout1);
        assertThat(storageV3.read(cb).collect(Collectors.toList()), hasSize(
            (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i)       < timeout && VersionUtil_V3.getTimeout(i)       < timeout1).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) < timeout && VersionUtil_V3.getTimeoutFromV4(i) < timeout1).count()
        ));
    }

    @Test
    public void testFieldLessThanEquals() {
        Integer key = INITIAL_INDEX_V3 + 4;

        List<ObjectModel_V3> objects;
        final int timeout = getTimeout(key);
        final int timeout1 = getTimeout(key + 1);

        ModelCriteriaBuilder cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.LE, timeout);

        objects = storageV3.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(
            (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i) <= timeout).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) <= timeout).count()
        ));
        for (ObjectModel_V3 m : objects) {
            VersionUtil_V3.V3_UTIL.assertValid(m);
        }

        cb = storageV3.getCriteriaBuilder()
          .compare(TIMEOUT, Operator.LE, timeout)
          .compare(TIMEOUT, Operator.LE, timeout1);
        assertThat(storageV3.read(cb).collect(Collectors.toList()), hasSize(
            (int) v3indices().filter(i -> VersionUtil_V3.getTimeout(i)       <= timeout && VersionUtil_V3.getTimeout(i)       <= timeout1).count()
          + (int) v4indices().filter(i -> VersionUtil_V3.getTimeoutFromV4(i) <= timeout && VersionUtil_V3.getTimeoutFromV4(i) <= timeout1).count()
        ));
    }

    private static IntStream v3indices() {
        return IntStream.range(INITIAL_INDEX_V3, INITIAL_INDEX_V3 + INITIAL_COUNT_V3);
    }

    private static IntStream v4indices() {
        return IntStream.range(INITIAL_INDEX_V4, INITIAL_INDEX_V4 + INITIAL_COUNT_V4);
    }

}
