import request from '@/utils/request'

export const queryPageApi = (params) => request.get('/attendance', { params })
export const queryByStudentApi = (studentId) => request.get(`/attendance/student/${studentId}`)
export const queryByClazzAndDateApi = (clazzId, date) => request.get(`/attendance/clazz/${clazzId}`, { params: { date } })
export const queryRateApi = (clazzId) => request.get(`/attendance/rate/${clazzId}`)
export const queryClazzListApi = () => request.get('/attendance/clazzList')
export const addApi = (att) => request.post('/attendance', att)
export const updateApi = (att) => request.put('/attendance', att)
export const deleteApi = (id) => request.delete(`/attendance/${id}`)
