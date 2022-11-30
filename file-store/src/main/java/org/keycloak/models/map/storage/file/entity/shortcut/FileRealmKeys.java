package org.keycloak.models.map.storage.file.entity.shortcut;

import org.keycloak.models.map.storage.file.parser.ShortcutProcessor;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.realm.entity.MapComponentEntityImpl;
import org.keycloak.models.map.storage.file.YamlContext;
import org.keycloak.models.map.storage.file.YamlContext.DefaultListContext;
import org.keycloak.models.map.storage.file.realm.ComponentsYamlContext;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author hmlnarik
 */
public class FileRealmKeys implements ShortcutProcessor {

    public static class KeysYamlContext extends DefaultListContext {

        @Override
        public YamlContext<?> getContext(String nameOfSubcontext) {
            return new KeyYamlContext() {
                @Override
                protected MapComponentEntity createComponent() {
                    MapComponentEntity res = createKeyComponent();
                    res.setProviderId(nameOfSubcontext);
                    return res;
                }
            };
        }

        @Override
        public void add(String name, Object value) {    // e.g. add("sig", list-of-sig-key-components);
            if (value instanceof List) {
                List<MapComponentEntity> lValue = (List<MapComponentEntity>) value;
                lValue.forEach(this::add);
            }
        }

        private static MapComponentEntity createKeyComponent() {
            MapComponentEntity res = new MapComponentEntityImpl();
            res.setProviderType(KeyProvider.class.getName());
            res.setConfig(new HashMap<>());
            return res;
        }
    }

    private static class KeyYamlContext extends ComponentsYamlContext {

        @Override
        public void add(Object value) {
            add("key@" + getResult().size(), value);
        }

    }

}
