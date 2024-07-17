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
import {
  Stack,
  StackProps,
  Fade,
  Typography,
  Button,
  Breadcrumbs,
  Link,
  SxProps
} from '@mui/material'
import { Icons } from '../../../../assets/icons'
import { styled } from '@mui/material/styles'
import { useNavigate, useParams } from 'react-router-dom'

interface ClusterMenuProps extends StackProps {}

const PageMenuButton = styled(Button)({
  boxShadow: 'none',
  width: '100%',
  paddingLeft: 15,
  paddingRight: 15,
  paddingTop: 8,
  paddingBottom: 8,
  borderRadius: 8,
  textTransform: 'none',
  color: '#43497a',

  '&.active': {
    boxShadow: 'none',
    backgroundColor: '#17c8eb',
    color: '#fff'
  }
})

const ClusterMenu = forwardRef<typeof Stack, ClusterMenuProps>(
  ({ ...props }, ref) => {
    const navigate = useNavigate()
    const { clusterId } = useParams()

    return (
      <Stack
        spacing={2}
        direction="row"
        alignItems="center"
        justifyContent={'space-between'}
        sx={{ bgcolor: '#fafafa', pl: 1, pr: 1 }}>
        <Breadcrumbs>
          <Link
            underline="hover"
            color="inherit"
            href="/eventmesh-cluster/list">
            <Stack direction="row" spacing={1} alignItems="center">
              <Icons.List fontSize="small" color="inherit"></Icons.List>
              <Typography fontSize="inherit">Cluster List</Typography>
            </Stack>
          </Link>
          <Typography color="text.primary">1</Typography>
        </Breadcrumbs>

        <Stack direction={'row'} spacing={1}>
          <PageMenuButton
            className="active"
            onClick={() => navigate(`${clusterId}/overview`)}>
            Overview
          </PageMenuButton>
          <PageMenuButton
            onClick={() => {
              navigate(`${clusterId}/topic`)
            }}>
            Topic
          </PageMenuButton>
          <PageMenuButton>Message</PageMenuButton>
          <PageMenuButton>Meta</PageMenuButton>
          <PageMenuButton>Runtime</PageMenuButton>
          <PageMenuButton>Storage</PageMenuButton>
          <PageMenuButton>User</PageMenuButton>
          <PageMenuButton>Log</PageMenuButton>
          <PageMenuButton>Setting</PageMenuButton>
          <PageMenuButton>Config</PageMenuButton>
        </Stack>
      </Stack>
    )
  }
)

ClusterMenu.displayName = 'ClusterMenu'
export default ClusterMenu
