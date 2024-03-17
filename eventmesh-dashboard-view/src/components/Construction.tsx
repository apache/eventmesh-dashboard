import React, { forwardRef } from 'react'
import { Box, BoxProps, Typography } from '@mui/material'
import { Icons } from '../assets/icons'
import { grey } from '@mui/material/colors'

interface ConstructionProps extends BoxProps {}

const Construction = forwardRef<typeof Box, ConstructionProps>(
  ({ ...props }, ref) => {
    return (
      <Box
        ref={ref}
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          flexDirection: 'column',
          color: grey[500]
        }}>
        <Icons.Foundation fontSize="large" />
        <Typography paragraph>Under constraction</Typography>
      </Box>
    )
  }
)

Construction.displayName = 'Construction'
export default Construction
