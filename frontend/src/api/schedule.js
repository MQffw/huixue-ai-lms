import request from '@/utils/request'

export const listScheduleApi = (params) => request.get('/schedules', { params })
export const scheduleByClazzApi = (clazzId) => request.get(`/schedules/clazz/${clazzId}`)
