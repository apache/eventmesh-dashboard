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

import React, { forwardRef } from 'react'
import { Stack, StackProps, TextField, Paper, Button } from '@mui/material'
import { DataGrid, GridColDef, GridRowsProp } from '@mui/x-data-grid'
import { grey } from '@mui/material/colors'

interface TopicListProps extends StackProps {}

const TopicList = forwardRef<typeof Stack, TopicListProps>(
  ({ ...props }, ref) => {
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
          <TextField size="small" placeholder="Topic 名称"  variant="outlined" />

          <Button
            variant="outlined"
            size="small"
            sx={{ textTransform: 'none' }}>
            新增Topic
          </Button>
        </Stack>

        <DataGrid
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
          rows={rows}
          columns={columns}
        />
      </Paper>
    )
  }
)

TopicList.displayName = 'TopicList'
export default TopicList

const rows: GridRowsProp = [
  {
    id: 1,
    topicName: 'Topicname A0012',
    topicStatus: 'NORMAL',
    topicDesc: 'Topicname A0012 is for service A'
  },
  {
    id: 2,
    topicName: 'Topicname C23',
    topicStatus: 'ABNORMAL',
    topicDesc: 'This is for service B'
  },
  {
    id: 3,
    topicName: 'Topicname 40012',
    topicStatus: 'ABNORMAL',
    topicDesc:
      'Topic description. This could be too long to display completely in the cell'
  }
]

const columns: GridColDef[] = [
  { field: 'topicName', headerName: 'Topic 名称', width: 240 },
  { field: 'topicStatus', headerName: '健康状态', width: 150 },
  { field: 'topicDesc', headerName: '描述', flex: 1 },
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
