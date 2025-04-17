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


package org.apache.eventmesh.dashboard.core.metadata.difference;


import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;

import java.util.List;

public class BodyDataDifference extends AbstractBufferDifference {


    @Override
    void doDifference() {
        List<BaseClusterIdBase> objectList = sourceHandler.getData();
        if (objectList.isEmpty()) {
            return;
        }
        objectList.forEach((value) -> {
            String key = value.nodeUnique();
            if (value.isInsert()) {
                this.insertData.add(value);
                this.allData.put(key, value);
            } else if (value.isUpdate()) {
                this.updateData.add(value);
                this.allData.put(key, value);
            } else if (value.isDelete()) {
                this.deleteData.add(value);
                this.allData.remove(key);
            }
        });

    }
}
