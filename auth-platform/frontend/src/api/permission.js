import request from '../utils/request'

export const getPermissionTree = () => request.get('/permissions/tree')
export const createPermission = (data) => request.post('/permissions', data)
export const updatePermission = (id, data) => request.put(`/permissions/${id}`, data)
export const deletePermission = (id) => request.delete(`/permissions/${id}`)
