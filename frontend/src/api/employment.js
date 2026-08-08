import request from '@/utils/request'

export const queryPageApi = (params) => request.get('/employments', { params })
export const queryByStudentApi = (studentId) => request.get(`/employments/student/${studentId}`)
export const queryClassStatsApi = (clazzId) => request.get(`/employments/class/${clazzId}/stats`)
export const queryAllStatsApi = () => request.get('/employments/stats')
export const addApi = (emp) => request.post('/employments', emp)
export const updateApi = (emp) => request.put('/employments', emp)
export const deleteApi = (id) => request.delete(`/employments/${id}`)
export const deleteBatchApi = (ids) => request.delete(`/employments/batch/${ids}`)
