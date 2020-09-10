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

import org.keycloak.playground.nodowntimeupgrade.VersionUtil.Constants;
import org.keycloak.playground.nodowntimeupgrade.base.model.HasId;
import org.keycloak.playground.nodowntimeupgrade.base.model.ModelVersion;
import org.keycloak.playground.nodowntimeupgrade.base.model.ObjectModel_V4;
import org.keycloak.playground.nodowntimeupgrade.base.storage.Storage;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V1.V1_UTIL;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V3.V3_UTIL;
import static org.keycloak.playground.nodowntimeupgrade.VersionUtil_V4.V4_UTIL;

/**
 * Test for validation of compatibility between the versions.
 * It is expected that:
 * <ul>
 * <li>Any two adjacent versions can read and write each other.</li>
 * <li>Any record with version newer than the next version (i.e. version difference is at least 2)
 *     is reported and exception is thrown.</li>
 * <li>Any record older than the current version is still legible.</li>
 * </ul>
 *
 * @author hmlnarik
 */
public class CompatibilityTest extends AbstractNoDowntimeUpgradeTest {

    private static final int INITIAL_COUNT_V1 = 100;
    private static final int INITIAL_COUNT_V3 = 400;
    private static final int INITIAL_COUNT_V4 = 200;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void init() {
        createInstances(INITIAL_COUNT_V1, INITIAL_COUNT_V3, INITIAL_COUNT_V4);
    }

    private static <T extends HasId<String>> void testReadFull(Storage<T> storage, VersionUtil<T> versionUtil) {
        for (String key : storage.keys()) {
            T om = storage.read(key);
            try {
                versionUtil.assertValid(om);
            } catch (AssertionError e) {
                throw enhancedExceptionReport(e, storage, om);
            }
        }
    }

    /**
     * <ul>
     * <li><b>Scenario:</b> Read all records as if read by version 1 reader.</li>
     * <li><b>Expected result:</b> Failure received as some sort of exception, the records are incompatible.</li>
     * </ul>
     */
    @Test
    public void testReadFullStoreWithV1() {
        expectedException.expect(CoreMatchers.isA(Exception.class));
        testReadFull(storageV1, V1_UTIL);
    }

    /**
     * <ul>
     * <li><b>Scenario:</b> Read all records as if read by version 3 reader.</li>
     * <li><b>Expected result:</b> All records are readable.</li>
     * </ul>
     */
    @Test
    public void testReadFullStoreWithV3() {
        testReadFull(storageV3, V3_UTIL);
    }

    /**
     * <ul>
     * <li><b>Scenario:</b> Read all records as if read by version 4 reader.</li>
     * <li><b>Expected result:</b> All records are readable.</li>
     * </ul>
     */
    @Test
    public void testReadFullStoreWithV4() {
        testReadFull(storageV4, V4_UTIL);
    }

    /**
     * <ol>
     * <li>Read a version 1 object via version 1 storage.</li>
     * <li>Read the same object via version 4 storage, and write it back.
     *     This should migrate the object to version 4.</li>
     * <li>Read the same object via version 1 storage. This should throw an exception
     *     since the object would have been migrated to version 4 object in the step
     *     above and cannot be thus read with the old store.</li>
     * </ol>
     */
    @Test
    public void testReadViaOldVersionAfterUpgradeFails() {
        // Find one version 1 entity
        String key = storageV1.keys().stream()
          .filter(i -> (Integer.valueOf(i) / Constants.INITIAL_INDEX_FACTOR) == ModelVersion.VERSION_1.getVersion())
          .findAny()
          .orElseThrow(() -> new IllegalStateException("Storage should contain at least one version 1 record."));

        storageV1.read(key);    // This should pass

        ObjectModel_V4 om4 = storageV4.read(key);
        V4_UTIL.assertValid(om4);

        storageV4.write(om4);  // Update the record

        V4_UTIL.assertValid(storageV4.read(key));   // Read and check the updated record

        expectedException.expect(CoreMatchers.isA(Exception.class));
        storageV1.read(key);    // This should fail now, the record would be updated to incompatible V4
    }

}
