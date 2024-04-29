import { ResourceStats } from '../routes/navigation/navigation.types'
import { TopicStats } from '../routes/topic/stats/topic-stats.types'
import {
  TopicListDatas,
  TopicListParams
} from '../routes/topic/topic-list/TopicList'
import { Topic } from '../routes/topic/topic.types'
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
  const respData: FetchRespone<ResourceStats> = {
    data: await resp.json()
  }
  return respData
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

  const respData: FetchRespone<TopicStats> = {
    data: await resp.json()
  }
  return respData
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
    data: { topics: respJson.data, totalCount: respJson.total }
  }
  return respData
}
