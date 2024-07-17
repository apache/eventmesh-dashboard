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

import { ResourceStats } from '../routes/navigation/navigation.types'
import { TopicStats } from '../routes/eventmesh/cluster/topic/stats/topic-stats.types'
import {
  TopicListDatas,
  TopicListParams
} from '../routes/eventmesh/cluster/topic/topic-list/TopicList'
import { Topic } from '../routes/eventmesh/cluster/topic/topic.types'
import { InstanceTypeEnum } from '../types/types'
import { FetchRespone, ListApiRespone } from './request.types'

const ServiceHost = process.env.REACT_APP_SERVICE_HOST

export const fetchResourceStats = async (
  clusterId: number
): Promise<FetchRespone<ResourceStats>> => {
  const queryParams = new URLSearchParams({
    clusterId: clusterId.toString()
  }).toString()

  const EventMeshHeaders = new Headers()
  EventMeshHeaders.append('Content-Type', 'application/json')
  EventMeshHeaders.append('queryClause', '{}')

  const resp = await fetch(
    `${ServiceHost}/cluster/getResourceNum?${queryParams}`,
    { method: 'GET', headers: EventMeshHeaders }
  )

  const respJson = (await resp.json()) as unknown as FetchRespone<ResourceStats>

  return respJson
}

export const fetchTopicStats = async (
  clusterId: number
): Promise<FetchRespone<TopicStats>> => {
  const queryParams = new URLSearchParams({
    instanceType: InstanceTypeEnum.Topic.toString(),
    clusterId: clusterId.toString()
  }).toString()

  const headers = new Headers()
  headers.append('Content-Type', 'application/json')
  headers.append('queryClause', '{}')

  const resp = await fetch(
    `${ServiceHost}/cluster/health/getInstanceLiveProportion?${queryParams}`,
    { method: 'GET', headers }
  )

  const respJson = (await resp.json()) as unknown as FetchRespone<TopicStats>

  return respJson
}

export const fetchTopics = async (
  params: TopicListParams
): Promise<FetchRespone<TopicListDatas>> => {
  const { page, pageSize, clusterId, topicName } = params

  const headers = new Headers()
  headers.append('Content-Type', 'application/json')
  headers.append(
    'queryClause',
    JSON.stringify({
      limitPageNum: page,
      limitStart: 0,
      limitSize: pageSize
    })
  )

  const resp = await fetch(`${ServiceHost}/cluster/topic/topicList`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      clusterId,
      ...(topicName && { topicName })
    })
  })

  const respJson = (await resp.json()) as unknown as ListApiRespone<Topic>

  const respData: FetchRespone<TopicListDatas> = {
    code: respJson.code,
    data: { topics: respJson.data, totalCount: respJson.total },
    message: respJson.message
  }
  return respData
}
