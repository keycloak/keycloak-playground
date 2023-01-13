package org.keycloak.models.map.storage.file.entity.shortcut;

import org.keycloak.models.map.storage.file.parser.ShortcutProcessor;
import org.keycloak.models.map.storage.file.YamlContext;
import org.keycloak.models.map.storage.file.AttributesLikeYamlContext;

/**
 *
 * @author hmlnarik
 */
public class FileRealmBrowserHeaders implements ShortcutProcessor {

    public static YamlContext<?> produceYamlContext() {
        return AttributesLikeYamlContext.prefixed("browserHeaders.");
    }

}
