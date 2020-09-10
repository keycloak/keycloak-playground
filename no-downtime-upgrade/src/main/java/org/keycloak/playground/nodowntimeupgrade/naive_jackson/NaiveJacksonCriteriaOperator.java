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

import org.keycloak.playground.nodowntimeupgrade.base.storage.ModelCriteriaBuilder.Operator;
import java.util.EnumMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author hmlnarik
 */
public class NaiveJacksonCriteriaOperator {

    private static final EnumMap<Operator, Function<Object, Predicate<Object>>> OPERATORS = new EnumMap<>(Operator.class);

    private static final Logger LOG = Logger.getLogger(NaiveJacksonCriteriaOperator.class.getSimpleName());

    private static final Predicate<Object> ALWAYS_FALSE = o -> false;

    static {
        OPERATORS.put(Operator.EQ, NaiveJacksonCriteriaOperator::eq);
        OPERATORS.put(Operator.NE, NaiveJacksonCriteriaOperator::ne);
        OPERATORS.put(Operator.LT, NaiveJacksonCriteriaOperator::lt);
        OPERATORS.put(Operator.LE, NaiveJacksonCriteriaOperator::le);
        OPERATORS.put(Operator.GT, NaiveJacksonCriteriaOperator::gt);
        OPERATORS.put(Operator.GE, NaiveJacksonCriteriaOperator::ge);
        OPERATORS.put(Operator.LIKE, NaiveJacksonCriteriaOperator::like);
        OPERATORS.put(Operator.ILIKE, NaiveJacksonCriteriaOperator::ilike);
    }

    /**
     * Returns a predicate {@code P(x)} for comparing {@code value} and {@code x} as {@code x OP value}.
     * <b>Implementation note:</b> Note that this may mean reverse logic to e.g. {@link Comparable#compareTo}.
     * @param operator
     * @param value
     * @return
     */
    public static Predicate<Object> predicateFor(Operator op, Object value) {
        final Function<Object, Predicate<Object>> funcToGetPredicate = OPERATORS.get(op);
        if (funcToGetPredicate == null) {
            throw new IllegalArgumentException("Unknown operator: " + op);
        }
        return funcToGetPredicate.apply(value);
    }

    public static Predicate<Object> eq(Object value) {
        return v -> Objects.equals(value, v);
    }

    public static Predicate<Object> ne(Object value) {
        return v -> ! Objects.equals(value, v);
    }

    public static Predicate<Object> lt(Object value) {
        if (value instanceof Comparable) {
            Comparable cValue = (Comparable) value;
            return o -> {
                try {
                    return o != null && cValue.compareTo(o) >  0;
                } catch (ClassCastException ex) {
                    LOG.log(Level.WARNING, "Incomparable argument type for comparison operation: {0}", value.getClass().getSimpleName());
                    return false;
                }
            };
        } else {
            throw new IllegalArgumentException("Incomparable argument for comparison operation: " + value);
        }
    }

    public static Predicate<Object> le(Object value) {
        if (value instanceof Comparable) {
            Comparable cValue = (Comparable) value;
            return o -> {
                try {
                    return o != null && cValue.compareTo(o) >= 0;
                } catch (ClassCastException ex) {
                    LOG.log(Level.WARNING, "Incomparable argument type for comparison operation: {0}", value.getClass().getSimpleName());
                    return false;
                }
            };
        } else {
            throw new IllegalArgumentException("Incomparable argument for comparison operation: " + value);
        }
    }

    public static Predicate<Object> gt(Object value) {
        if (value instanceof Comparable) {
            Comparable cValue = (Comparable) value;
            return o -> {
                try {
                    return o != null && cValue.compareTo(o) <  0;
                } catch (ClassCastException ex) {
                    LOG.log(Level.WARNING, "Incomparable argument type for comparison operation: {0}", value.getClass().getSimpleName());
                    return false;
                }
            };
        } else {
            throw new IllegalArgumentException("Incomparable argument for comparison operation: " + value);
        }
    }

    public static Predicate<Object> ge(Object value) {
        if (value instanceof Comparable) {
            Comparable cValue = (Comparable) value;
            return o -> {
                try {
                    return o != null && cValue.compareTo(o) <= 0;
                } catch (ClassCastException ex) {
                    LOG.log(Level.WARNING, "Incomparable argument type for comparison operation: {0}", value.getClass().getSimpleName());
                    return false;
                }
            };
        } else {
            throw new IllegalArgumentException("Incomparable argument for comparison operation: " + value);
        }
    }

    public static Predicate<Object> like(Object value) {
        if (value instanceof String) {
            String sValue = (String) value;
            boolean anyBeginning = sValue.startsWith("%");
            boolean anyEnd = sValue.endsWith("%");

            Pattern pValue = Pattern.compile(
              (anyBeginning ? ".*" : "")
              + Pattern.quote(sValue.substring(anyBeginning ? 1 : 0, sValue.length() - (anyEnd ? 1 : 0)))
              + (anyEnd ? ".*" : ""),
              Pattern.DOTALL
            );
            return o -> {
                return o instanceof String && pValue.matcher((String) o).matches();
            };
        }
        return ALWAYS_FALSE;
    }

    public static Predicate<Object> ilike(Object value) {
        if (value instanceof String) {
            String sValue = (String) value;
            boolean anyBeginning = sValue.startsWith("%");
            boolean anyEnd = sValue.endsWith("%");

            Pattern pValue = Pattern.compile(
              (anyBeginning ? ".*" : "")
              + Pattern.quote(sValue.substring(anyBeginning ? 1 : 0, sValue.length() - (anyEnd ? 1 : 0)))
              + (anyEnd ? ".*" : ""),
              Pattern.CASE_INSENSITIVE + Pattern.DOTALL
            );
            return o -> {
                return o instanceof String && pValue.matcher((String) o).matches();
            };
        }
        return ALWAYS_FALSE;
    }
}
