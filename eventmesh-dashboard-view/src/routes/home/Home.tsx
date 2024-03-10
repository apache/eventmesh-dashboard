import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import Construction from '../../components/Construction'
import Page from '../../components/Page'

interface HomeProps extends BoxProps {}

const Home = forwardRef<typeof Box, HomeProps>(({ ...props }, ref) => {
  return (
    <Page>
      <Construction />
    </Page>
  )
})

Home.displayName = 'Home'
export default Home
