import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'

interface PageProps extends BoxProps {}

const Page = forwardRef<typeof Box, PageProps>(
  ({ children, ...props }, ref) => {
    return (
      <Box
        ref={ref}
        sx={{
          width: 1,
          height: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
        {children}
      </Box>
    )
  }
)

Page.displayName = 'Page'
export default Page
