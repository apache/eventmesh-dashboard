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
import { Box, BoxProps, Typography } from '@mui/material'
import { Icons } from '../assets/icons'
import { grey } from '@mui/material/colors'

interface ConstructionProps extends BoxProps {
  title?: string
}

const Construction = forwardRef<typeof Box, ConstructionProps>(
  ({ title, ...props }, ref) => {
    return (
      <Box
        ref={ref}
        sx={{
          width: 1,
          height: 1,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          flexDirection: 'column',
          color: grey[500]
        }}>
        <Icons.Foundation fontSize="large" />
        <Typography paragraph>
          {title ? `${title} is coming` : 'Coming'} soon
        </Typography>
      </Box>
    )
  }
)

Construction.displayName = 'Construction'
export default Construction
