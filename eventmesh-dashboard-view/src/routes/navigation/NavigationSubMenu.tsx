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
import {
  Button,
  ButtonProps,
  Stack,
  Typography,
  Chip,
  StackProps,
  Collapse
} from '@mui/material'
import { alpha, styled } from '@mui/material/styles'
import { grey } from '@mui/material/colors'
import { NavMenuIdEnum, NavMenuType } from './navigation.types'
import { useNavigate } from 'react-router-dom'

const NavButton = styled(Button)({
  boxShadow: 'none',
  width: '100%',
  paddingLeft: 15,
  paddingRight: 15,
  paddingTop: 8,
  paddingBottom: 8,
  borderRadius: 8,
  textTransform: 'none',
  color: '#43497a',
  justifyContent: 'flex-start',

  '& .MuiButton-startIcon': {
    padding: 5,
    marginRight: 15,
    color: 'inherit',
    // backgroundColor: 'white',
    borderRadius: 8
  },

  '&.active': {
    backgroundColor: 'white',
    boxShadow: '2px 2px 20px 5px rgba(0,0,0,0.05)',

    '& .MuiButton-startIcon': {
      boxShadow: 'none',
      color: '#17c8eb'
      // color: 'white',
      // backgroundColor: '#17c8eb'
    }
  }
})

interface NavigationSubMenuItemProps extends ButtonProps {
  icon: React.ReactNode
  text: string
  count?: number
  active: boolean
}

const NavigationSubMenuItem = forwardRef<
  typeof NavButton,
  NavigationSubMenuItemProps
>(({ text, count, icon, active, onClick, ...props }, ref) => {
  return (
    <NavButton
      startIcon={icon}
      variant={active ? 'contained' : 'text'}
      className={active ? 'active' : ''}
      // sx={{ color: active ? 'white' : 'inherit' }}
      onClick={onClick}>
      <Stack
        sx={{ width: 1 }}
        direction="row"
        justifyContent="space-between"
        alignItems="center">
        <Typography fontSize="inherit">{text}</Typography>
        {(count ?? 0) > 0 && (
          <Chip
            sx={{
              height: 15,
              fontSize: 13,
              bgcolor: active ? grey[200] : 'white'
            }}
            size="small"
            color="default"
            label={count}
          />
        )}
      </Stack>
    </NavButton>
  )
})

interface SubNavifationMenuProps extends StackProps {
  show: boolean
  menus: NavMenuType[]
  activeMenuId: string
  onMenuActived: (key: NavMenuIdEnum) => void
}

const NavigationSubMenu = forwardRef<typeof Stack, SubNavifationMenuProps>(
  ({ activeMenuId, show, onMenuActived, menus, onClick, ...props }, ref) => {
    const navigate = useNavigate()

    return (
      <Collapse in={show} exit={!show}>
        <Stack
          sx={{
            py: 2,
            px: 2,
            bgcolor: alpha('#fff', 0.5),
            borderTopLeftRadius: 0,
            borderTopRightRadius: 0,
            borderBottomLeftRadius: 8,
            borderBottomRightRadius: 8
          }}>
          {menus.map((menu) => {
            return (
              <NavigationSubMenuItem
                key={menu.id}
                icon={menu.icon}
                text={menu.text}
                count={menu?.count}
                active={activeMenuId === menu?.id}
                onClick={() => {
                  onMenuActived(menu?.id)
                  navigate(menu.route)
                }}
              />
            )
          })}
        </Stack>
      </Collapse>
    )
  }
)

NavigationSubMenu.displayName = 'NavigationSubMenu'
export default NavigationSubMenu
