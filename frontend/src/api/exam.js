import request from '@/utils/request'

export const listExamsApi = () => request.get('/exams')
export const getExamApi = (id) => request.get(`/exams/${id}`)
export const examsByClazzApi = (clazzId) => request.get(`/exams/clazz/${clazzId}`)
export const addExamApi = (data) => request.post('/exams', data)
export const updateExamApi = (data) => request.put('/exams', data)
export const deleteExamApi = (id) => request.delete(`/exams/${id}`)

// 兼容旧调用（成绩页等仍用 queryAllApi / queryInfoApi）
export const queryAllApi = listExamsApi
export const queryInfoApi = getExamApi
export const queryByClazzApi = examsByClazzApi
