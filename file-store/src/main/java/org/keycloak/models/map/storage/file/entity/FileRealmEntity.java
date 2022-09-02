package org.keycloak.models.map.storage.file.entity;

import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.MapRealmEntityImpl;
import org.keycloak.models.map.storage.file.annotations.IdSubstituteWhenIdMissing;

@GenerateJsonSchema("realm.yaml")
public interface FileRealmEntity extends MapRealmEntity {

    @Override
    @IdSubstituteWhenIdMissing
    @Required
    String getName();


    @Type("common.yaml#/$defs/Map-String-Set-String--")
    Map<String, List<String>> getAttributes();

    
    Set<FileClientEntity> getClients();

}
