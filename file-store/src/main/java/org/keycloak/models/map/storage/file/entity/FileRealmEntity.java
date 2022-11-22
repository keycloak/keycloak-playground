package org.keycloak.models.map.storage.file.entity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.storage.file.annotations.GenerateJsonSchema;
import org.keycloak.models.map.storage.file.annotations.Shortcut;
import org.keycloak.models.map.storage.file.annotations.YamlContextSupplier;
import org.keycloak.models.map.storage.file.entity.shortcut.FileRealmBrowserHeaders;
import org.keycloak.models.map.storage.file.entity.shortcut.FileRealmKeys;
import org.keycloak.models.map.storage.file.realm.AttributesLikeYamlContext;
import org.keycloak.models.map.storage.file.realm.ComponentsYamlContext;

@GenerateJsonSchema(
    version=1,
    file="file/v1/schema-realm.yaml",
    id="https://keycloak.org/schema/v1/file/realm",
    title="Keycloak Realm Schema"
)
public interface FileRealmEntity extends MapRealmEntity {

    @Override
    // @DefaultFrom("id")
    String getName();
    
    @Override
    String getId();

    @Override
    @Shortcut(name="displayName", key="displayName")
    @Shortcut(name="browserHeaders", processor=FileRealmBrowserHeaders.class)
    @YamlContextSupplier(AttributesLikeYamlContext.class)
    Map<String, List<String>> getAttributes();

    @Override
    @Shortcut(name="keys", processor=FileRealmKeys.class)
    @YamlContextSupplier(ComponentsYamlContext.class)
    public Set<MapComponentEntity> getComponents();
}
