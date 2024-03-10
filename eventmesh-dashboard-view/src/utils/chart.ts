import * as echarts from 'echarts/core'

import { LineChart } from 'echarts/charts'
import {
    PolarComponent,
    TitleComponent,
    TooltipComponent,
    GridComponent,
    MarkAreaComponent,
    MarkLineComponent,
    ToolboxComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
    LineChart,
    PolarComponent,
    TitleComponent,
    GridComponent,
    TitleComponent,
    TooltipComponent,
    MarkLineComponent,
    MarkAreaComponent,
    ToolboxComponent,
    CanvasRenderer
])

export default echarts