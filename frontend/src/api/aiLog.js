import request from '@/utils/request'

export const pageAiLogsApi = (params) => request.get('/ai-logs', { params })
export const aiLogDetailApi = (traceId) => request.get(`/ai-logs/${traceId}`)
