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

package org.apache.eventmesh.dashboard.console.function.metadata;

import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class MetadataServiceWrapper {

    private SingleMetadataServiceWrapper dbToService;

    private SingleMetadataServiceWrapper serviceToDb;

    private Long cacheId;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SingleMetadataServiceWrapper {

        /**
         * true -> incremental updates false -> full volume updates
         *
         * @See MetadataManager
         */
        @Default
        private Boolean cache = true;


        /**
         * handler is the target of metadata, it will process the metadata from syncService
         */
        private MetadataHandler<?> handler;
    }
}
