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

import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.realm.entity.MapComponentEntityImpl;
import org.keycloak.models.map.storage.file.YamlContext.DefaultListContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <table>
 * <tr><td>Result:</td>                         <td>{@code List<MapComponentEntity>}</td></tr>
 * <tr><td>Expected value format in YAML:</td>  <td>map</td></tr>
 * <tr><td>Usual path to yaml context:</td>     <td>{@code /components}</td></tr>
 * <tr>
 *  <td>Example value</td>
 *  <td>
 *   <pre>
 *     c1:
 *       providerId: xyz
 *       configAa: za
 *   </pre>
 * </td>
 * </tr>
 * </table>
 * @author hmlnarik
 */
public class ComponentsYamlContext extends DefaultListContext {

    /**
     * Adds a new component whose representation in YAML is a map where
     * ID is the map {@code key} and {@code value} contains component description.
     * <p>
     *
     * Note however that {@link MapRealmEntity#getComponents()} is a <i>collection</i>, not a map.
     * <p>
     * For that reason, this context class is a <i>collection</i> context
     * (see the ancestor: {@link DefaultListContext}).
     * 
     * The translation from map entry into list elements happens at the end of this method,
     * where {@link #add(Object)} is used to record new {@link MapComponentEntity} object.
     */

    @Override
    public void add(String name, Object value) {
        if (value instanceof List) {
            List<?> lValue = (List<?>) value;
            lValue.forEach(this::add);
        }
        if (! (value instanceof Map)) {
            throw new IllegalStateException("Invalid format of " + "components" + " element");
        }
        Map<String, Object> mValue = (Map<String, Object>) value;

        MapComponentEntity res = createComponent();

        Map<String, List<String>> config = new HashMap<>();
        res.setId(name);
        res.setName(name);
        mValue.forEach((key, configValue) -> {
            switch (key) {
                case "providerId":
                    res.setProviderId(String.valueOf(configValue));
                    break;
                case "name":
                    res.setName(String.valueOf(configValue));
                    break;
                default:
                    List<String> v;
                    if (! (configValue instanceof List)) {
                        v = Arrays.asList(String.valueOf(configValue));
                    } else {
                        v = (List<String>) ((List) configValue).stream().map(String::valueOf).collect(Collectors.toList());
                    }
                    config.put(key, v);
            }
        });

        if (! config.isEmpty()) {
            res.setConfig(config);
        }

        // Note the translation from map to list here: Instead of using add(key, value), add(object) is used
        // Reason is that file stores the components as a map where the key is ID (and potentially name)
        // while the internal representation is a List of MapComponentEntity objects.

        super.add(res);
    }

    protected MapComponentEntity createComponent() {
        MapComponentEntity res = new MapComponentEntityImpl();
        return res;
    }

}
