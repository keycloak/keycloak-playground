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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityImpl;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntityImpl;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.group.MapGroupEntityImpl;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.storage.file.YamlContext;
import org.keycloak.models.map.storage.file.client.ClientYamlContext;
import org.keycloak.models.map.storage.file.writer.WritingMechanism;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Present;
import org.snakeyaml.engine.v2.events.DocumentEndEvent;
import org.snakeyaml.engine.v2.events.DocumentStartEvent;
import org.snakeyaml.engine.v2.events.Event;
import org.snakeyaml.engine.v2.events.StreamEndEvent;
import org.snakeyaml.engine.v2.events.StreamStartEvent;

public class RealmWriterTest {

    private void writeEventsTofile(List<Event> events) throws RuntimeException, IOException {
        DumpSettings settings = DumpSettings.builder().build();
        Present present = new Present(settings);
        writeTofile(present.emitToString(events.iterator()));
    }

    private void writeTofile(String str) throws RuntimeException, IOException {
        File file = new File("target/test.yaml");
        file.delete();
        if (file.createNewFile()) {
            try (FileWriter writer = new FileWriter(file);) {
                writer.write(str);
            }
        } else {
            throw new RuntimeException("File already existed!");
        }
    }

    @Test
    public void testWriteRealm() throws Exception {
        String realmId = "realm1";

        MapRealmEntity realm = DeepCloner.DUMB_CLONER.newInstance(MapRealmEntity.class);
        realm.setId(realmId);
        realm.setName("name");
        realm.setEnabled(true);
        realm.setDisplayName("displayName");
        realm.setBrowserSecurityHeader("browserHeaders.X-Debug", "1");
        realm.setBrowserSecurityHeader("browserHeaders.X-Keycloak", "19.0,3");

        realm.setAttribute("a", List.of("11"));
        realm.setAttribute("b", List.of("aa", "bb"));

        List<Event> events = new LinkedList<>();
        addStartEvents(events);

        addEntityWithContext(events, realm, new RealmYamlContext());

        addEndEvents(events);

        writeEventsTofile(events);
    }

    private <E> void addEntityWithContext(List<Event> events, E entity, YamlContext<E> initialContext) {
        initialContext.writeValue(entity, new WritingMechanism(events));
    }

    private void addStartEvents(List<Event> events) {
        events.add(new StreamStartEvent());
        events.add(new DocumentStartEvent(false, Optional.empty(), new HashMap<>()));
    }

    private void addEndEvents(List<Event> events) {
        events.add(new DocumentEndEvent(false));
        events.add(new StreamEndEvent());
    }
}
