import React, { forwardRef } from 'react'
import { Paper, PaperProps, Stack, Typography } from '@mui/material'
import { Icons } from '../../../assets/icons'

interface TopicCountProps extends PaperProps {}

const TopicCount = forwardRef<typeof Paper, TopicCountProps>(
  ({ ...props }, ref) => {
    return (
      <Paper
        sx={{
          height: 80,
          pt: 1,
          pb: 1,
          pr: {
            sm: 1,
            md: 3
          },
          pl: {
            sm: 1,
            md: 3
          }
        }}>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center">
          <Icons.EventMeshTopic />
          <Stack sx={{ width: 120 }} alignItems="center">
            <Typography paragraph variant="caption" sx={{ m: 0 }}>
              Topic 总数量
            </Typography>
            <Typography
              paragraph
              variant="h6"
              color="primary"
              sx={{ m: 0, fontWeight: 'bold' }}>
              5
            </Typography>
          </Stack>
        </Stack>
      </Paper>
    )
  }
)

TopicCount.displayName = 'TopicCount'
export default TopicCount
