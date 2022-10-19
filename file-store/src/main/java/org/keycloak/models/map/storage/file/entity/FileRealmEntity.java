package org.keycloak.models.map.storage.file.entity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.MapRealmEntityImpl;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.storage.file.annotations.GenerateJsonSchema;
import org.keycloak.models.map.storage.file.annotations.Shortcut;
import org.keycloak.models.map.storage.file.entity.shortcut.FileRealmBrowserHeaders;
import org.keycloak.models.map.storage.file.entity.shortcut.FileRealmKeys;
import java.util.Map.Entry;

@GenerateJsonSchema(
    version=1,
    file="file/v1/schema-realm.yaml",
    id="https://keycloak.org/schema/v1/file/realm",
    title="Keycloak Realm Schema"
)
public interface FileRealmEntity extends MapRealmEntity {

    public static final String BROWSER_HEADER_PREFIX = "_browser_header.";

    @Override
    // @DefaultFrom("id")
    String getName();
    
    @Override
    String getId();

    @Override
    @Shortcut(name="displayName", key="displayName")
    @Shortcut(name="browserHeaders", processor=FileRealmBrowserHeaders.class)
    Map<String, List<String>> getAttributes();

    @Override
    @Shortcut(name="keys", processor=FileRealmKeys.class)
    public Set<MapComponentEntity> getComponents();

    public class Impl extends MapRealmEntityImpl implements FileRealmEntity {

        private static final FileRealmKeys FILE_REALM_KEYS = new FileRealmKeys();
        private static final Predicate<MapComponentEntity> FILEREALMKEYS_NEG_PRED = Predicate.not(FILE_REALM_KEYS::isHandledByShortcut);

        public Integer version;

        @SuppressWarnings("unchecked")
        public java.util.Set<org.keycloak.models.map.realm.entity.MapComponentEntity> getComponentsForSerialization() {
            final Set<MapComponentEntity> components = super.getComponents();
            return components == null ? null : components.stream()
              .filter(FILEREALMKEYS_NEG_PRED)
              .collect(Collectors.toSet());
        }

        private static final FileRealmBrowserHeaders FILE_REALM_BROWSER_HEADERS = new FileRealmBrowserHeaders();
        private static final Predicate<Entry<String, List<String>>> FILEREALMBROWSERHEADERS_NEG_PRED = Predicate.not(FILE_REALM_BROWSER_HEADERS::isHandledByShortcut);

        public Map<String, List<String>> getAttributesForSerialization() {
            Map<String, List<String>> attributes = super.getAttributes();
            return attributes == null ? null : attributes.entrySet().stream()
              .filter(FILEREALMBROWSERHEADERS_NEG_PRED)
              .filter(me -> ! "displayName".equals(me.getKey()))
              .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        }

    }
}
