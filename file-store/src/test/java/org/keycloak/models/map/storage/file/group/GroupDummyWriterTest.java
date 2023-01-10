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
package org.keycloak.models.map.storage.file.group;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityImpl;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntityImpl;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.AutogeneratedClasses;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.group.MapGroupEntityFields;
import org.keycloak.models.map.group.MapGroupEntityImpl;
import org.keycloak.models.map.storage.file.YamlContext;
import org.keycloak.models.map.storage.file.client.ClientYamlContext;
import org.keycloak.models.map.storage.file.MapEntityYamlContext;
import org.keycloak.models.map.storage.file.writer.WritingMechanism;
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

public class GroupDummyWriterTest {

    final ImplicitTuple implicitTuple = new ImplicitTuple(true, true);

    //TEMP
    public static final java.util.Map<Class<?>, Class<?>> IMPL_TO_INTERFACE = Map.of(
            MapClientEntityImpl.class, MapClientEntity.class, 
            MapProtocolMapperEntityImpl.class, MapProtocolMapperEntity.class, 
            MapGroupEntityImpl.class, MapGroupEntity.class
    );
    public static final java.util.Map<Class<?>, YamlContext<?>> ENTITY_TO_CONTEXT = Map.of(
            MapClientEntity.class, new ClientYamlContext()
    );

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

    @Test
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
        addStartEvents(events);

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

        addEndEvents(events);

