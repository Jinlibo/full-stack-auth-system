import request from '../utils/request'

export const getUsers = (params) => request.get('/users', {params})
export const getUser = (id) => request.get(`/users/${id}`)
export const createUser = (data) => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const deleteUser = (id) => request.delete(`/users/${id}`)
export const changePassword = (data) => request.put('/users/me/password', data)
export const updateProfile = (data) => request.put('/users/me/profile', data)
