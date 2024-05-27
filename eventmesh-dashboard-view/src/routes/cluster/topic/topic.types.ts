export enum TopicHealthStatusEnum {
  // 失败;
  Failed = 0,
  // 成功;
  Succeed = 1,
  // 检查;
  Checking = 2,
  // 超时;
  Expired = 3
}

export enum TopicCreationStatusEnum {
  // 等待创建中;
  Pending = 0,
  // 正在创建中;
  Processing = 1,
  // 创建成功;
  Created = 2,
  // 创建失败;
  Failed = 3,
  // 未知
  Unkown = 4
}

export type Topic = {
  id: number
  topicName: string
  description: string
  status: TopicHealthStatusEnum
  createProgress: TopicCreationStatusEnum
  retentionMs: number
}
