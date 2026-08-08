import request from '@/utils/request'

//分页条件查询
export const queryPageApi = (page, pageSize) => request.get(`/report/log/page?page=${page}&pageSize=${pageSize}`)
