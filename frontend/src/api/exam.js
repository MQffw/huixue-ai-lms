import request from '@/utils/request'

export const queryAllApi = () => request.get('/exams')
export const queryByClazzApi = (clazzId) => request.get(`/exams/clazz/${clazzId}`)
