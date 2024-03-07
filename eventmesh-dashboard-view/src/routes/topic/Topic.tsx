import React, { forwardRef } from 'react'
import { Box, BoxProps, Paper, Stack } from '@mui/material'
import Page from '../../components/Page'
import TopicCount from './TopicCount'
import AbnormalTopicCount from './AbnormalTopicCount'

interface TopicProps extends BoxProps {}

const Topic = forwardRef<typeof Box, TopicProps>(({ ...props }, ref) => {
  return (
    <Page>
      <Stack sx={{ width: 1, height: 1, p: 2 }} spacing={1}>
        <Stack direction="row" spacing={1}>
          <Stack sx={{ width: { sm: 180, md: 240, lg: 360 } }} spacing={1}>
            <TopicCount />
            <AbnormalTopicCount />
          </Stack>
          <Stack>
            <Stack direction="row">
              <Box>Filter</Box>
              <Box>Refresh</Box>
            </Stack>
            <Stack direction="row">
              <Box sx={{ flexFlow: 1 }}>Topic Stats</Box>
              <Box>Message Stats</Box>
              <Box>Average Message Stats</Box>
            </Stack>
          </Stack>
        </Stack>
        <Stack sx={{ flexGrow: 1 }}>
          <Stack
            direction="row"
            justifyContent="space-between"
            alignItems="center">
            <Box> Search</Box>
            <Box> Add</Box>
          </Stack>
          <Paper sx={{ flexGrow: 1 }}>List</Paper>
        </Stack>
      </Stack>
    </Page>
  )
})

Topic.displayName = 'Topic'
export default Topic
