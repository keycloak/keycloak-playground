/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.playground.nodowntimeupgrade.naive_jackson;

import org.keycloak.playground.nodowntimeupgrade.base.model.HasId;
import org.keycloak.playground.nodowntimeupgrade.base.model.ModelVersion;
import org.keycloak.playground.nodowntimeupgrade.base.storage.ModelCriteriaBuilder;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @param <M> Object model class
 * @param <E> Entity class
 * @author hmlnarik
 */
public abstract class AbstractNaiveVersionedJacksonStorage<M extends HasId<String>, E> extends AbstractMapStorage<M> {

    private final static byte[] EMPTY = new byte[] {};

    public static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)

      .setSerializationInclusion(JsonInclude.Include.NON_NULL)

      .setVisibility(PropertyAccessor.ALL, Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    ;

    private final int supportedVersion;

    private final Function<M, E> modelToEntityFunc;

    private final Class<E> entityClass;

    private final Class<M> modelClass;

    private final Function<E, M> entityToModelFunc;

    private final ModelCriteriaBuilder criteriaBuilder;

    /**
     *
     * @param store Store where the objects are stored to.
     * @param version Version of the object models this particular store is handling.
     * @param modelToEntityFunc Function to create an entity from a model.
     * @param entityClass Class representing the entity. Used for serialization from JSON to internal entity representation.
     * @param entityToModelFunc Function to create a model from an entity that is guaranteed to be
     *        at the model version compatible with {@code version}, migrated beforehand if necessary.
     *        Entity version is compatible if it is either {@code supportedVersion} or {@code(#supportedVersion + 1)}.
     */
    public AbstractNaiveVersionedJacksonStorage(
      ConcurrentMap<String, byte[]> store,
      ModelVersion version,
      Class<M> modelClass, Function<M, E> modelToEntityFunc,
      Class<E> entityClass, Function<E, M> entityToModelFunc,
      ModelCriteriaBuilder criteriaBuilder) {
        super(store);
        this.supportedVersion = version.getVersion();
        this.modelToEntityFunc = modelToEntityFunc;
        this.entityToModelFunc = entityToModelFunc;
        this.entityClass = entityClass;
        this.modelClass = modelClass;
        this.criteriaBuilder = criteriaBuilder;
    }

    @Override
    protected byte[] modelToByteArray(M object) throws Exception {
        if (object == null) {
            return EMPTY;
        }
        return MAPPER.writeValueAsBytes(modelToEntityFunc.apply(object));
    }

    @Override
    protected M byteArrayToModel(byte[] bytes) throws Exception {
        return byteArrayToModel(bytes, e -> true);
    }

    /**
     * Returns model converted from the byte array. The flow goes this way:
     * <ol>
     * <li>Parse the JSON in {@code bytes}</li>
     * <li>Migrate tree from previous versions to the current one via JSON operations</li>
     * <li>Convert the parsed tree to the entity</li>
     * <li>Validate that the entity satisfies given {@code include} predicate</li>
     * <li>Return {@code null} if the predicate is not satisfied, or convert the entity into the model</li>
     * </ol>
     * @param bytes
     * @param modelFilter
     * @return
     * @throws IllegalArgumentException if the version is incompatible
     * @throws IOException See {@link ObjectMapper#readValue}
     * @throws JsonParseException See {@link ObjectMapper#readValue}
     * @throws JsonMappingException See {@link ObjectMapper#readValue}
     */
    protected M byteArrayToModel(byte[] bytes, Predicate<? super M> modelFilter) throws Exception {
        // validate input
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        // validate logical entity version
        ObjectNode tree = MAPPER.readValue(bytes, ObjectNode.class);
        JsonNode ev = tree.get("entityVersion");
        if (ev == null || ! ev.isInt()) {
            return null;
        }
        int entityVersion = ev.asInt();

        if (entityVersion < supportedVersion) {
            tree = Migration.migrateTreeTo(entityVersion, supportedVersion, tree);
        }

        if (! canReadVersion(entityVersion) ) {
            throw new IllegalArgumentException("Incompatible version: " + entityVersion);
        }

        return Optional.ofNullable(MAPPER.treeToValue(tree, entityClass))
          .map(entityToModelFunc)
          .filter(modelFilter)
          .orElse(null);
    }

    /**
     * Same as {@link #byteArrayToModel(byte[], java.util.function.Predicate)} with
     * any exception thrown in the body converted to {@link RuntimeException}.
     * @param bytes
     * @param modelFilter
     * @return
     */
    protected M maskedByteArrayToModel(byte[] bytes, Predicate<? super M> modelFilter) {
        try {
            return byteArrayToModel(bytes, modelFilter);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public int getSupportedVersion() {
        return supportedVersion;
    }

    protected boolean canReadVersion(int versionToBeRead) {
        // Forward compatibility is maintained between two adjacent versions for this playground project.
        // This may or may be different in the real setup.

        return versionToBeRead <= getSupportedVersion() + 1;
    }

    @Override
    public String dump(String id) {
        final byte[] bytes = store.get(id);
        return bytes == null ? "<null>" : new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public Stream<M> read(ModelCriteriaBuilder criteria) {
        NaiveJacksonCriteriaBuilder<M> b = criteria.unwrap(NaiveJacksonCriteriaBuilder.class);
        return store.keySet().stream()
          .filter(b.getIndexFilter())
          .map(store::get)
          .map(byteArr -> maskedByteArrayToModel(byteArr, b.getModelFilter()))
          .filter(Objects::nonNull);
    }

    @Override
    public ModelCriteriaBuilder getCriteriaBuilder() {
        return this.criteriaBuilder;
    }

}
