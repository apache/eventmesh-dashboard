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

package org.apache.eventmesh.dashboard.core.metadata;

import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;

import java.util.List;

/**
 * @param <T> metadata type or entity type, {@code <T>} is the source type of handler, there should be a converter in the handler to convert
 *            {@code <T>} to the target type.<p> method in this interface should be implemented as async method, if the method is eventmesh manage
 *            operation.
 */
public interface MetadataHandler<T> {


    default void handleAll(List<T> addData, List<T> updateData, List<T> deleteData) {
        if (addData != null) {
            addData.forEach(this::addMetadata);
        }
        if (updateData != null) {
            updateData.forEach(this::updateMetadata);
        }
        if (deleteData != null) {
            deleteData.forEach(this::deleteMetadata);
        }
    }

    default void handleAllObject(List<Object> addData, List<Object> updateData, List<Object> deleteData) {
        handleAll((List<T>) addData, (List<T>) updateData, (List<T>) deleteData);
    }

    //metaData: topic, center, etc. add meta is to create a topic.
    void addMetadata(T meta);

    default void addMetadata(List<T> meta) {
        if (meta != null) {
            meta.forEach(this::addMetadata);
        }
    }

    default void addMetadataObject(Object meta) {
        addMetadata((T) meta);
    }

    default void addMetadataObject(List<Object> meta) {
        if (meta != null) {
            meta.forEach(t -> addMetadata((T) t));
        }
    }

    default void replaceMetadata(List<Object> meta) {
        if (meta != null) {
            deleteMetadata((List<T>) meta);
            addMetadataObject(meta);
        }
    }

    default void updateMetadata(T meta) {
        this.addMetadata(meta);
    }

    /**
     * If this handler is db handler, do implement this method to improve performance
     *
     * @param meta
     */
    default void updateMetadata(List<T> meta) {
        if (meta != null) {
            meta.forEach(this::updateMetadata);
        }
    }

    default void updateMetadataObject(Object meta) {
        this.addMetadata((T) meta);
    }

    void deleteMetadata(T meta);

    default void deleteMetadata(List<T> meta) {
        if (meta != null) {
            meta.forEach(this::deleteMetadata);
        }
    }

    default void deleteMetadataObject(Object meta) {
        deleteMetadata((T) meta);
    }

    List<T> getData();

    List<T> getData(GlobalRequest globalRequest);

}
