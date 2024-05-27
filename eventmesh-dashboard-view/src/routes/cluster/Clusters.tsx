import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import RootLayout from '../../components/page/RootLayout'

interface ClustersProps extends BoxProps {}

const Clusters = forwardRef<typeof Box, ClustersProps>(({ ...props }, ref) => {
  return <RootLayout>Clusters</RootLayout>
})

Clusters.displayName = 'Clusters'
export default Clusters
