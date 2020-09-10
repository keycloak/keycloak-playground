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

import org.keycloak.playground.nodowntimeupgrade.base.storage.ModelCriteriaBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static org.keycloak.playground.nodowntimeupgrade.base.model.HasId.SearchableFields.ID;
import static org.keycloak.playground.nodowntimeupgrade.naive_jackson.NaiveJacksonCriteriaOperator.predicateFor;

/**
 * Naive jackson criteria builder is really naive. The only indexed field
 * in the storage is the {@code id}, i.e. map key, yet it is still treated
 * as non-indexed sequential access field. The filtering of the rest
 * is postponed to later stage when the models are reconstructed from the
 * JSON form. This is highly inefficient.
 *
 * @author hmlnarik
 */
public class NaiveJacksonCriteriaBuilder<M> implements ModelCriteriaBuilder {

    @FunctionalInterface
    public interface TriConsumer<A extends NaiveJacksonCriteriaBuilder<?>,B,C> { A apply(A a, B b, C c); }

    private static final Predicate<Object> ALWAYS_TRUE = e -> true;
    private static final Predicate<Object> ALWAYS_FALSE = e -> false;

    private final Predicate<? super String> indexFilter;
    private final Predicate<? super M> modelFilter;

    private final Map<String, TriConsumer<NaiveJacksonCriteriaBuilder<M>, Operator, Object>> fieldPredicates;

    public NaiveJacksonCriteriaBuilder(Map<String, TriConsumer<NaiveJacksonCriteriaBuilder<M>, Operator, Object>> fieldPredicates) {
        this(fieldPredicates, ALWAYS_TRUE, ALWAYS_TRUE);
    }

    private NaiveJacksonCriteriaBuilder(Map<String, TriConsumer<NaiveJacksonCriteriaBuilder<M>, Operator, Object>> fieldPredicates,
      Predicate<? super String> indexReadFilter, Predicate<? super M> sequentialReadFilter) {
        this.fieldPredicates = fieldPredicates;
        this.indexFilter = indexReadFilter;
        this.modelFilter = sequentialReadFilter;
    }

    protected static <M> Map<String, TriConsumer<NaiveJacksonCriteriaBuilder<M>, Operator, Object>> basePredicates() {
        Map<String, TriConsumer<NaiveJacksonCriteriaBuilder<M>, Operator, Object>> fieldPredicates = new HashMap<>();
        fieldPredicates.put(ID, (o, op, value) -> o.idCompare(op, value));
        return fieldPredicates;
    }

    @Override
    public ModelCriteriaBuilder compare(String modelField, Operator op, Object value) {
        TriConsumer<NaiveJacksonCriteriaBuilder<M>, Operator, Object> method = fieldPredicates.get(modelField);
        if (method == null) {
            throw new IllegalArgumentException("Filter not implemented for field " + modelField);
        }

        return method.apply(this, op, value);
    }

    protected NaiveJacksonCriteriaBuilder<M> idCompare(Operator op, Object value) {
        if (! (value instanceof String)) {
            return new NaiveJacksonCriteriaBuilder<>(fieldPredicates, ALWAYS_FALSE, this.modelFilter);
        }

        switch (op) {
            case EQ:
            case NE:
            case LT:
            case LE:
            case GT:
            case GE:
                return new NaiveJacksonCriteriaBuilder<>(fieldPredicates, this.indexFilter.and(predicateFor(op, value)), this.modelFilter);
            default:
                throw new AssertionError("Invalid operator: " + op);
        }
    }

    protected NaiveJacksonCriteriaBuilder<M> fieldCompare(Operator op, Object value, Function<? super M, ?> getter) {
        Predicate<Object> valueComparator = predicateFor(op, value);
        final Predicate<M> p = m -> valueComparator.test(getter.apply(m));
        final Predicate<? super M> mf = modelFilter;
        final Predicate<? super M> resModelFilter = p == ALWAYS_FALSE || mf == ALWAYS_FALSE
          ? ALWAYS_FALSE
          : p.and(mf);
        return new NaiveJacksonCriteriaBuilder<>(fieldPredicates, this.indexFilter, resModelFilter);
    }

    @Override
    public ModelCriteriaBuilder and(ModelCriteriaBuilder... builders) {
        Predicate<? super String> resIndexFilter = Stream.of(builders)
          .map(NaiveJacksonCriteriaBuilder.class::cast)
          .map(NaiveJacksonCriteriaBuilder::getIndexFilter)
          .reduce(ALWAYS_TRUE, (p1, p2) -> p1.and(p2));
        Predicate<? super M> resModelFilter = Stream.of(builders)
          .map(NaiveJacksonCriteriaBuilder.class::cast)
          .map(NaiveJacksonCriteriaBuilder::getModelFilter)
          .reduce(ALWAYS_TRUE, (p1, p2) -> p1.and(p2));
        return new NaiveJacksonCriteriaBuilder<>(fieldPredicates, resIndexFilter, resModelFilter);
    }

    @Override
    public ModelCriteriaBuilder or(ModelCriteriaBuilder... builders) {
        Predicate<? super String> resIndexFilter = Stream.of(builders)
          .map(NaiveJacksonCriteriaBuilder.class::cast)
          .map(NaiveJacksonCriteriaBuilder::getIndexFilter)
          .reduce(ALWAYS_FALSE, (p1, p2) -> p1.or(p2));
        Predicate<? super M> resModelFilter = Stream.of(builders)
          .map(NaiveJacksonCriteriaBuilder.class::cast)
          .map(NaiveJacksonCriteriaBuilder::getModelFilter)
          .reduce(ALWAYS_FALSE, (p1, p2) -> p1.or(p2));
        return new NaiveJacksonCriteriaBuilder<>(fieldPredicates, resIndexFilter, resModelFilter);
    }

    @Override
    public ModelCriteriaBuilder not(ModelCriteriaBuilder builder) {
        NaiveJacksonCriteriaBuilder b = builder.unwrap(NaiveJacksonCriteriaBuilder.class);
        Predicate<? super String> resIndexFilter = b.getIndexFilter() == ALWAYS_TRUE ? ALWAYS_TRUE : b.getIndexFilter().negate();
        Predicate<? super M> resModelFilter = b.getModelFilter() == ALWAYS_TRUE ? ALWAYS_TRUE : b.getModelFilter().negate();
        return new NaiveJacksonCriteriaBuilder<>(fieldPredicates, resIndexFilter, resModelFilter);
    }

    @Override
    public <T extends ModelCriteriaBuilder> T unwrap(Class<T> clazz) {
        if (clazz.isInstance(this)) {
            return clazz.cast(this);
        } else {
            throw new ClassCastException("Incompatible class: " + clazz);
        }
    }

    public Predicate<? super String> getIndexFilter() {
        return indexFilter;
    }

    public Predicate<? super M> getModelFilter() {
        return modelFilter;
    }

}
