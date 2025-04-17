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

/**
 * @param <T> metadata type or entity type, {@code <T>} is the source type of handler, there should be a converter in the handler to convert
 *            {@code <T>} to the target type.<p> method in this interface should be implemented as async method, if the method is eventmesh manage
 *            operation.
 */
public interface MetadataHandler<T> extends DataMetadataHandler<T>, UpdateMetadataHandler<T> {


}
