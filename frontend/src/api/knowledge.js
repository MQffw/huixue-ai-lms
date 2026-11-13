import request from '@/utils/request'

export const listDocumentsApi = () => request.get('/knowledge/documents')

export const uploadDocumentApi = (formData) => request.post('/knowledge/upload', formData)

export const deleteDocumentApi = (id) => request.delete(`/knowledge/documents/${id}`)

export const searchKnowledgeApi = (q) => request.get('/knowledge/search', { params: { q } })

export const knowledgeStatusApi = () => request.get('/knowledge/status')
