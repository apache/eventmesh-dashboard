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

import React, { forwardRef, useState, useEffect } from 'react'
import { Stack, StackProps, Select, MenuItem, Grid } from '@mui/material'
import TopicCount from './TopicCount'
import AbnormalTopicCount from './AbnormalTopicCount'
import { Icons } from '../../../../../assets/icons'
import StatsChart from './StatsChart'
import { grey } from '@mui/material/colors'

import { useAppSelector } from '../../../../../store'
import { fetchTopicStats } from '../../../../../service/topics'
import { TopicStats } from './topic-stats.types'

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

  const [topicStats, setTopicStats] = useState<TopicStats | null>(null)
  const seletedClusterId = useAppSelector(
    (state) => state.public.seletedClusterId
  )

  const getTopicStats = async () => {
    if (!seletedClusterId) {
      return
    }
    const resp = await fetchTopicStats(seletedClusterId)
    if (resp.data) {
      setTopicStats(resp?.data)
    }
  }

  useEffect(() => {
    getTopicStats()
  }, [seletedClusterId])

  return (
    <Stack spacing={2}>
      <Grid container columnGap={2} wrap="nowrap">
        <Grid item sm={4}>
          <TopicCount value={topicStats?.allNum} />
        </Grid>
        <Grid item sm={4}>
          <AbnormalTopicCount value={topicStats?.abnormalNum} />
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
