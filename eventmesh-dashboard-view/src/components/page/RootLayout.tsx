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

import { Outlet } from 'react-router-dom'
import { grey } from '@mui/material/colors'
import Navigation from '../../routes/navigation/Navigation'

interface RootLayoutProps extends StackProps {}

const RootLayout = forwardRef<typeof Box, RootLayoutProps>(
  ({ children, ...props }, ref) => {
    return (
      <Stack
        direction="row"
        sx={{
          position: 'relative',
          height: '100vh',
          bgcolor: grey[100]
        }}>
        <Navigation />
        <Box sx={{ flexGrow: 1, pl: 4, overflow: 'hidden' }}>{children}</Box>
      </Stack>
    )
  }
)
RootLayout.displayName = 'RootLayout'
export default RootLayout
