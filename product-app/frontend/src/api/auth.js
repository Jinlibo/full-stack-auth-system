import request from '../utils/request'

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getOAuthUrl = (state) => request.get('/auth/oauth2/authorize-url', {params: {state}})
export const oauthCallback = (data) => request.post('/auth/oauth2/callback', data)
export const oauthCreateNew = (data) => request.post('/auth/oauth2/create-new', data)
export const oauthBindExisting = (data) => request.post('/auth/oauth2/bind-existing', data)
export const bindOAuth = (data) => request.post('/users/me/oauth/bind', data)
export const logout = () => request.post('/auth/logout')
export const getCurrentUser = () => request.get('/users/me')
