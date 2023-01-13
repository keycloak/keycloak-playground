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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.keycloak.models.map.storage.file.AttributesLikeYamlContext;
import org.keycloak.models.map.storage.file.writer.WritingMechanism;

public class RealmAttributesYamlContext extends AttributesLikeYamlContext {

    private final Map<String, Supplier<? extends DefaultMapContext>> attributeContextCreators;

    public RealmAttributesYamlContext(Map<String, Supplier<? extends DefaultMapContext>> attributeContextCreators) {
        this.attributeContextCreators = attributeContextCreators;
    }

    @Override
    public void writeValue(Map<String, Object> value, WritingMechanism mech, Runnable addKeyEvent) {
        //this for-each handles all singleton and prefixed attributes
        for (Map.Entry<String, Supplier<? extends DefaultMapContext>> entry : new TreeMap<>(attributeContextCreators).entrySet()) {
            DefaultMapContext context = entry.getValue().get();
            if (context instanceof AttributesLikeYamlContext.SingletonAttributesMapYamlContext) {
                mech.addScalar(entry.getKey());
                mech.addScalar(((List)value.get(entry.getKey())).get(0));
                value.remove(entry.getKey()); // remove already written attribute from attributes
            } else if (context instanceof AttributesLikeYamlContext.Prefixed) {
                AttributesLikeYamlContext.Prefixed prefixed = (AttributesLikeYamlContext.Prefixed) context;
                ((AttributesLikeYamlContext.Prefixed) context).writeValue(getAllAttributesWithPrefix(prefixed.getPrefix(), value), mech, () -> {
                    mech.addScalar(prefixed.getPrefix().substring(0, prefixed.getPrefix().length() - 1));
                });
            }
        }

        // write the rest of attributes
        super.writeValue(value, mech, () -> {
            mech.addScalar("attributes");
        });
    }

    /**
     * Extracts and removes all attributes from {@code attributes} with the given {@code prefix}. 
     *
     * @param prefix
     * @param attributes
     * @return All attributes with the given prefix. Prefix is trimmed of from attribute keys.
     */
    private Map<String, Object> getAllAttributesWithPrefix(String prefix, Map<String, Object> attributes) {
        List<String> toRemove = new LinkedList<>();
        Map<String, Object> attributesWithPrefix = new TreeMap<>(attributes).entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(entry -> { 
                    toRemove.add(entry.getKey());
                    return entry;
                }).collect(Collectors.toMap(
                        entry -> entry.getKey().substring(prefix.length()),
                        entry -> entry.getValue()
                ));
        toRemove.stream().forEach(key -> attributes.remove(key));
        return attributesWithPrefix;
    }
}
