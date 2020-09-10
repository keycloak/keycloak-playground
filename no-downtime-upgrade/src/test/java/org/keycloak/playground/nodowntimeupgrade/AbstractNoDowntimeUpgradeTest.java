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
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V1;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V3;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V4;
import org.keycloak.playground.nodowntimeupgrade.base.storage.Storage;
import org.keycloak.playground.nodowntimeupgrade.base.storage.VersionedStorage;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V1.V1_UTIL;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V3.V3_UTIL;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V4.V4_UTIL;

/**
 * Base test class for the individual 0-downtime upgrade tests.
 * <p>
 * This test is parameterized by all registered implementations of {@link VersionedStorage} interface
 * available via {@link ServiceLoader} lookup. The tested implementations may be narrowed by specifying
 * a {@code storageClassRegex} system property which defines a regular expression that has to match
 * canonical class name of each tested storage implementation. By default, there is no exclusion.
 *
 * @author hmlnarik
 */
@RunWith(Parameterized.class)
public abstract class AbstractNoDowntimeUpgradeTest {

    protected Storage<ObjectModel_V1> storageV1;
    protected Storage<ObjectModel_V3> storageV3;
    protected Storage<ObjectModel_V4> storageV4;

    private static final Pattern STORAGE_CLASS_REGEX = Pattern.compile(System.getProperty("storageClassRegex", ".*"));

    @Parameter(0)
    public VersionedStorage storage;

    @Parameter(1)
    public Object storageClassName;

    @Parameters(name = "{1}")
    public static Iterable<Object[]> versionedStorages() {
        ServiceLoader<VersionedStorage> loader = ServiceLoader.load(VersionedStorage.class);
        Iterator<VersionedStorage> it = loader.iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false)
          .map(storage -> new Object[] { storage, storage.getClass().getCanonicalName() })
          .filter(o -> STORAGE_CLASS_REGEX.matcher((String) o[1]).find())
          .collect(Collectors.toList());
    }

    public static <T extends HasId<String>> AssertionError enhancedExceptionReport(AssertionError e, Storage<T> storage, T om) throws AssertionError {
        if (om == null) {
            return e;
        }
        return new AssertionError("Assertion failed (see below for details).\n"
          + "Error: " + e.getMessage() + "\n"
          + "Model: " + om
          + (om.getId() == null ? "" : "\nRaw entity data: " + storage.dump(om.getId())), e
        );
    }

    @Before
    public void initStorage() {
        storage.cleanup();
        storageV1 = storage.getStorageV1();
        storageV3 = storage.getStorageV3();
        storageV4 = storage.getStorageV4();
    }

    public void createInstances(int countV1, int countV3, int countV4) {
        for (int i = 0; i < countV1; i ++) {
            storageV1.create(V1_UTIL.newInstance(i));
        }

        for (int i = 0; i < countV3; i ++) {
            storageV3.create(V3_UTIL.newInstance(i));
        }

        for (int i = 0; i < countV4; i ++) {
            storageV4.create(V4_UTIL.newInstance(i));
        }
    }

}
