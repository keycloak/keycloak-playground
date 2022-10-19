package org.keycloak.models.map.storage.file.annotations;

/**
 *
 * @author hmlnarik
 */
public @interface GenerateJsonSchema {

    public int version();

    public String file();

    public String id();

    public String title();

}
