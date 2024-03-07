import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import Page from '../../components/Page'
import Construction from '../../components/Construction'

interface UsersProps extends BoxProps {}

const Users = forwardRef<typeof Box, UsersProps>(({ ...props }, ref) => {
  return (
    <Page ref={ref}>
      <Construction />
    </Page>
  )
})

Users.displayName = 'Users'
export default Users
