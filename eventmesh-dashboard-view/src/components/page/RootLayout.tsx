import React, { forwardRef } from 'react'
import { Box, Stack, StackProps } from '@mui/material'

import { Outlet } from 'react-router-dom'
import { grey } from '@mui/material/colors'
import Navigation from '../../routes/navigation/Navigation'

interface RootLayoutProps extends StackProps {}

const RootLayout = forwardRef<typeof Box, RootLayoutProps>(
  ({ children, ...props }, ref) => {
    return (
      <Stack
        direction="row"
        sx={{
          position: 'relative',
          height: '100vh',
          bgcolor: grey[100]
        }}>
        <Navigation />
        <Box sx={{ flexGrow: 1, pl: 4, overflow: 'hidden' }}>{children}</Box>
      </Stack>
    )
  }
)
RootLayout.displayName = 'RootLayout'
export default RootLayout