        writeEventsTofile(events);
    }

    @Test
    public void testALittleBitLessDummyWriteGroup() throws Exception {
        String realmId = "realm1";

        MapGroupEntity parentGroup = DeepCloner.DUMB_CLONER.newInstance(MapGroupEntity.class);
        MapGroupEntity childGroup = DeepCloner.DUMB_CLONER.newInstance(MapGroupEntity.class);

        parentGroup.setId("id1");
        parentGroup.setName("parent group");
        parentGroup.setRealmId(realmId);
        parentGroup.setGrantedRoles(Set.of("role1", "role2", "role3"));
        parentGroup.setAttribute("a0", List.of("v0"));
        parentGroup.setAttribute("a1", List.of("v1", "v2"));
        parentGroup.setAttribute("a2", List.of("v3", "v3", "v4"));

//        childGroup.setId("id2");
//        childGroup.setName("child group");
//        childGroup.setRealmId(realmId);
//        childGroup.setParentId(parentGroup.getId());
//        childGroup.setGrantedRoles(Set.of("role4"));
        
        List<Event> events = new LinkedList<>();
        addStartEvents(events);

        addEntityWithContext(events, parentGroup, new MapEntityYamlContext<>(MapGroupEntity.class));

        addEndEvents(events);

        writeEventsTofile(events);
    }

    @Test
    public void testALittleBitLessDummyWriteClient() throws Exception {
        String realmId = "realm1";

        MapProtocolMapperEntity pm = DeepCloner.DUMB_CLONER.newInstance(MapProtocolMapperEntity.class);
        pm.setName("pmName");
        pm.setProtocolMapper("mapper");
        pm.setConfig(Map.of("key1", "value1", "key2", "value2"));

        MapClientEntity client = DeepCloner.DUMB_CLONER.newInstance(MapClientEntity.class);
//        client.setId("id1");
        client.setClientId("client1");
        client.setRealmId(realmId);
        client.setEnabled(true);
        client.addRedirectUri("redirect_uri1");
        client.addRedirectUri("redirect_uri2");
        client.addProtocolMapper(pm);
        client.setAttribute("a0", List.of("v0"));
        client.setAttribute("a1", List.of("v1", "v2"));
        client.setAttribute("a2", List.of("v3", "v3", "v4"));

        List<Event> events = new LinkedList<>();
        addStartEvents(events);

        addEntityWithContext(events, client, new ClientYamlContext());

        addEndEvents(events);

        writeEventsTofile(events);
    }

    private <E> void addEntityWithContext(List<Event> events, E entity, YamlContext<E> initialContext) {
        initialContext.writeValue(entity, new WritingMechanism(events));
    }

    private <E> void addEntity(List<Event> events, E entity) {
        EntityField<E>[] fields = (EntityField<E>[]) AutogeneratedClasses.ENTITY_FIELDS.get(IMPL_TO_INTERFACE.get(entity.getClass()));

        for (EntityField<E> field : fields) {

            System.out.println("  adding field " + field.getName());
            // do not write ID or REALM_ID into the file
            // ID will be recorded in name of the file
            // REALM_ID will be recorded by directory name
            if (!field.getNameCamelCase().equals("id") && !field.getNameCamelCase().equals("realmId")) {

                Object fieldVal = field.get(entity);
                String fieldName = field.getNameCamelCase();

                if (fieldVal != null) { // field has some non-null value ... do not store anything with null value
                    // following should be pair of scalars
                    if (field.getFieldClass().equals(String.class) || field.getFieldClass().equals(Boolean.class)) { 
                        addPairOfScalars(events, fieldName, fieldVal.toString());
                    } else if (field.getFieldClass().equals(Set.class)) { // REDIRECT_URIS
                        
                        if (field.getCollectionElementClass().equals(String.class)) {
                            Set<String> values = (Set) field.get(entity); // we know it's Set<String>

                            if (values.size() == 1) {
                                addPairOfScalars(events, fieldName, values.iterator().next());
                            } else {
                                addSequence(events, fieldName, values);
                            }
                        } else if (AbstractEntity.class.isAssignableFrom(field.getCollectionElementClass())) { //set of entities
                            events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, fieldName, ScalarStyle.PLAIN));
                            ((Set) field.get(entity)).forEach(abstractEntity -> addEntity(events, abstractEntity));
                        } else {
                            throw new UnsupportedOperationException("not supported - " + field.getCollectionElementClass().getSimpleName());
                        }

                    } else if (field.getFieldClass().equals(Map.class)) {
                        if (field.getMapKeyClass().equals(String.class)) {
                            addMap(events, field, (Map) fieldVal);
                        } else {
                            throw new UnsupportedOperationException("not supported - " + field.getMapKeyClass().getSimpleName()
                                + ", " + field.getMapValueClass().getSimpleName());
                        }
                    }
                }
            }
        }
    }

    private void addStartEvents(List<Event> events) {
        events.add(new StreamStartEvent());
        events.add(new DocumentStartEvent(false, Optional.empty(), new HashMap<>()));
    }

    private void addPairOfScalars(List<Event> events, String key, String value) {
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, key, ScalarStyle.PLAIN));
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, value, ScalarStyle.PLAIN));
    }

    private void addSequence(List<Event> events, String keyName, Collection<String> values) {
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, keyName, ScalarStyle.PLAIN));
        events.add(new SequenceStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK));
        for (String value : values) {
            events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, value, ScalarStyle.PLAIN));
        }
        events.add(new SequenceEndEvent());
    }

    private void addMap(List<Event> events, EntityField field, Map<?, ?> map) {
        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, field.getNameCamelCase(), ScalarStyle.PLAIN));
        events.add(new MappingStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK));
        if (field.getMapValueClass().equals(String.class)) {
            for (Map.Entry<?, ?> o : map.entrySet()) {
                addPairOfScalars(events, o.getKey().toString(), o.getValue().toString());
            }
        } else if (field.getMapValueClass().equals(List.class)) {
            for (Map.Entry<String, List<String>> entry : ((Map<String, List<String>>) map).entrySet()) {
                if (entry.getValue().size() == 1) {
                    addPairOfScalars(events, entry.getKey(), entry.getValue().get(0));
                } else {
                    addSequence(events, entry.getKey(), entry.getValue());
                }
            }
        }
        
        events.add(new MappingEndEvent());
    }

    private void addAttributes(List<Event> events, Map<String, List<String>> attributes) {
        String attributesKey = "attributes";

        events.add(new ScalarEvent(Optional.empty(), Optional.empty(), implicitTuple, attributesKey, ScalarStyle.PLAIN));
        events.add(new MappingStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.BLOCK));
        for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
            if (entry.getValue().size() == 1) {
                addPairOfScalars(events, entry.getKey(), entry.getValue().get(0));
            } else {
                addSequence(events, entry.getKey(), entry.getValue());
            }
        }
        events.add(new MappingEndEvent());
    }
    
    private void addEndEvents(List<Event> events) {
        events.add(new DocumentEndEvent(false));
        events.add(new StreamEndEvent());
    }
}
