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


package org.apache.eventmesh.dashboard.console.controller.cluster;


import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

public class HomeController {


    private ClusterService clusterService;



    public void clusterHome(){
        // 运维事件

        // runtime 集群 以及

        // meta 集群 以及

        // 存储集群

        // kubernetes 列表。 本集群 没有或则资源不够，找上级集群。如果有多个上级

        // 下面的是否需要展示
        // client

        // group 列表 topic 列表。是否需要在 home 展示

        // 连接
    }

    public void runtimeHome(){
        // 在哪个 cluster

        // 在哪一个 kubernetes

        // message 统计

        // in out 统计

        // topic 统计数据

        // group 统计数据

        // client 统计数据

        // 连接 统计数据

        // 实例资源统计数据
    }


    public void groupHome(){
        // cluster

        // topic

        // client

        //
    }

    public void topicHome(){

    }

    public void clientHome(){

    }


    public void userHome(){

    }

}
