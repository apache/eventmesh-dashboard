/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React from 'react'
import { useRoutes, Navigate } from 'react-router-dom'
import Home from './home/Home'
import ClusterOverView from './eventmesh/clusters/overview/Overview'
import ClusterTopic from './eventmesh/clusters/topic/Topic'
import ClusterRuntime from './eventmesh/clusters/runtime/Runtime'
import ClusterConnection from './eventmesh/clusters/connection/Connection'
import ClusterMessage from './eventmesh/clusters/message/Message'
import ClusterSecurity from './eventmesh/clusters/security/Security'
import User from './user/User'
import Settings from './settings/Settings'
import Clusters from './eventmesh/clusters/Clusters'
import K8s from './k8s/K8s'
import RocketMq from './rocket-mq/RocketMq'
import Connection from './connection/Connection'
import EventMesh from './eventmesh/Eventmesh'

const AppRoutes = () => {
  return useRoutes([
    {
      path: '*',
      element: <Navigate to="home" replace />
    },
    { path: 'home', element: <Home /> },
    {
      path: 'eventmesh',
      element: <EventMesh />,
      children: [
        {
          path: 'clusters',
          element: <Clusters />,
          children: [
            {
              path: '*',
              element: <Navigate to="home" replace />
            },
            { path: ':clusterId/overview', element: <ClusterOverView /> },
            { path: ':clusterId/runtime', element: <ClusterRuntime /> },
            { path: ':clusterId/topic', element: <ClusterTopic /> },
            { path: ':clusterId/connection', element: <ClusterConnection /> },
            { path: ':clusterId/message', element: <ClusterMessage /> },
            { path: ':clusterId/security', element: <ClusterSecurity /> }
          ]
        }
      ]
    },
    { path: 'connection', element: <Connection /> },
    { path: 'rocket-mq', element: <RocketMq /> },
    { path: 'k8s', element: <K8s /> },
    { path: 'settings', element: <Settings /> },
    { path: 'user', element: <User /> },
  ])
}

export default AppRoutes
