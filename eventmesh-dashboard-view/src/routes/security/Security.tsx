import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import Page from '../../components/Page'
import Construction from '../../components/Construction'

interface SecurityProps extends BoxProps {}

const Security = forwardRef<typeof Box, SecurityProps>(({ ...props }, ref) => {
  return (
    <Page ref={ref}>
      <Construction />
    </Page>
  )
})

Security.displayName = 'Security'
export default Security
