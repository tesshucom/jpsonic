/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.util;

import java.util.HashMap;

/*
 * Class that aggregates HashMap generation.
 * - Allows null value.
 * - Variable.
 * In these two points, the policies of legacy implementation are not unified.
 * If solved , most impls can be replaced with Map.of(...) after Java8.
 */
@SuppressWarnings("UnnecessarilyFullyQualified")
public final class LegacyMap {

    private LegacyMap() {
    }

    /*
     * The part that calls this may simply not be replaced.
     */
    public static <K, V> java.util.Map<K, V> of() {
        return new HashMap<>();
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2) {
        return createMapN(k1, v1, k2, v2);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return createMapN(k1, v1, k2, v2, k3, v3);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9,
                k10, v10);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10, K k11,
            V v11) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9,
                k10, v10, k11, v11);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10, K k11, V v11,
            K k12, V v12) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9,
                k10, v10, k11, v11, k12, v12);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10, K k11, V v11,
            K k12, V v12, K k13, V v13) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9,
                k10, v10, k11, v11, k12, v12, k13, v13);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10, K k11, V v11,
            K k12, V v12, K k13, V v13, K k14, V v14) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9,
                k10, v10, k11, v11, k12, v12, k13, v13, k14, v14);
    }

    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
            K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10, K k11, V v11,
            K k12, V v12, K k13, V v13, K k14, V v14, K k15, V v15) {
        return createMapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9,
                k10, v10, k11, v11, k12, v12, k13, v13, k14, v14, k15, v15);
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public static <K, V> java.util.Map<K, V> of(K key, V value) {
        java.util.Map<K, V> result = new HashMap<>(1);
        result.put(key, value);
        return result;
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static <K, V> java.util.Map<K, V> createMapN(Object... input) {
        if ((input.length & 1) != 0) {
            throw new InternalError("length is odd");
        }
        java.util.Map<K, V> result = new HashMap<>(input.length);
        for (int i = 0; i < input.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = (K) input[i];
            @SuppressWarnings("unchecked")
            V v = (V) input[i + 1];
            result.put(k, v);
        }
        return result;
    }

}
