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
import { Stack, StackProps } from '@mui/material'
import Construction from '../../components/Construction'
import RootLayout from '../../components/page/RootLayout'

interface K8sProps extends StackProps {}

const K8s = forwardRef<typeof Stack, K8sProps>(
  ({ children, ...props }, ref) => {
    return (
      <RootLayout>
        <Construction title="K8s" />
      </RootLayout>
    )
  }
)

K8s.displayName = 'K8s'
export default K8s
