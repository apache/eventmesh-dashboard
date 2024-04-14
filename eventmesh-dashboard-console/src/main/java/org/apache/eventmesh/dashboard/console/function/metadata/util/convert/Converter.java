/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.function.metadata.util.convert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converter is used to convert data between database entity and metadata
 *
 * @param <S> source data
 * @param <T> target data
 */
public interface Converter<S, T> {

    T convert(S source);

    default List<T> convert(List<S> source) {
        List<T> results = new ArrayList<T>(source.size());
        source.forEach(t -> results.add(convert(t)));
        return results;
    }

    /**
     * @param source source data
     * @return A string that is unique to the source, usually a url
     */
    String getUnique(S source);

    default Map<String, Object> getUniqueKeyMap(List<S> source) {
        Map<String, Object> newObjectMap = new HashMap<>();
        if (Objects.nonNull(source)) {
            source.forEach(t -> newObjectMap.put(getUnique(t), t));
        }
        return newObjectMap;
    }

    default Map<String, Object> getUniqueKeyMapFromObject(List<Object> source) {
        return getUniqueKeyMap((List<S>) source);
    }

    default List<Object> convertFromObject(List<Object> source) {
        List<T> results = new ArrayList<T>(source.size());
        source.forEach(t -> results.add(convert((S) t)));
        return (List<Object>) results;
    }
}
