import React, { forwardRef, useRef, useEffect } from 'react'
import { Box, BoxProps, Paper } from '@mui/material'
import echarts from '../../../utils/chart'
import { grey } from '@mui/material/colors'

interface StatsChartProps extends BoxProps {
  title?: string
  small?: boolean
}

const StatsChart = forwardRef<typeof Box, StatsChartProps>(
  ({ title, small = false, sx, ...props }, ref) => {
    const chartElemRef = useRef(null)
    const chartInsRef = useRef<echarts.ECharts | null>(null)

    useEffect(() => {
      if (!chartInsRef.current) {
        const chartIns = echarts.init(chartElemRef.current)
        const chartOptions = small
          ? getChartOptionsSmall(title)
          : getChartOptions(title)
        chartIns.setOption(chartOptions)
        chartInsRef.current = chartIns
      }
    }, [])

    return (
      <Paper
        sx={{
          ...sx,
          width: 1,
          height: 1,
          borderRadius: 4,
          boxShadow: '2px 2px 40px 2px rgba(0,0,0,.05)'
        }}>
        <Box ref={chartElemRef} sx={{ width: 1, height: 1 }}></Box>
      </Paper>
    )
  }
)

StatsChart.displayName = 'StatsChart'
export default StatsChart

const getChartOptionsSmall = (title?: string) => {
  return {
    title: {
      text: title,
      top: 10,
      left: 10,
      textStyle: {
        fontSize: 12,
        color: grey[600]
      }
    },
    grid: {
      top: 50,
      left: 50,
      right: 20,
      bottom: 30
    },
    xAxis: {
      type: 'category',
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed',
          color: grey[200]
        }
      },
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
    },
    yAxis: {
      type: 'value',
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed',
          color: grey[200]
        }
      },
      axisTick: {
        color: grey[200]
      }
    },
    series: [
      {
        data: [20, 32, 51, 934, 190, 130, 320],
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: {
          width: 2,
          color: '#17c8eb'
        }
      }
    ]
  }
}

const getChartOptions = (title?: string) => {
  return {
    title: {
      text: title,
      top: 15,
      left: 10,
      textStyle: {
        fontSize: 12,
        color: grey[600]
      }
    },
    grid: {
      top: 60,
      left: 60,
      right: 40,
      bottom: 40
    },
    xAxis: {
      type: 'category',
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed',
          color: grey[200]
        }
      },
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
    },
    yAxis: {
      type: 'value',
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed',
          color: grey[200]
        }
      },
      axisTick: {
        color: grey[200]
      }
    },
    series: [
      {
        data: [20, 32, 51, 934, 190, 130, 320],
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: {
          width: 2,
          color: '#17c8eb'
        }
      }
    ]
  }
}
