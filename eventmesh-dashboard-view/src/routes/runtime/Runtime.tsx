import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import Page from '../../components/Page'
import Construction from '../../components/Construction'

interface RuntimeProps extends BoxProps {}

const Runtime = forwardRef<typeof Box, RuntimeProps>(({ ...props }, ref) => {
  return (
    <Page>
      <Construction />
    </Page>
  )
})

Runtime.displayName = 'Runtime'
export default Runtime
