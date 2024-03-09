import React, { forwardRef } from 'react'
import { Paper, PaperProps, Stack, Typography, Button } from '@mui/material'
import { red } from '@mui/material/colors'

interface AbnormalTopicCountProps extends PaperProps {}

const AbnormalTopicCount = forwardRef<typeof Paper, AbnormalTopicCountProps>(
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
          <Stack alignItems="flex-start">
            <Typography paragraph variant="caption" sx={{ m: 0 }}>
              异常状态 Topic 数量
            </Typography>
            <Button sx={{ fontSize: 13, mb: -2 }} size="small">
              查看详情
            </Button>
          </Stack>
          <Stack sx={{ width: 120 }} alignItems="center">
            <Typography
              variant="subtitle1"
              sx={{
                width: 32,
                height: 32,
                borderRadius: 16,
                bgcolor: red[400],
                boxSizing: 'content-box',
                border: `8px solid ${red[50]}`,
                textAlign: 'center',
                fontWeight: 'bolder',
                color: 'white'
              }}>
              2
            </Typography>
          </Stack>
        </Stack>
      </Paper>
    )
  }
)

AbnormalTopicCount.displayName = 'AbnormalTopicCount'
export default AbnormalTopicCount
