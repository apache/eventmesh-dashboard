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
import { fetchTopics } from '../../../service/topics'
import { useAppSelector } from '../../../store'
import {
  Topic,
  TopicCreationStatusEnum,
  TopicHealthStatusEnum
} from '../topic.types'
import { TopicCreationStatusText, TopicHealthStatusText } from '../topic.const'

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
    const seletedClusterId = useAppSelector(
      (state) => state.public.seletedClusterId
    )
    const [loading, setLoading] = useState(false)
    const [listParams, setListParams] = useState<TopicListParams>({
      page: 1,
      pageSize: 10,
      clusterId: 0
    })
    const [listDatas, setListDatas] = useState<TopicListDatas>({
      topics: [],
      totalCount: 0
    })

    const getTopics = async () => {
      if (loading) {
        return
      }
      setLoading(true)
      const resp = await fetchTopics(listParams)
      setListDatas(resp.data)
      setLoading(false)
    }

    useEffect(() => {
      if (seletedClusterId) {
        console.log(seletedClusterId)
        setListParams({ ...listParams, clusterId: seletedClusterId })
      }
    }, [seletedClusterId])

    useEffect(() => {
      getTopics()
    }, [listParams])

    return (
      <Paper
        sx={{
          flexGrow: 1,
          borderRadius: 4,
          boxShadow: '2px 2px 40px 2px rgba(0,0,0,.05)'
        }}>
        <Stack
          sx={{ px: 2, py: 3 }}
          direction="row"
          justifyContent="space-between"
          alignItems="center">
          <TextField size="small" placeholder="Topic 名称" variant="outlined" />

          <Button
            variant="outlined"
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
