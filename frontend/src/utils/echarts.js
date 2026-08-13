// ECharts 配置（单色系：靛蓝 indigo 为主，中性灰为辅，状态色克制）
export const accent = '#4f46e5'
export const accentStrong = '#6366f1'

export const semantic = {
  ok: '#16a34a',
  warn: '#d97706',
  danger: '#dc2626',
  neutral: '#a1a1aa'
}

// 单色系阶梯（indigo 同色相，由亮到暗）+ 少量中性灰
export const ramp = [
  '#818cf8', '#6366f1', '#4f46e5', '#4338ca', '#3730a3',
  '#a1a1aa', '#d4d4d8', '#16a34a', '#d97706', '#dc2626'
]

export const statusColor = {
  RUNNING: '#16a34a',
  FAULT: '#dc2626',
  REPAIRING: '#d97706',
  STOPPED: '#a1a1aa',
  SCRAPPED: '#d4d4d8'
}

export const typeColor = {
  ELEVATOR: '#4f46e5',
  FIRE: '#4338ca',
  PUMP: '#6366f1',
  ACCESS: '#818cf8',
  OTHER: '#a1a1aa'
}

export const axisStyle = {
  axisLine: { lineStyle: { color: '#e3e3e7' } },
  axisTick: { show: false },
  axisLabel: { color: '#9d9da8', fontSize: 11 },
  splitLine: { lineStyle: { color: 'rgba(24,24,27,.05)' } }
}

export const tooltipStyle = {
  backgroundColor: '#ffffff',
  borderColor: '#e3e3e7',
  borderWidth: 1,
  textStyle: { color: '#18181b', fontSize: 12 },
  extraCssText: 'border-radius:8px;box-shadow:0 8px 24px rgba(24,24,27,.1);'
}

// 极淡面积渐变
export const areaGradient = (color) => ({
  type: 'linear',
  x: 0, y: 0, x2: 0, y2: 1,
  colorStops: [
    { offset: 0, color },
    { offset: 1, color: 'rgba(255,255,255,0)' }
  ]
})
