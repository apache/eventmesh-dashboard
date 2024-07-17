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
import { Box, BoxProps, Button, Stack, Tab, Tabs } from '@mui/material'
import Page from '../../../../components/page/Layout'
import Stats from './stats/Stats'
import TopicList from './topic-list/TopicList'
import { styled } from '@mui/material/styles'

interface TopicProps extends BoxProps {}

const PagSubMenuButton = styled(Button)({
  paddingLeft: 8,
  paddingRight: 8,
  paddingTop: 4,
  paddingBottom: 4,
  borderRadius: 8,
  textTransform: 'none',
  color: '#43497a',
  boxShadow: 'none',
  fontSize: 'small',

  '&.active': {
    backgroundColor: '#17c8eb',
    color: '#fff',
    boxShadow: '2px 2px 20px 5px rgba(0,0,0,0.05)'
  }
})

const Topic = forwardRef<typeof Box, TopicProps>(({ ...props }, ref) => {
  return (
    <Page sx={{ height: 1, display: 'flex', flexDirection: 'column' }}>
      <Stack direction="row" sx={{ pt: 1, pb: 1 }} spacing={2}>
        <PagSubMenuButton className="active">Overview</PagSubMenuButton>
        <PagSubMenuButton> Topic List</PagSubMenuButton>
      </Stack>

      <Stack sx={{ flexGrow: 1 }} spacing={2}>
        <Stats />
        <TopicList />
      </Stack>
    </Page>
  )
})

Topic.displayName = 'Topic'
export default Topic
