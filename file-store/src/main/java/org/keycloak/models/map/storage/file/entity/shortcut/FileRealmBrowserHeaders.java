package org.keycloak.models.map.storage.file.entity.shortcut;

import org.keycloak.models.map.storage.file.entity.FileRealmEntity;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.keycloak.models.map.common.UndefinedValuesUtils.isUndefined;
import static org.keycloak.models.map.storage.file.entity.FileRealmEntity.BROWSER_HEADER_PREFIX;

/**
 *
 * @author hmlnarik
 */
public class FileRealmBrowserHeaders implements ShortcutProcessor<FileRealmEntity, Map<String, String>, Map.Entry<String, List<String>>> {

    @Override
    public boolean isHandledByShortcut(Map.Entry<String, List<String>> p0) {
        return p0 != null && p0.getKey() != null && p0.getKey().startsWith(BROWSER_HEADER_PREFIX);
    }

    @Override
    public Map<String, String> get(FileRealmEntity fre) {
        Map<String, List<String>> attributes = fre.getAttributes();
        return attributes == null ? null : attributes.entrySet().stream()
          .filter(me -> ! isUndefined(me.getValue()))
          .filter(this::isHandledByShortcut)
          .collect(Collectors.toUnmodifiableMap(
            me -> me.getKey().substring(BROWSER_HEADER_PREFIX.length()),
            me -> me.getValue().get(0))
          )
        ;
    }

    @Override
    public void set(FileRealmEntity fre, Map<String, String> browserHeaders) {
        if (browserHeaders != null) {
            browserHeaders.forEach((k, v) -> fre.setAttribute(BROWSER_HEADER_PREFIX + k, Collections.singletonList(v)));
        }
    }

}
