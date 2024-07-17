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

import React, { forwardRef, useState } from 'react'
import {
  Box,
  BoxProps,
  Button,
  Paper,
  Stack,
  TextField,
  Typography
} from '@mui/material'

import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { Cluster } from './cluster.type'
import RounderPaper from '../../../components/RounderPaper'
import { useNavigate } from 'react-router-dom'

export type ClusterListParams = {
  page: number
  pageSize: number
  clusterId?: number
}

export type ClusterListDatas = {
  topics: Cluster[]
  totalCount: number
}

interface ClusterListProps extends BoxProps {}

const ClusterList = forwardRef<typeof Box, ClusterListProps>(
  ({ ...props }, ref) => {
    const navigate = useNavigate()
    const [loading, setLoading] = useState(false)
    const [listParams, setListParams] = useState<ClusterListParams>({
      page: 1,
      pageSize: 10
    })

    const [listDatas, setListDatas] = useState<ClusterListDatas>({
      topics: [
        {
          id: 1,
          name: 'Demo Cluster',
          status: '运行中',
          healthStatus: '健康',
          numOfRuntime: '2',
          storage: '123 Gb'
        }
      ],
      totalCount: 0
    })

    return (
      <RounderPaper sx={{ flexGrow: 1 }} elevation={8}>
        <Stack sx={{ height: 1 }} spacing={2}>
          <Stack
            direction="row"
            justifyContent="space-between"
            sx={{ pt: 2, pl: 2, pr: 2 }}>
            <TextField size="small" placeholder="集群 ID"></TextField>
            <Button size="small" variant="contained">
              新增集群
            </Button>
          </Stack>
          <DataGrid
            loading={loading}
            sx={{ flexGrow: 1, border: 0, borderRadius: 4 }}
            rows={listDatas.topics}
            columns={getTopicColumns({
              handlerIdClick: (clusterId) => {
                navigate(`/eventmesh-cluster/${clusterId}/overview`)
              }
            })}
          />
        </Stack>
      </RounderPaper>
    )
  }
)

ClusterList.displayName = 'ClusterList'
export default ClusterList

const getTopicColumns = ({
  handlerIdClick
}: {
  handlerIdClick: (clusterId: string | number) => void
}): GridColDef<Cluster>[] => {
  return [
    {
      field: 'id',
      headerName: '集群 ID',
      width: 160,
      disableColumnMenu: true,
      filterable: false,
      sortable: false,
      renderCell: (row) => {
        return (
          <Button onClick={() => handlerIdClick(row.value)}>{row.value}</Button>
        )
      }
    },
    {
      field: 'name',
      headerName: '集群名称',
      flex: 1,
      disableColumnMenu: true,
      filterable: false,
      sortable: false
    },
    {
      field: 'status',
      headerName: '状态',
      width: 180,
      disableColumnMenu: true,
      filterable: false,
      sortable: false,
      renderCell: (row) => {
        return (
          <Typography fontSize="inherit" color="'primary.main'">
            {row.value}
          </Typography>
        )
      }
    },
    {
      field: 'healthStatus',
      headerName: '健康状态',
      width: 180,
      disableColumnMenu: true,
      filterable: false,
      sortable: false,
      renderCell: (row) => {
        return (
          <Typography fontSize="inherit" color="success.main">
            {row.value}
          </Typography>
        )
      }
    },
    {
      field: 'numOfRuntime',
      headerName: 'Runtime 数量',
      width: 180,
      disableColumnMenu: true,
      filterable: false,
      sortable: false,
      renderCell: (row) => {
        return row.value
      }
    },
    {
      field: 'actions',
      headerName: '操作',
      headerAlign: 'center',
      width: 120,
      align: 'center',
      disableColumnMenu: true,
      filterable: false,
      sortable: false,
      renderCell: () => {
        return (
          <Stack direction="row" spacing={1}>
            <Button size="small">删除</Button>
          </Stack>
        )
      }
    }
  ]
}
