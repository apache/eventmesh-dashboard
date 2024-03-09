import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import Page from '../../components/Page'
import Construction from '../../components/Construction'

interface ConnectionProps extends BoxProps {}

const Connection = forwardRef<typeof Box, ConnectionProps>(
  ({ ...props }, ref) => {
    return (
      <Page>
        <Construction />
      </Page>
    )
  }
)

Connection.displayName = 'Connection'
export default Connection
