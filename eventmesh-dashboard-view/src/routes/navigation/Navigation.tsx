import React, { forwardRef, useState } from 'react'
import {
  Stack,
  StackProps,
  Box,
  Divider,
  Typography,
  Chip
} from '@mui/material'
import { Icons } from '../../assets/icons'
import { grey } from '@mui/material/colors'
import NavigationItem from './NavigationItem'

const DefNavigations = [
  { icon: <Icons.HomeOutlined />, text: 'Home', count: 0 },
  { icon: <Icons.HomeOutlined />, text: 'Runtime', count: 0 },
  { icon: <Icons.HomeOutlined />, text: 'Topic', count: 5, active: true },
  { icon: <Icons.HomeOutlined />, text: 'Connection', count: 0 },
  { icon: <Icons.HomeOutlined />, text: 'Message', count: 0 },
  { icon: <Icons.HomeOutlined />, text: 'Security', count: 0 }
]

interface NavigationProps extends StackProps {}

const Navigation = forwardRef<typeof Stack, NavigationProps>(
  ({ ...props }, ref) => {
    const [navigationMenus, setMavigationMenus] = useState(DefNavigations)
    return (
      <Stack sx={{ width: 240, boxShadow: 1, bgcolor: 'white' }}>
        <Box sx={{ height: 80 }}>Logo</Box>
        <Divider />
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
          sx={{ pl: 2, pr: 2, mt: 1, mb: 1 }}>
          <Typography variant="overline" fontSize={12} color={grey[600]}>
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
        <Stack sx={{ pl: 2, pr: 2 }} spacing={0.5}>
          {navigationMenus.map((item) => {
            return (
              <NavigationItem
                icon={item.icon}
                text={item.text}
                count={item.count}
                active={item.active}
              />
            )
          })}
        </Stack>
        <Divider sx={{ mt: 4 }} />
      </Stack>
    )
  }
)

Navigation.displayName = 'Navigation'
export default Navigation
