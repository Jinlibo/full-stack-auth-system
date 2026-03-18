import request from '../utils/request'

export const getUsers = (params) => request.get('/users', {params})
export const getUser = (id) => request.get(`/users/${id}`)
export const updateProfile = (data) => request.put('/users/me/profile', data)
export const unbindOAuth = (provider) => request.delete(`/users/me/oauth/${provider}`)
export const setPassword = (data) => request.put('/users/me/password', data)
