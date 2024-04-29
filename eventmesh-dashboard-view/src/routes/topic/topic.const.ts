import { TopicCreationStatusEnum, TopicHealthStatusEnum } from './topic.types'

export const TopicHealthStatusText = {
  [TopicHealthStatusEnum.Failed]: '失败',
  [TopicHealthStatusEnum.Succeed]: '成功',
  [TopicHealthStatusEnum.Checking]: '正在检查',
  [TopicHealthStatusEnum.Expired]: '已超时'
}

export const TopicCreationStatusText = {
  [TopicCreationStatusEnum.Pending]: '等待创建',
  [TopicCreationStatusEnum.Processing]: '正在创建',
  [TopicCreationStatusEnum.Created]: '创建成功',
  [TopicCreationStatusEnum.Failed]: '创建失败',
  [TopicCreationStatusEnum.Unkown]: '未知'
}
