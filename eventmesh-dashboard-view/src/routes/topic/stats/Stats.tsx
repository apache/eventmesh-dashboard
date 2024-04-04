import React, { forwardRef, useState } from 'react'
import { Stack, StackProps, Select, MenuItem, Grid, Box } from '@mui/material'
import TopicCount from './TopicCount'
import AbnormalTopicCount from './AbnormalTopicCount'
import { Icons } from '../../../assets/icons'
import StatsChart from './StatsChart'
import { grey } from '@mui/material/colors'

enum TimeOptionEnum {
  LatestHour = 'LATEST_HOUR'
}
type StatsParams = {
  time?: TimeOptionEnum
  runtimeId?: string
}

const TimeOptions = [{ value: TimeOptionEnum.LatestHour, label: '最新一小时' }]

interface StatsProps extends StackProps {}

const Stats = forwardRef<typeof Stack, StatsProps>(({ ...props }, ref) => {
  const [statsParams, setStatsParams] = useState<StatsParams>({
    runtimeId: ''
  })

  return (
    <Stack spacing={2}>
      <Grid container columnGap={2} wrap="nowrap">
        <Grid item sm={4}>
          <TopicCount />
        </Grid>
        <Grid item sm={4}>
          <AbnormalTopicCount />
        </Grid>
        <Grid item sm={4}></Grid>
      </Grid>

      <Stack>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center">
          <Stack direction="row" spacing={2}>
            <Select
              sx={{ fontSize: 12, color: grey[500] }}
              disableUnderline
              displayEmpty
              size="small"
              variant="standard"
              value={statsParams.time ?? ''}>
              <MenuItem value={''} sx={{ fontSize: 12 }}>
                所有时间段
              </MenuItem>
              {TimeOptions.map((opt) => {
                return (
                  <MenuItem
                    key={opt.value}
                    value={opt.value}
                    sx={{ fontSize: 12 }}>
                    {opt.label}
                  </MenuItem>
                )
              })}
            </Select>

            <Select
              sx={{ fontSize: 12, color: grey[500] }}
              displayEmpty
              disableUnderline
              size="small"
              variant="standard"
              value={statsParams.time ?? ''}>
              <MenuItem value={''} sx={{ fontSize: 12 }}>
                所有 Runtime
              </MenuItem>
              {TimeOptions.map((opt) => {
                return (
                  <MenuItem
                    key={opt.value}
                    value={opt.value}
                    sx={{ fontSize: 12 }}>
                    {opt.label}
                  </MenuItem>
                )
              })}
            </Select>
          </Stack>

          <Icons.Refresh fontSize="inherit" />
        </Stack>
        <Grid container sx={{ height: 300 }} columnGap={2} wrap="nowrap">
          <Grid item sm={8}>
            <StatsChart title="Topic 数量" />
          </Grid>
          <Grid item sm={4}>
            <Stack sx={{ width: 1, height: 1 }} spacing={2}>
              <StatsChart small title="Topic 消息数量" />
              <StatsChart small title="Topic Message 平均数量" />
            </Stack>
          </Grid>
        </Grid>
      </Stack>
    </Stack>
  )
})

Stats.displayName = 'Stats'
export default Stats
