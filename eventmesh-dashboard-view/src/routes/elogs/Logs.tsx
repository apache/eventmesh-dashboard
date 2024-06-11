import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import Page from '../../components/page/Layout'
import Construction from '../../components/Construction'

interface LogsProps extends BoxProps {}

const Logs = forwardRef<typeof Box, LogsProps>(({ ...props }, ref) => {
  return (
    <Page ref={ref}>
      <Construction />
    </Page>
  )
})

Logs.displayName = 'Logs'
export default Logs
