import request from '@/utils/request'

export const queryPageApi = (params) => request.get('/scores', { params })
export const queryByStudentApi = (studentId) => request.get(`/scores/student/${studentId}`)
export const queryExamStatsApi = (examId) => request.get(`/scores/exam/${examId}/stats`)
export const queryExamRankingApi = (examId) => request.get(`/scores/exam/${examId}/ranking`)
export const addApi = (score) => request.post('/scores', score)
export const updateApi = (score) => request.put('/scores', score)
export const deleteApi = (id) => request.delete(`/scores/${id}`)
export const deleteBatchApi = (ids) => request.delete(`/scores/batch/${ids}`)
