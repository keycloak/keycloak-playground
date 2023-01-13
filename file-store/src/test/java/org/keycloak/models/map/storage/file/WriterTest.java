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
package org.keycloak.models.map.storage.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.group.MapGroupEntityFields;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.storage.file.client.ClientYamlContext;
import org.keycloak.models.map.storage.file.realm.RealmYamlContext;
import org.keycloak.models.map.storage.file.writer.WritingMechanism;
import org.keycloak.models.map.storage.file.writer.WritingMechanismImpl;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Present;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.events.DocumentEndEvent;
import org.snakeyaml.engine.v2.events.DocumentStartEvent;
import org.snakeyaml.engine.v2.events.Event;
import org.snakeyaml.engine.v2.events.ImplicitTuple;
import org.snakeyaml.engine.v2.events.MappingEndEvent;
import org.snakeyaml.engine.v2.events.MappingStartEvent;
import org.snakeyaml.engine.v2.events.ScalarEvent;
import org.snakeyaml.engine.v2.events.SequenceEndEvent;
import org.snakeyaml.engine.v2.events.SequenceStartEvent;
import org.snakeyaml.engine.v2.events.StreamEndEvent;
import org.snakeyaml.engine.v2.events.StreamStartEvent;

public class WriterTest {

    final ImplicitTuple implicitTuple = new ImplicitTuple(true, true);

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

//    @Test
    public void testDummyWriting() throws IOException {

        List<Event> events = List.of(
            new StreamStartEvent(),
            new DocumentStartEvent(false, Optional.empty(), new HashMap<>()),

            new MappingStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK),
                new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "key", ScalarStyle.PLAIN),
                new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "value", ScalarStyle.PLAIN),
                new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "key1", ScalarStyle.PLAIN),
                new MappingStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK),
                    new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "key2", ScalarStyle.PLAIN),
                    new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "value2", ScalarStyle.PLAIN),
                    new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "key3", ScalarStyle.PLAIN),
                    new SequenceStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK),
                        new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "seq1", ScalarStyle.PLAIN),
                        new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "seq2", ScalarStyle.PLAIN),
                        new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "seq3", ScalarStyle.PLAIN),
                    new SequenceEndEvent(),
                new MappingEndEvent(),
            new MappingEndEvent(),

            new DocumentEndEvent(false), 
            new StreamEndEvent()
        );

        writeEventsTofile(events);
    }

