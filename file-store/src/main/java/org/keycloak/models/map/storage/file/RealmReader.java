package org.keycloak.models.map.storage.file;

import java.io.StringReader;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author hmlnarik
 */
public class RealmReader {

    private static final String REALM_YAML = 
    "version: 1\n" +
    "name: name\n" +
    "attributes:\n" +
    "  a: '11'\n" +
    "  b: \n" +
    "    - aa\n" +
    "    - bb\n"
    ;

    public static void main(String[] args) {
      Yaml yaml = new Yaml();

      yaml.load(new StringReader(REALM_YAML));
    }
}
