import request from '@/utils/request'

export const queryPageApi = (params) => request.get('/notices', { params })
export const queryLatestApi = (limit = 10) => request.get('/notices/latest', { params: { limit } })
export const queryByTypeApi = (type) => request.get(`/notices/type/${type}`)
export const searchApi = (keyword) => request.get('/notices/search', { params: { keyword } })
export const queryInfoApi = (id) => request.get(`/notices/${id}`)
export const addApi = (notice) => request.post('/notices', notice)
export const updateApi = (notice) => request.put('/notices', notice)
export const deleteApi = (id) => request.delete(`/notices/${id}`)
export const deleteBatchApi = (ids) => request.delete(`/notices/batch/${ids}`)
