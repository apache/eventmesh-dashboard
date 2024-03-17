export enum NavMenuIdEnum {
    Home = 'HOME',
    Runtime = 'RUNTIME',
    Topic = 'TOPIC',
    Connection = 'CONNECTION',
    Message = 'MESSAGE',
    Security = 'SECURITY',
    Settings = 'SETTINGS',
    Users = 'USERS',
    Logs = 'LOGS'
}

export type NavMenuType = {
    id: NavMenuIdEnum
    icon: React.ReactNode
    text: string
    route: string
    count?: number
}