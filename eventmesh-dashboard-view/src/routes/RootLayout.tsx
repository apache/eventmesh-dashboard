import React from 'react'
import { Box, Stack } from '@mui/material'

import { Outlet } from 'react-router-dom'
import { grey } from '@mui/material/colors'
import Navigation from './navigation/Navigation'

const RootLayout = () => {
  return (
    <Stack
      direction="row"
      sx={{
        position: 'relative',
        height: '100vh',
        bgcolor: grey[100]
      }}>
      <Navigation />
      <Box sx={{ flexGrow: 1, overflow: 'hidden' }}>
        <Outlet />
      </Box>
    </Stack>
  )
}

export default RootLayout
