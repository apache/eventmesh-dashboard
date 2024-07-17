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

import React, { forwardRef, useEffect, useState } from 'react'
import {
  Stack,
  StackProps,
  Box,
  Divider,
  Typography,
  alpha
} from '@mui/material'
import { Icons } from '../../assets/icons'
import { grey } from '@mui/material/colors'
import NavigationItem from './NavigationItem'
import { NavMenuIdEnum, NavMenuType } from './navigation.types'
import { useNavigate } from 'react-router-dom'
import NavigationSubMenu from './NavigationSubMenu'
import { useAppDispatch, useAppSelector } from '../../store'
import { globalSlice } from '../../store/reducers/public/public.slice'

const getNavigationMenus = (): NavMenuType[] => {
  return [
    {
      id: NavMenuIdEnum.Home,
      icon: <Icons.Home />,
      text: 'Home',
      route: '/home',
      count: 0
    },
    {
      id: NavMenuIdEnum.Eventmesh,
      icon: <Icons.Eventmesh />,
      text: 'Eventmesh',
      route: '/eventmesh-cluster/list',
      count: 0,
    },

    {
      id: NavMenuIdEnum.Connection,
      icon: <Icons.Connection />,
      text: 'Connection',
      route: '/connection',
      count: 0
    },
    {
      id: NavMenuIdEnum.ROCKET_MQ,
      icon: <Icons.RocketMq />,
      route: '/rocket-mq',
      text: 'Rocket MQ',
      count: 0
    },
    {
      id: NavMenuIdEnum.K8S,
      icon: <Icons.K8s />,
      route: '/k8s',
      text: 'K8s',
      count: 0
    },

    {
      id: NavMenuIdEnum.User,
      icon: <Icons.Users />,
      route: '/users',
      text: 'Users'
    },

    {
      id: NavMenuIdEnum.Settings,
      icon: <Icons.Settings />,
      route: '/settings',
      text: 'Settings'
    }
  ]
}

interface NavigationProps extends StackProps {}

const Navigation = forwardRef<typeof Stack, NavigationProps>(
  ({ ...props }, ref) => {
    const navigate = useNavigate()
    const dispatch = useAppDispatch()
    const navigation = useAppSelector((state) => state.public.navigation)
    const activeMenuId = navigation.activeMenuId
    const pinSubmenuIds = navigation.pinSubmenuIds
    const [navigationMenus, setNavigationMenus] = useState<NavMenuType[]>(
      getNavigationMenus()
    )

    const setActiveMenuId = (menuId: NavMenuIdEnum) => {
      dispatch(globalSlice.actions.setNavigationActiveMenuId(menuId))
    }

    const changeMenuPinStatus = (menuId: NavMenuIdEnum) => {
      if (pinSubmenuIds?.includes(menuId)) {
        dispatch(
          globalSlice.actions.setNavigationPinMenuIds(
            pinSubmenuIds.filter((item) => {
              return item !== menuId
            })
          )
        )
      } else {
        dispatch(
          globalSlice.actions.setNavigationPinMenuIds([
            ...pinSubmenuIds,
            menuId
          ])
        )
      }
    }

    // const getResourceStats = async () => {
    //   const resp = await fetchResourceStats(clusterId)
    //   if (resp.data) {
    //     const respData = resp.data
    //     const newGeneralMenus = generalMenus.map((menu) => {
    //       switch (menu.id) {
    //         case NavMenuIdEnum.ClusterTopic: {
    //           return { ...menu, count: respData.topicsNum }
    //         }
    //         case NavMenuIdEnum.ClusterConnection: {
    //           return { ...menu, count: respData.connectionsNum }
    //         }
    //         default: {
    //           return menu
    //         }
    //       }
    //     })
    //     setGeneralMenus(newGeneralMenus)
    //   }
    // }

    // useEffect(() => {
    //   getResourceStats()
    // }, [])

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
          {navigationMenus.map((item, idx) => {
            const hasActiveSubmenu = Boolean(
              item.subMenus?.find((subMenu) => subMenu.id === activeMenuId)
            )
            const showSubmenu =
              activeMenuId === item.id ||
              Boolean(
                item.subMenus?.find((subMenu) => subMenu.id === activeMenuId)
              ) ||
              Boolean(pinSubmenuIds?.includes(item.id))

            return (
              <Stack
                key={item.id}
                sx={{ bgcolor: alpha('#fff', 0.5), borderRadius: 2 }}>
                <NavigationItem
                  key={item.id}
                  icon={item.icon}
                  text={item.text}
                  count={item?.count}
                  hasSubmenu={Boolean(item.subMenus)}
                  pinSubmenu={pinSubmenuIds.includes(item.id)}
                  active={activeMenuId === item.id || hasActiveSubmenu}
                  onPinChange={() => {
                    changeMenuPinStatus(item.id)
                  }}
                  onMenuActived={() => {
                    setActiveMenuId(item.id)
                    navigate(item.route)
                  }}
                />

                {item.subMenus && (
                  <NavigationSubMenu
                    show={showSubmenu}
                    menus={item?.subMenus}
                    activeMenuId={activeMenuId}
                    onMenuActived={(menuId) => setActiveMenuId(menuId)}
                  />
                )}
              </Stack>
            )
          })}
        </Stack>
      </Stack>
    )
  }
)

Navigation.displayName = 'Navigation'
export default Navigation
