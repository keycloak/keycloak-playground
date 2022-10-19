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

import org.keycloak.models.map.realm.MapRealmEntityFields;
import org.keycloak.models.map.storage.file.YamlContext.DefaultListContext;
import org.keycloak.models.map.storage.file.YamlContext.DefaultMapContext;
import org.keycloak.models.map.storage.file.YamlContext.DefaultObjectContext;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.models.map.storage.file.entity.FileRealmEntity;
import org.keycloak.models.map.storage.file.entity.MapFieldProperty;
import org.keycloak.models.map.storage.file.realm.RealmYamlContext;
import java.util.EnumMap;
import java.util.function.Supplier;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.constructor.SafeConstructor.ConstructYamlFloat;
import org.yaml.snakeyaml.constructor.SafeConstructor.ConstructYamlTimestamp;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 *
 * @author hmlnarik
 */
public class RealmParser {

    private static final Logger LOG = Logger.getLogger(RealmParser.class);

    private static final Map<String, MapFieldProperty> NAME_TO_FIELD = Stream.of(MapRealmEntityFields.values())
      .collect(Collectors.toMap(MapRealmEntityFields::getNameDashed, MapFieldProperty::new));

    public final Stack<String> context = new Stack<String>() {
        @Override
        public synchronized String pop() {
            final String res = super.pop();
            System.out.println(" POP: " + String.join(".", this));
            return res;
        }

        @Override
        public String push(String item) {
            final String res = super.push(item);
            System.out.println("PUSH: " + String.join(".", this));
            return res;
        }

    };

    public class MyConstructor extends Constructor {

        public MyConstructor() {
        }

        public MyConstructor(Class<? extends Object> theRoot) {
            super(theRoot);
        }
        
        @Override
        protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
            /**
             * This has been copied from parent and enhanced with context manipulation
             */
            List<NodeTuple> nodeValue = node.getValue();
            for (NodeTuple tuple : nodeValue) {
                Node keyNode = tuple.getKeyNode();
                Node valueNode = tuple.getValueNode();
                Object key = constructObject(keyNode);
                if (key != null) {
                    try {
                        key.hashCode();// check circular dependencies
                    } catch (Exception e) {
                        throw new ConstructorException("while constructing a mapping", node.getStartMark(),
                          "found unacceptable key " + key, tuple.getKeyNode().getStartMark(), e) {};
                    }
                }

                context.push(String.valueOf(key));
                try {
                    Object value = constructObject(valueNode);
                    if (keyNode.isTwoStepsConstruction()) {
                        if (loadingConfig.getAllowRecursiveKeys()) {
                            postponeMapFilling(mapping, key, value);
                        } else {
                            throw new YAMLException(
                              "Recursive key for mapping is detected but it is not configured to be allowed.");
                        }
                    } else {
                        mapping.put(key, value);
                    }
                } finally {
                    context.pop();
                }
            }
        }

        {
            this.typeDefinitions.put(FileRealmEntity.Impl.class, new TypeDescription(FileRealmEntity.Impl.class) {
                @Override
                public Object newInstance(Node node) {
                    return new FileRealmEntity.Impl();
                }

                @Override
                public Property getProperty(String name) {
                    if (NAME_TO_FIELD.containsKey(name)) {
                        return NAME_TO_FIELD.get(name);
                    }
                    return super.getProperty(name);
                }

                @Override
                public boolean setProperty(Object targetBean, String propertyName, Object value) throws Exception {
                    context.pop();
                    return super.setProperty(targetBean, propertyName, value);
                }

                @Override
                public Object newInstance(String propertyName, Node node) {
                    context.push(String.valueOf(propertyName));
                    return super.newInstance(propertyName, node);
                }

            });

            this.yamlClassConstructors.put(NodeId.mapping, new ConstructMapping() {
                @Override
                public Object construct(Node node) {
                    System.out.println("node: " + ((MappingNode) node).getValue());
                    if (node.isTwoStepsConstruction()) {
                        throw new IllegalArgumentException("Unexpected 2nd step. Node: " + node);
                    }
                    return super.construct(node);
                }

            });
            this.yamlClassConstructors.put(NodeId.scalar, new ConstructScalar() {
                @Override
                public Object construct(Node node) {
                    final ScalarNode sNode = (ScalarNode) node;
                    if (node.isTwoStepsConstruction()) {
                        throw new IllegalArgumentException("Unexpected 2nd step. Node: " + node);
                    }

                    if (sNode.getValue() == null) {
                        return null;
                    }

                    // For attributes, scalar nodes need to be returned as an array
                    if (List.class.isAssignableFrom(node.getType())) {
                        Class<? extends Object> listType = node.getType();
                        node.setType(sNode.getValue().getClass());
                        return Arrays.asList(super.construct(node));
                    }

                    return super.construct(node);
                }

            });


        }

    }

    public FileRealmEntity parse(InputStream is) {
//        Yaml yamlParser = new Yaml(new MyConstructor());
//        return yamlParser.loadAs(is, FileRealmEntity.Impl.class);

        final MyConstructor constructor = new MyConstructor();
        Composer composer = new ComposerForMapReader(new ParserImpl(new StreamReader(new UnicodeReader(is))), new Resolver(), new LoaderOptions(), context);
        constructor.setComposer(composer);
        return (FileRealmEntity) constructor.getSingleData(FileRealmEntity.Impl.class);
    }

//    public FileRealmEntity parseLowLevel(InputStream is) {
    public FileRealmEntity parseLowLevel(InputStream is) {
        return YamlContextAwareParser.parse(is, new RealmYamlContext());
    }
}
