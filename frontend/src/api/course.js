import request from '@/utils/request'

export const queryPageApi = (name, subject, page, pageSize) =>
  request.get('/courses', { params: { name, subject, page, pageSize } })
export const queryInfoApi = (id) => request.get(`/courses/${id}`)
export const addApi = (course) => request.post('/courses', course)
export const updateApi = (course) => request.put('/courses', course)
export const deleteApi = (id) => request.delete(`/courses/${id}`)
export const deleteBatchApi = (ids) => request.delete(`/courses/batch/${ids}`)
export const queryAllApi = () => request.get('/courses/list')
export const queryBySubjectApi = (subject) => request.get(`/courses/subject/${subject}`)
