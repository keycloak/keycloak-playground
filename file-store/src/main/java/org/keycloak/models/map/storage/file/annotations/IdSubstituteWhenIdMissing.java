package org.keycloak.models.map.storage.file.annotations;

/**
 * Marks a getter method of a property which should be used as an ID in the case when
 * value of ID is not defined explicitly.
 */
public @interface IdSubstituteWhenIdMissing {
    int priority() default 1;

    Class<? extends ValueTransformer<?>> valueTransformer();
}
