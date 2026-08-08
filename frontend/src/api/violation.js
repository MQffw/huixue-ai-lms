import request from '@/utils/request'

export const queryPageApi = (params) => request.get('/violations', { params })
export const queryByStudentApi = (studentId) => request.get(`/violations/student/${studentId}`)
export const queryRecentApi = (days = 30) => request.get('/violations/recent', { params: { days } })
export const addApi = (v) => request.post('/violations', v)
export const updateApi = (v) => request.put('/violations', v)
export const deleteApi = (id) => request.delete(`/violations/${id}`)
export const deleteBatchApi = (ids) => request.delete(`/violations/batch/${ids}`)
