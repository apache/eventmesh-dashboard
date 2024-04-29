import React, { forwardRef, useEffect, useState } from 'react'
import { Stack, StackProps, Box, Divider, Typography } from '@mui/material'
import { Icons } from '../../assets/icons'
import { grey } from '@mui/material/colors'
import NavigationItem from './NavigationItem'
import { NavMenuIdEnum, NavMenuType } from './navigation.types'
import { useNavigate } from 'react-router-dom'
import { fetchResourceStats } from '../../service/topics'

const DefGeneralMenus: NavMenuType[] = [
  {
    id: NavMenuIdEnum.Home,
    icon: <Icons.Home />,
    text: 'Home',
    route: 'home',
    count: 0
  },
  {
    id: NavMenuIdEnum.Runtime,
    icon: <Icons.Runtime style={{ color: 'inherit' }} />,
    text: 'Runtime',
    route: 'runtime',
    count: 0
  },
  {
    id: NavMenuIdEnum.Topic,
    icon: <Icons.Topic />,
    text: 'Topic',
    route: 'topic',
    count: 0
  },
  {
    id: NavMenuIdEnum.Connection,
    icon: <Icons.Connection />,
    text: 'Connection',
    route: 'connection',
    count: 0
  },
  {
    id: NavMenuIdEnum.Message,
    icon: <Icons.Message />,
    text: 'Message',
    route: 'message',
    count: 0
  },
  {
    id: NavMenuIdEnum.Security,
    icon: <Icons.Security />,
    text: 'Security',
    route: 'security',
    count: 0
  }
]

const DefSystemMenus: NavMenuType[] = [
  {
    id: NavMenuIdEnum.Settings,
    icon: <Icons.Settings />,
    route: 'settings',
    text: 'Settings'
  },
  {
    id: NavMenuIdEnum.Users,
    icon: <Icons.Users style={{ color: 'inherit' }} />,
    route: 'users',
    text: 'Users'
  },
  { id: NavMenuIdEnum.Logs, icon: <Icons.Logs />, route: 'logs', text: 'Logs' }
]

interface NavigationProps extends StackProps {}

const Navigation = forwardRef<typeof Stack, NavigationProps>(
  ({ ...props }, ref) => {
    const navigate = useNavigate()
    const [generalMenus, setGeneralMenus] = useState(DefGeneralMenus)
    const [clusterId, setClusterId] = useState(1)

    const [activeMenuId, setActiveMenuId] = useState<NavMenuIdEnum>(
      NavMenuIdEnum.Home
    )

    const getResourceStats = async () => {
      const resp = await fetchResourceStats(clusterId)
      if (resp.data) {
        const respData = resp.data
        const newGeneralMenus = generalMenus.map((menu) => {
          switch (menu.id) {
            case NavMenuIdEnum.Topic: {
              return { ...menu, count: respData.topicsNum }
            }
            case NavMenuIdEnum.Connection: {
              return { ...menu, count: respData.connectionsNum }
            }
            default: {
              return menu
            }
          }
        })
        setGeneralMenus(newGeneralMenus)
      }
    }

    useEffect(() => {
      getResourceStats()
    }, [])

    return (
      <Stack sx={{ width: 260, px: 3 }}>
        <Box
          sx={{
            height: 80,
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'centers'
          }}>
          <Icons.EventMeshLogo style={{ width: '60%' }} />
        </Box>
        <Divider />
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
          sx={{ pl: 2, pr: 2, mt: 1, mb: 1 }}>
          <Typography
            variant="overline"
            fontSize={12}
            fontWeight="bold"
            color={grey[600]}>
            General
          </Typography>

          <Stack direction="row" alignItems="center">
            <Icons.AddCircleOutlineOutlined
              fontSize="inherit"
              sx={{ transform: 'scale(.7)' }}
            />
            <Typography variant="caption" fontSize={13}>
              接入集群
            </Typography>
          </Stack>
        </Stack>
        <Stack sx={{ pl: 2, pr: 2 }} spacing={1}>
          {generalMenus.map((item) => {
            return (
              <NavigationItem
                key={item.id}
                icon={item.icon}
                text={item.text}
                count={item?.count}
                active={activeMenuId === item.id}
                onClick={() => {
                  setActiveMenuId(item.id)
                  navigate(item.route)
                }}
              />
            )
          })}
        </Stack>
        <Divider sx={{ mt: 4 }} />
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
          sx={{ pl: 2, pr: 2, mt: 1, mb: 1 }}>
          <Typography
            variant="overline"
            fontSize={12}
            fontWeight="bold"
            color={grey[600]}>
            System
          </Typography>
        </Stack>
        <Stack sx={{ pl: 2, pr: 2 }} spacing={1}>
          {DefSystemMenus.map((item) => {
            return (
              <NavigationItem
                key={item.id}
                icon={item.icon}
                text={item.text}
                count={item?.count}
                active={activeMenuId === item.id}
                onClick={() => {
                  setActiveMenuId(item.id)
                  navigate(item.route)
                }}
              />
            )
          })}
        </Stack>
      </Stack>
    )
  }
)

Navigation.displayName = 'Navigation'
export default Navigation
