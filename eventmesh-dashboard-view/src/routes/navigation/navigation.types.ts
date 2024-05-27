export enum NavMenuIdEnum {
  Home = 'HOME',
  Clusters = 'CLUSTERS',
  Settings = 'SETTINGS',
  Users = 'USERS',
  Logs = 'LOGS',

  ClusterOverview = 'CLUSTER_OVERVIEW',
  ClusterRuntime = 'CLUSTER_RUNTIME',
  ClusterTopic = 'CLUSTER_TOPIC',
  ClusterConnection = 'CLUSTER_CONNECTION',
  ClusterMessage = 'CLUSTER_MESSAGE',
  ClusterSecurity = 'CLUSTER_SECURITY'
}

export type NavMenuType = {
  id: NavMenuIdEnum
  icon: React.ReactNode
  text: string
  route: string
  count?: number
  subMenus?: NavMenuType[]
  pinSubMenus?: boolean
}

export type ResourceStats = {
  topicsNum: number
  connectionsNum: number
}
