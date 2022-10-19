package org.keycloak.models.map.storage.file.entity.shortcut;

import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.storage.file.entity.FileRealmEntity;
import java.util.List;

/**
 *
 * @author hmlnarik
 */
public class FileRealmKeys implements ShortcutProcessor<FileRealmEntity, List<FileRealmKey>, MapComponentEntity> {

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
