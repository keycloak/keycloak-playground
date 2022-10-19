package org.keycloak.models.map.storage.file.entity.shortcut;

import java.util.stream.Stream;


public interface ShortcutConverter<F, T> {

    Stream<T> createEntitiesFromShortcut(F shortcut);

    F createShortcutFromEntity(T entity);

}
