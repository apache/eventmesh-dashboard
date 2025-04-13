package org.apache.eventmesh.dashboard.core.metadata;

import java.util.List;

public interface UpdateMetadataHandler<T> {


    //metaData: topic, center, etc. add meta is to create a topic.
    void addMetadata(T meta);

    default void addMetadata(List<T> meta) {
        if (meta != null) {
            meta.forEach(this::addMetadata);
        }
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

    void deleteMetadata(T meta);

    default void deleteMetadata(List<T> meta) {
        if (meta != null) {
            meta.forEach(this::deleteMetadata);
        }
    }
}
