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

import React, { forwardRef, useEffect, useState } from 'react'
import {
  Stack,
  StackProps,
  TextField,
  Paper,
  Button,
  Typography
} from '@mui/material'
import { DataGrid, GridColDef, GridRowsProp } from '@mui/x-data-grid'
import { grey } from '@mui/material/colors'
import { fetchTopics } from '../../../../../service/topics'
import {
  Topic,
  TopicCreationStatusEnum,
  TopicHealthStatusEnum
} from '../topic.types'
import { TopicCreationStatusText, TopicHealthStatusText } from '../topic.const'
import { useParams } from 'react-router-dom'
import { AppSectionBoxShadow } from '../../../../../app.const'

export type TopicListParams = {
  page: number
  pageSize: number
  clusterId: number
  topicName?: string
}

export type TopicListDatas = {
  topics: Topic[]
  totalCount: number
}

interface TopicListProps extends StackProps {}

const TopicList = forwardRef<typeof Stack, TopicListProps>(
  ({ ...props }, ref) => {
    const routeParams = useParams()
    const clusterId = routeParams?.clusterId
    const [loading, setLoading] = useState(false)

    const [listParams, setListParams] = useState<TopicListParams>({
      page: 1,
      pageSize: 10,
      clusterId: Number(clusterId)
    })

    const [listDatas, setListDatas] = useState<TopicListDatas>({
      topics: [],
      totalCount: 0
    })

    const getTopics = async () => {
      if (loading && Boolean(clusterId)) {
        return
      }
      setLoading(true)
      const resp = await fetchTopics(listParams)
      if (resp?.code) {
        setListDatas(resp.data)
      }
      setLoading(false)
    }

    useEffect(() => {
      setListParams({ ...listParams, clusterId: Number(clusterId) })
    }, [clusterId])

    useEffect(() => {
      getTopics()
    }, [listParams])

    return (
      <Paper
        sx={{
          flexGrow: 1,
          borderRadius: 4,
          boxShadow: AppSectionBoxShadow
        }}>
        <Stack
          sx={{ px: 2, py: 3 }}
          direction="row"
          justifyContent="space-between"
          alignItems="center">
          <TextField size="small" placeholder="Topic 名称" variant="outlined" />

          <Button
            variant="contained"
            size="small"
            sx={{ textTransform: 'none' }}>
            新增Topic
          </Button>
        </Stack>

        <DataGrid
          loading={loading}
          density="compact"
          sx={{
            border: 0,
            borderRadius: 4,
            '& .MuiDataGrid-columnHeaderTitle': {
              color: grey[400]
            },
            '& .MuiDataGrid-cell': {
              fontWeight: 'normal',
              fontSize: 14
            }
          }}
          rows={listDatas.topics}
          columns={getTopicColumns()}
        />
      </Paper>
    )
  }
)

TopicList.displayName = 'TopicList'
export default TopicList

const getTopicColumns = (): GridColDef<Topic>[] => {
  return [
    {
      field: 'id',
      headerName: 'Topic ID',
      type: 'number',
      width: 80,
      renderCell: (row) => {
        return <Button>{row.value}</Button>
      }
    },
    {
      field: 'topicName',
      headerName: 'Topic 名称',
      width: 240
    },
    {
      field: 'status',
      headerName: '健康状态',
      width: 150,
      renderCell: (row) => {
        const topicStatus = row.value as TopicHealthStatusEnum
        return (
          <Typography fontSize={'inherit'}>
            {TopicHealthStatusText[topicStatus]}
          </Typography>
        )
      }
    },
    {
      field: 'createProgress',
      headerName: '创建状态',
      width: 150,
      renderCell: (row) => {
        const topicStatus = row.value as TopicCreationStatusEnum
        return (
          <Typography fontSize={'inherit'}>
            {TopicCreationStatusText[topicStatus]}
          </Typography>
        )
      }
    },
    {
      field: 'retentionMs',
      headerName: '保留时间（ms）',
      width: 150
    },
    { field: 'description', headerName: '描述', flex: 1 },
    {
      field: 'actions',
      headerName: '操作',
      headerAlign: 'center',
      width: 240,
      align: 'center',
      renderCell: () => {
        return (
          <Stack direction="row" spacing={1}>
            <Button size="small">清除数据</Button>
            <Button size="small">删除</Button>
          </Stack>
        )
      }
    }
  ]
}
