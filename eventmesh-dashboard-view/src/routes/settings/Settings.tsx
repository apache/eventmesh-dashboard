import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import Page from '../../components/Page'
import Construction from '../../components/Construction'

interface SettingsProps extends BoxProps {}

const Settings = forwardRef<typeof Box, SettingsProps>(({ ...props }, ref) => {
  return (
    <Page ref={ref}>
      <Construction />
    </Page>
  )
})

Settings.displayName = 'Settings'
export default Settings
