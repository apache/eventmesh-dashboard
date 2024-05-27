import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import Construction from '../../../components/Construction'
import RootLayout from '../../../components/page/RootLayout'

interface HomeProps extends BoxProps {}

const Home = forwardRef<typeof Box, HomeProps>(({ ...props }, ref) => {
  return (
    <RootLayout>
      <Construction />
    </RootLayout>
  )
})

Home.displayName = 'Home'
export default Home
