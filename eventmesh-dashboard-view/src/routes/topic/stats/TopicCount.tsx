import React, { forwardRef } from 'react'
import { Paper, PaperProps, Stack, Typography, Button } from '@mui/material'
import { grey } from '@mui/material/colors'

interface TopicCountProps extends PaperProps {}

const TopicCount = forwardRef<typeof Paper, TopicCountProps>(
  ({ ...props }, ref) => {
    return (
      <Paper
        sx={{
          width: 1,
          py: 1,
          px: 2,
          borderRadius: 4,
          boxShadow: '2px 2px 40px 2px rgba(0,0,0,.05)',
          display: 'flex',
          flexDirection: 'column'
        }}>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center">
          <Typography
            paragraph
            variant="caption"
            sx={{ m: 0 }}
            fontWeight="bold"
            color={grey[600]}>
            Topic 总数量
          </Typography>
          <Button sx={{ color: grey[400] }} disabled size="small">
            <span>&nbsp;</span>
          </Button>
        </Stack>

        <Typography
          paragraph
          variant="h6"
          color="primary"
          sx={{ m: 0, fontWeight: 'bold', color: '#43497a' }}>
          5
        </Typography>
      </Paper>
    )
  }
)

TopicCount.displayName = 'TopicCount'
export default TopicCount
