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

import React, { forwardRef } from 'react'
import { Box, Stack, StackProps } from '@mui/material'

interface EventMeshProps extends StackProps {}
import RootLayout from '../../components/page/RootLayout'
import Page from '../../components/page/Layout'
import { Outlet, matchPath, useLocation } from 'react-router-dom'
import ClusterMenu from './cluster/cluster-menu/ClusterMenu'

const Eventmesh = forwardRef<typeof Stack, EventMeshProps>(
  ({ children, ...props }, ref) => {
    const { pathname } = useLocation()

    const isEventmeshClusterPath =
      !!matchPath('/eventmesh-cluster/*', pathname) &&
      !['/eventmesh-cluster/list', '/eventmesh-cluster/list/'].includes(
        pathname
      )

    return (
      <RootLayout>
        <Page
          sx={{ height: 1, p: 2, display: 'flex', flexDirection: 'column' }}>
          {isEventmeshClusterPath && <ClusterMenu />}
          <Outlet />
        </Page>
      </RootLayout>
    )
  }
)

Eventmesh.displayName = 'Eventmesh'
export default Eventmesh
