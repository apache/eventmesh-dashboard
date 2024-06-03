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
import { Box, BoxProps, Stack } from '@mui/material'
import Page from '../../../components/page/Layout'
import Stats from './stats/Stats'
import TopicList from './topic-list/TopicList'

interface TopicProps extends BoxProps {}

const Topic = forwardRef<typeof Box, TopicProps>(({ ...props }, ref) => {
  return (
    <Page>
      <Stack sx={{ width: 1, height: 1, p: 2 }} spacing={2}>
        <Stats />
        <TopicList />
      </Stack>
    </Page>
  )
})

Topic.displayName = 'Topic'
export default Topic
