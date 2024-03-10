import React, { forwardRef, useState } from 'react'
import {
  Stack,
  StackProps,
  Box,
  Select,
  MenuItem,
  Paper,
  Grid
} from '@mui/material'
import TopicCount from './TopicCount'
import AbnormalTopicCount from './AbnormalTopicCount'
import { Icons } from '../../../assets/icons'
import StatsChart from './StatsChart'

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
    <Stack direction="row" spacing={1}>
      <Stack sx={{ width: { sm: 180, md: 240, lg: 360 } }} spacing={1}>
        <TopicCount />
        <AbnormalTopicCount />
      </Stack>

      <Paper sx={{ flexGrow: 1, p: 1 }}>
        <Stack sx={{ width: 1, height: 1 }} spacing={2}>
          <Stack
            direction="row"
            justifyContent="space-between"
            alignItems="center">
            <Stack direction="row" spacing={2}>
              <Select
                sx={{
                  fontSize: 14
                }}
                disableUnderline
                displayEmpty
                size="small"
                variant="standard"
                value={statsParams.time ?? ''}>
                <MenuItem value={''}>所有时间段</MenuItem>
                {TimeOptions.map((opt) => {
                  return (
                    <MenuItem
                      key={opt.value}
                      value={opt.value}
                      sx={{ fontSize: 'inherit' }}>
                      {opt.label}
                    </MenuItem>
                  )
                })}
              </Select>

              <Select
                sx={{ fontSize: 14 }}
                displayEmpty
                disableUnderline
                size="small"
                variant="standard"
                value={statsParams.time ?? ''}>
                <MenuItem value={''}>所有 Runtime</MenuItem>
                {TimeOptions.map((opt) => {
                  return (
                    <MenuItem
                      key={opt.value}
                      value={opt.value}
                      sx={{ fontSize: 'inherit' }}>
                      {opt.label}
                    </MenuItem>
                  )
                })}
              </Select>
            </Stack>

            <Icons.Refresh fontSize="inherit" />
          </Stack>
          <Grid container sx={{ flexGrow: 1 }}>
            <Grid item sm={4}>
              <StatsChart />
            </Grid>
            <Grid item sm={4}>
              <StatsChart />
            </Grid>
            <Grid item sm={4}>
              <StatsChart />
            </Grid>
          </Grid>
        </Stack>
      </Paper>
    </Stack>
  )
})

Stats.displayName = 'Stats'
export default Stats
