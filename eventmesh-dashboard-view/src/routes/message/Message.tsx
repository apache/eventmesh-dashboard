import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import Page from '../../components/Page'
import Construction from '../../components/Construction'

interface MessageProps extends BoxProps {}

const Message = forwardRef<typeof Box, MessageProps>(({ ...props }, ref) => {
  return (
    <Page ref={ref}>
      <Construction />
    </Page>
  )
})

Message.displayName = 'Message'
export default Message
