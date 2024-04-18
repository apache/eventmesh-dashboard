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
import RootLayout from './RootLayout'
import Home from './home/Home'
import Topic from './topic/Topic'
import Runtime from './runtime/Runtime'
import Connection from './connection/Connection'
import Message from './message/Message'
import Security from './security/Security'
import Users from './users/Users'
import Logs from './logs/Logs'
import Settings from './settings/Settings'

const AppRoutes = () => {
  return useRoutes([
    {
      path: '/',
      element: <RootLayout />,
      children: [
        {
          path: '*',
          element: <Navigate to="/home" replace />
        },
        { path: '', element: <Navigate to="/home" /> },
        { path: 'home', element: <Home /> },
        { path: 'runtime', element: <Runtime /> },
        { path: 'topic', element: <Topic /> },
        { path: 'connection', element: <Connection /> },
        { path: 'message', element: <Message /> },
        { path: 'security', element: <Security /> },
        { path: 'settings', element: <Settings /> },
        { path: 'users', element: <Users /> },
        { path: 'logs', element: <Logs /> }
      ]
    }
  ])
}

export default AppRoutes
