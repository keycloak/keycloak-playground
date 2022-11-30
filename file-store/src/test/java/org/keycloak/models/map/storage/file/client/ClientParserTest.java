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
package org.keycloak.models.map.storage.file.client;

import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.storage.file.YamlContextAwareParser;
import org.keycloak.models.map.storage.file.realm.MapEntityYamlContext;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author hmlnarik
 */
public class ClientParserTest {

    @Test
    public void testEventProcessing() throws FileNotFoundException {
        String realmId = "realm1";
        String clientId = "client1";
        InputStream is = getClass().getResourceAsStream("/testdir/" + realmId + "/clients/" + clientId + ".yaml");
        MapClientEntity v = YamlContextAwareParser.parse(is, new MapEntityYamlContext<>(MapClientEntity.class));

        assertThat(v.getClientId(), is("https://localhost/client1"));

        assertThat(v.getAttributes().keySet(), containsInAnyOrder("a", "b"));
        assertThat(v.getAttribute("a"), contains("c11"));
        assertThat(v.getAttribute("b"), containsInAnyOrder("caa", "cbb", "cbb"));

        assertThat(v.getProtocolMappers(), hasSize(1));
        assertTrue(v.getProtocolMapper("name1").isPresent());
        final MapProtocolMapperEntity pm = v.getProtocolMapper("name1").get();
        assertThat(pm.getProtocolMapper(), is("pm"));
        assertThat(pm.getConfig(), hasEntry("pma", "a"));
        assertThat(pm.getConfig(), hasEntry("pmb", "b"));
        assertThat(pm.getConfig(), is(aMapWithSize(2)));

//        assertThat(v.getId(), is("client1"));

    }

}