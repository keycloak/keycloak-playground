package org.keycloak.models.map.storage.file.entity;

import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.MapRealmEntityImpl;
import org.keycloak.models.map.storage.file.annotations.IdSubstituteWhenIdMissing;

@GenerateJsonSchema("realm.yaml")
public interface FileRealmEntity extends MapRealmEntity {

    @Override
    @Required
    String getName();
    
    @Override
    @DefaultFrom("name")
    String getId();


    @Type("common.yaml#/$defs/Map-String-Set-String--")
    Map<String, List<String>> getAttributes();

    
    Set<FileClientEntity> getClients();

}
