import request from '@/utils/request'

export const queryPageApi = (params) => request.get('/payments', { params })
export const queryByStudentApi = (studentId) => request.get(`/payments/student/${studentId}`)
export const queryByStatusApi = (status) => request.get(`/payments/status/${status}`)
export const addApi = (payment) => request.post('/payments', payment)
export const updateApi = (payment) => request.put('/payments', payment)
export const deleteApi = (id) => request.delete(`/payments/${id}`)
export const deleteBatchApi = (ids) => request.delete(`/payments/batch/${ids}`)
