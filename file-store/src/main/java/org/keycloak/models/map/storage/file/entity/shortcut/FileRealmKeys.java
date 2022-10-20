package org.keycloak.models.map.storage.file.entity.shortcut;

import org.keycloak.keys.KeyProvider;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.realm.entity.MapComponentEntityImpl;
import org.keycloak.models.map.storage.file.YamlContext;
import org.keycloak.models.map.storage.file.YamlContext.DefaultListContext;
import org.keycloak.models.map.storage.file.entity.FileRealmEntity;
import org.keycloak.models.map.storage.file.realm.ComponentsYamlContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 *
 * @author hmlnarik
 */
public class FileRealmKeys implements ShortcutProcessor<FileRealmEntity, List<FileRealmKey>, MapComponentEntity> {

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
            add("@" + getResult().size(), value);
        }
    }

    @Override
    public boolean isHandledByShortcut(MapComponentEntity p0) {
        return p0 == null || ! "org.keycloak.keys.KeyProvider".equals(p0.getProviderType());
    }

    @Override
    public List<FileRealmKey> get(FileRealmEntity fre) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void set(FileRealmEntity fre, List<FileRealmKey> keys) {
        if (keys == null) {
            return;
        }
    }

}