//    @Test
    public void testDummyWriteGroup() throws Exception {
        String realmId = "realm1";

        MapGroupEntity parentGroup = DeepCloner.DUMB_CLONER.newInstance(MapGroupEntity.class);

        parentGroup.setId("id1");
        parentGroup.setName("parent group");
        parentGroup.setRealmId(realmId);
        parentGroup.setGrantedRoles(Set.of("role1", "role2", "role3"));
        parentGroup.setAttribute("a0", List.of("v0"));
        parentGroup.setAttribute("a1", List.of("v1, v2"));
        parentGroup.setAttribute("a2", List.of("v3, v3, v4"));

        List<Event> events = new LinkedList<>();

        events.add(new StreamStartEvent());
        events.add(new DocumentStartEvent(false, Optional.empty(), new HashMap<>()));

        //schemaVersion
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "schemaVersion", ScalarStyle.PLAIN));
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, "1", ScalarStyle.PLAIN));

        //name
        String nameKey = MapGroupEntityFields.NAME.getNameCamelCase();
        String nameValue = MapGroupEntityFields.NAME.get(parentGroup).toString();
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, nameKey, ScalarStyle.PLAIN));
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, nameValue, ScalarStyle.PLAIN));

        //attributes
        String attributesKey = MapGroupEntityFields.ATTRIBUTES.getNameCamelCase();
        Map<String, List<String>> attributes = (Map) MapGroupEntityFields.ATTRIBUTES.get(parentGroup);
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, attributesKey, ScalarStyle.PLAIN));
        events.add(new MappingStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK));
        for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
            events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, entry.getKey(), ScalarStyle.PLAIN)); //scalar - attrKey
            events.add(new SequenceStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK)); //sequence - attrValue - TODO for single value attributes - use scalar
            for (String attrValue : entry.getValue()) {
                events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, attrValue, ScalarStyle.PLAIN));
            }
            events.add(new SequenceEndEvent());
        }
        events.add(new MappingEndEvent());

        //parentId
        if (MapGroupEntityFields.PARENT_ID.get(parentGroup) != null) {
            String parentIdKey = MapGroupEntityFields.PARENT_ID.getNameCamelCase();
            String parentIdValue = MapGroupEntityFields.PARENT_ID.get(parentGroup).toString();

            events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, parentIdKey, ScalarStyle.PLAIN));
            events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, parentIdValue, ScalarStyle.PLAIN));
        }

        events.add(new DocumentEndEvent(false));
        events.add(new StreamEndEvent());

        writeEventsTofile(events);
    }

    @Test
    public void testALittleBitLessDummyWriteGroup() throws Exception {
        String realmId = "realm1";

        MapGroupEntity parentGroup = DeepCloner.DUMB_CLONER.newInstance(MapGroupEntity.class);

        parentGroup.setId("id1");
        parentGroup.setName("parent group");
        parentGroup.setRealmId(realmId);
        parentGroup.setGrantedRoles(Set.of("role1", "role2", "role3"));
        parentGroup.setAttribute("a0", List.of("v0"));
        parentGroup.setAttribute("a1", List.of("v1", "v2"));
        parentGroup.setAttribute("a2", List.of("v3", "v3", "v4"));

        List<Event> events = addEntityWithContext(parentGroup, new MapEntityYamlContext<>(MapGroupEntity.class));

        writeEventsTofile(events);
    }

    @Test
    public void testALittleBitLessDummyWriteClient() throws Exception {
        String realmId = "realm1";

        MapProtocolMapperEntity pm0 = DeepCloner.DUMB_CLONER.newInstance(MapProtocolMapperEntity.class);
        pm0.setName("name0");
        pm0.setProtocolMapper("pm0");
        Map<String, String> config = new HashMap<>();
        config.put("pma", "a");
        config.put("pmb", null); // this shouldn't be written
        pm0.setConfig(config);

        MapProtocolMapperEntity pm1 = DeepCloner.DUMB_CLONER.newInstance(MapProtocolMapperEntity.class);
        pm1.setName("name1");
        pm1.setProtocolMapper("pm0");
        pm1.setConfig(Map.of("pma", "a", "pmb", "b"));

        MapClientEntity client = DeepCloner.DUMB_CLONER.newInstance(MapClientEntity.class);
        client.setId("id1");
        client.setClientId("client1");
        client.setRealmId(realmId);
        client.setEnabled(true);
        client.addRedirectUri("redirect_uri1");
        client.addRedirectUri("redirect_uri2");
        client.addProtocolMapper(pm0);
        client.addProtocolMapper(pm1);
        client.setAttribute("a0", List.of()); // this shouldn't be written
        client.setAttribute("a1", List.of("v0"));
        client.setAttribute("a2", List.of("v1", "v2"));
        client.setAttribute("a3", List.of("v3", "v3", "v4"));


        List<Event> events = addEntityWithContext(client, new ClientYamlContext());

        writeEventsTofile(events);
    }

    @Test
    public void testALittleBitLessDummyWriteRealm() throws Exception {
        String realmId = "realm1";

        MapRealmEntity realm = DeepCloner.DUMB_CLONER.newInstance(MapRealmEntity.class);
        realm.setId("id1");
        realm.setName(realmId);
        realm.setEnabled(true);
        realm.setAttribute("a0", List.of()); // this shouldn't be written
        realm.setAttribute("a1", List.of("v0"));
        realm.setAttribute("defaultSignatureAlgorithm", List.of("v1"));
//        realm.setAttribute("actionTokenGeneratedByUserLifespan", List.of("v2"));//todo
        realm.setAttribute("actionTokenGeneratedByUserLifespan.type1", List.of("v3"));
        realm.setAttribute("actionTokenGeneratedByUserLifespan.type2", List.of("v4"));

        List<Event> events = addEntityWithContext(realm, new RealmYamlContext());

        writeEventsTofile(events);
    }

    private <E> List<Event> addEntityWithContext(E entity, YamlContext<E> initialContext) {
        WritingMechanism mech = new WritingMechanismImpl();
        initialContext.writeValue(entity, mech, () -> {
            mech.startStream();
            mech.startDocument();
        });
        mech.endDocument();
        mech.endStream();
        return mech.getEvents();
    }

}
