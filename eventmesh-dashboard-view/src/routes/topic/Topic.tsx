import React, { forwardRef, useState } from 'react'
import { Box, BoxProps, Paper, Stack } from '@mui/material'
import Page from '../../components/Page'
import Stats from './stats/Stats'
import TopicList from './topic-list/TopicList'

interface TopicProps extends BoxProps {}

const Topic = forwardRef<typeof Box, TopicProps>(({ ...props }, ref) => {
  return (
    <Page>
      <Stack sx={{ width: 1, height: 1, p: 2 }} spacing={1}>
        <Stats />
        <TopicList />
      </Stack>
    </Page>
  )
})

Topic.displayName = 'Topic'
export default Topic
