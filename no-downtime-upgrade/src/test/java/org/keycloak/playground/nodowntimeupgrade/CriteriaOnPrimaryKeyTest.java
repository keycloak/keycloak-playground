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
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V4;
import org.keycloak.playground.nodowntimeupgrade.base.storage.ModelCriteriaBuilder;
import org.keycloak.playground.nodowntimeupgrade.base.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.playground.nodowntimeupgrade.base.storage.Storage;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_FACTOR;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants.INITIAL_INDEX_V3;
import static org.keycloak.playground.nodowntimeupgrade.base.model.HasId.SearchableFields.ID;

/**
 * Test various operators on integer primary key. Each test within this class tests
 * a single operator by performing a {@link Storage#read(ModelCriteriaBuilder)} operation.
 *
 * @author hmlnarik
 */
public class CriteriaOnPrimaryKeyTest extends AbstractNoDowntimeUpgradeTest {

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
    public void testReadPrimaryKeyEquals() {
        Integer key = INITIAL_INDEX_FACTOR + 1;

        List<ObjectModel_V4> objects;

        ModelCriteriaBuilder cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.EQ, String.valueOf(key));

        objects = storageV4.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(1));
        for (ObjectModel_V4 m : objects) {
            VersionUtil_V4.V4_UTIL.assertValid(m);
        }
        assertThat(objects.stream().map(HasId::getId).collect(Collectors.toSet()), contains(String.valueOf(key)));

        cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.EQ, String.valueOf(key))
          .compare(ID, Operator.EQ, String.valueOf(key + 1));
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(0));

        cb = storageV4.getCriteriaBuilder();
        cb = cb.and(
          cb.compare(ID, Operator.EQ, String.valueOf(key)),
          cb.compare(ID, Operator.EQ, String.valueOf(key + 1))
        );
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(0));

        cb = storageV4.getCriteriaBuilder();
        cb = cb.or(
          cb.compare(ID, Operator.EQ, String.valueOf(key)),
          cb.compare(ID, Operator.EQ, String.valueOf(key + 1))
        );
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(2));
    }

    @Test
    public void testReadPrimaryKeyNotEquals() {
        Integer key = INITIAL_INDEX_FACTOR + 1;

        List<ObjectModel_V4> objects;

        ModelCriteriaBuilder cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.NE, String.valueOf(key));

        objects = storageV4.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(storageV4.keys().size() - 1));
        for (ObjectModel_V4 m : objects) {
            VersionUtil_V4.V4_UTIL.assertValid(m);
        }
        assertThat(objects.stream().map(HasId::getId).collect(Collectors.toSet()), not(contains(String.valueOf(key))));

        cb = storageV4.getCriteriaBuilder();
        cb = cb.not(cb.compare(ID, Operator.EQ, String.valueOf(key)));
        objects = storageV4.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(storageV4.keys().size() - 1));

        cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.NE, String.valueOf(key))
          .compare(ID, Operator.NE, String.valueOf(key + 1));
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(storageV4.keys().size() - 2));

        cb = storageV4.getCriteriaBuilder();
        cb = cb.and(
          cb.not(cb.compare(ID, Operator.EQ, String.valueOf(key))),
          cb.not(cb.compare(ID, Operator.EQ, String.valueOf(key + 1)))
        );
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(storageV4.keys().size() - 2));
    }

    @Test
    public void testReadPrimaryKeyGreaterThan() {
        List<ObjectModel_V4> objects;

        ModelCriteriaBuilder cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.GT, String.valueOf(INITIAL_INDEX_V3));

        objects = storageV4.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(INITIAL_COUNT_V3 + INITIAL_COUNT_V4 - 1));
        for (ObjectModel_V4 m : objects) {
            VersionUtil_V4.V4_UTIL.assertValid(m);
        }
        assertThat(objects.stream().map(HasId::getId).collect(Collectors.toSet()), not(contains(String.valueOf(INITIAL_INDEX_V3))));

        cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.GT, String.valueOf(INITIAL_INDEX_V3))
          .compare(ID, Operator.GT, String.valueOf(INITIAL_INDEX_V3 + 1));
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(INITIAL_COUNT_V3 + INITIAL_COUNT_V4 - 2));
    }

    @Test
    public void testReadPrimaryKeyGreaterThanEquals() {
        List<ObjectModel_V4> objects;

        ModelCriteriaBuilder cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.GE, String.valueOf(INITIAL_INDEX_V3));

        objects = storageV4.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(INITIAL_COUNT_V3 + INITIAL_COUNT_V4));
        for (ObjectModel_V4 m : objects) {
            VersionUtil_V4.V4_UTIL.assertValid(m);
        }
        assertThat(objects.stream().map(HasId::getId).collect(Collectors.toSet()), hasItem(String.valueOf(INITIAL_INDEX_V3)));

        cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.GE, String.valueOf(INITIAL_INDEX_V3))
          .compare(ID, Operator.GE, String.valueOf(INITIAL_INDEX_V3 + 1));
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(INITIAL_COUNT_V3 + INITIAL_COUNT_V4 - 1));

        cb = storageV4.getCriteriaBuilder();
        cb = cb.and(
          cb.compare(ID, Operator.GE, String.valueOf(INITIAL_INDEX_V3)),
          cb.compare(ID, Operator.GE, String.valueOf(INITIAL_INDEX_V3 + 1))
        );
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(INITIAL_COUNT_V3 + INITIAL_COUNT_V4 - 1));
    }

    @Test
    public void testReadPrimaryKeyLessThan() {
        List<ObjectModel_V4> objects;

        ModelCriteriaBuilder cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.LT, String.valueOf(INITIAL_INDEX_V3));

        objects = storageV4.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(INITIAL_COUNT_V1));
        for (ObjectModel_V4 m : objects) {
            VersionUtil_V4.V4_UTIL.assertValid(m);
        }
        assertThat(objects.stream().map(HasId::getId).collect(Collectors.toSet()), not(contains(String.valueOf(INITIAL_INDEX_V3))));

        cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.LT, String.valueOf(INITIAL_INDEX_V3))
          .compare(ID, Operator.LT, String.valueOf(INITIAL_INDEX_V3 + 1));
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(INITIAL_COUNT_V1));
    }

    @Test
    public void testReadPrimaryKeyLessThanEquals() {
        List<ObjectModel_V4> objects;

        ModelCriteriaBuilder cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.LE, String.valueOf(INITIAL_INDEX_V3));

        objects = storageV4.read(cb).collect(Collectors.toList());
        assertThat(objects, hasSize(INITIAL_COUNT_V1 + 1));
        for (ObjectModel_V4 m : objects) {
            VersionUtil_V4.V4_UTIL.assertValid(m);
        }
        assertThat(objects.stream().map(HasId::getId).collect(Collectors.toSet()), hasItem(String.valueOf(INITIAL_INDEX_V3)));

        cb = storageV4.getCriteriaBuilder()
          .compare(ID, Operator.LE, String.valueOf(INITIAL_INDEX_V3))
          .compare(ID, Operator.LE, String.valueOf(INITIAL_INDEX_V3 + 1));
        assertThat(storageV4.read(cb).collect(Collectors.toList()), hasSize(INITIAL_COUNT_V1 + 1));
    }

}
