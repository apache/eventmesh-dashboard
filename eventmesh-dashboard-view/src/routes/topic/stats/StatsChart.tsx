import React, { forwardRef, useRef, useEffect } from 'react'
import { Box, BoxProps } from '@mui/material'
import echarts from '../../../utils/chart'

interface StatsChartProps extends BoxProps {}

const StatsChart = forwardRef<typeof Box, StatsChartProps>(
  ({ ...props }, ref) => {
    const chartElemRef = useRef(null)
    const chartInsRef = useRef<echarts.ECharts | null>(null)

    useEffect(() => {
      if (!chartInsRef.current) {
        const chartIns = echarts.init(chartElemRef.current)
        const chartOptions = getChartOptions()
        chartIns.setOption(chartOptions)
        chartInsRef.current = chartIns
      }
    }, [])

    return <Box ref={chartElemRef} sx={{ width: 1, height: 1 }}></Box>
  }
)

StatsChart.displayName = 'StatsChart'
export default StatsChart

const getChartOptions = () => {
  return {
    grid: {
      top: 10,
      left: 40,
      right: 10,
      bottom: 20
    },
    xAxis: {
      type: 'category',
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed'
        }
      },
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
    },
    yAxis: {
      type: 'value',
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed'
        }
      }
    },
    series: [
      {
        data: [820, 932, 901, 934, 1290, 1330, 1320],
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: {
          width: 0
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgb(92, 216, 244)'
            },
            {
              offset: 1,
              color: 'rgb(115, 189, 255)'
            }
          ])
        }
      }
    ]
  }
}
