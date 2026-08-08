import request from '@/utils/request'

export const getDashboardApi = (params) => request.get('/ai-stats/dashboard', { params })
export const getDailyApi = (params) => request.get('/ai-stats/daily', { params })
export const getTokenSummaryApi = () => request.get('/ai-stats/tokens/summary')
export const getToolStatsApi = (days = 7) => request.get('/ai-stats/tools', { params: { days } })
