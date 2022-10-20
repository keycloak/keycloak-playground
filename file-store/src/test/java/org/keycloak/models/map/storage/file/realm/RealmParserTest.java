/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.file.realm;

import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;
import org.keycloak.models.map.storage.file.entity.FileRealmEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

/**
 *
 * @author hmlnarik
 */
public class RealmParserTest {

    @Test
    public void testEventProcessing() throws FileNotFoundException {
        RealmParser p = new RealmParser();
        FileRealmEntity v = p.parse(getClass().getResourceAsStream("/testdir/realm1/realm.yaml"));

        assertThat(v.getAttributes().keySet(), containsInAnyOrder("displayName", "a", "b", "browserHeaders.X-Debug", "browserHeaders.X-Keycloak"));
        assertThat(v.getAttribute("displayName"), contains("This is a display name"));
        assertThat(v.getAttribute("a"), contains("11"));
        assertThat(v.getAttribute("b"), contains("aa", "bb"));
        assertThat(v.getAttribute("browserHeaders.X-Debug"), contains("1"));
        assertThat(v.getAttribute("browserHeaders.X-Keycloak"), contains("19.0.3"));

        assertThat(v.getComponents(), hasSize(3));
    }

}