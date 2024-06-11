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
import ClusterOverView from './cluster/overview/Overview'
import ClusterTopic from './cluster/topic/Topic'
import ClusterRuntime from './cluster/runtime/Runtime'
import ClusterConnection from './cluster/connection/Connection'
import ClusterMessage from './cluster/message/Message'
import ClusterSecurity from './cluster/security/Security'
import Users from './users/Users'
import Logs from './elogs/Logs'
import Settings from './settings/Settings'
import Clusters from './cluster/Clusters'

const AppRoutes = () => {
  return useRoutes([
    {
      path: '*',
      element: <Navigate to="home" replace />
    },
    { path: 'home', element: <Home /> },
    {
      path: 'clusters',
      element: <Clusters />,
      children: [
        { path: 'clusters', element: <Clusters /> },
        {
          path: ':clusterId',
          children: [
            {
              path: '*',
              element: <Navigate to="home" replace />
            },
            { path: 'overview', element: <ClusterOverView /> },
            { path: 'runtime', element: <ClusterRuntime /> },
            { path: 'topic', element: <ClusterTopic /> },
            { path: 'connection', element: <ClusterConnection /> },
            { path: 'message', element: <ClusterMessage /> },
            { path: 'security', element: <ClusterSecurity /> }
          ]
        }
      ]
    },
    { path: 'settings', element: <Settings /> },
    { path: 'users', element: <Users /> },
    { path: 'logs', element: <Logs /> }
  ])
}

export default AppRoutes
