/**
 * 角色管理 API 模块
 *
 * 封装系统角色的 CRUD 接口，以及角色权限分配接口。
 *
 * RBAC（基于角色的访问控制）数据模型：
 *   用户 (SysUser) ←→ 用户角色关联 (SysUserRole) ←→ 角色 (SysRole)
 *   角色 (SysRole) ←→ 角色权限关联 (SysRolePermission) ←→ 权限 (SysPermission)
 *
 * 接口权限要求：
 *  - getRoles / listAllRoles / getRolePermissions：system:role:query
 *  - createRole：system:role:add
 *  - updateRole：system:role:edit
 *  - deleteRole：system:role:delete
 */

import request from '../utils/request'

/**
 * 分页查询角色列表
 *
 * 请求：GET /api/roles
 * 参数：{ pageNum, pageSize }
 * 响应：R<IPage<SysRole>>
 *   - records：角色列表，每项包含 id, roleName, roleKey, remark, status
 *   - total：总记录数
 *
 * @param {Object} params - 分页参数
 * @returns {Promise}
 */
export const getRoles = (params) => request.get('/roles', {params})

/**
 * 获取所有角色（不分页，用于下拉选择）
 *
 * 请求：GET /api/roles/all
 * 响应：R<List<SysRole>>
 *
 * 使用场景：用户管理页面的"角色"多选框，需要展示完整角色列表供用户选择
 *
 * @returns {Promise}
 */
export const getAllRoles = () => request.get('/roles/all')

/**
 * 获取指定角色已绑定的权限 ID 列表
 *
 * 请求：GET /api/roles/{id}/permissions
 * 响应：R<List<Long>>（权限 ID 数组）
 *
 * 使用场景：打开"编辑角色"对话框时，用于预勾选权限树中已有的权限节点
 *
 * @param {number} id - 角色 ID
 * @returns {Promise}
 */
export const getRolePermissions = (id) => request.get(`/roles/${id}/permissions`)

/**
 * 创建新角色
 *
 * 请求：POST /api/roles
 * 请求体：{
 *   roleName:      角色名称（如"系统管理员"）
 *   roleKey:       角色标识（唯一，如"ADMIN"，用于权限注解中）
 *   remark:        备注说明（可选）
 *   permissionIds: 要授予该角色的权限 ID 列表（含父节点的半选 ID）
 * }
 * 响应：R<Void>
 *
 * @param {Object} data - 角色创建数据
 * @returns {Promise}
 */
export const createRole = (data) => request.post('/roles', data)

/**
 * 更新角色信息及权限
 *
 * 请求：PUT /api/roles/{id}
 * 请求体：同 createRole（roleKey 字段传了但后端不允许修改）
 * 响应：R<Void>
 *
 * 更新逻辑（后端）：
 *  1. 更新角色基本信息
 *  2. 删除该角色的所有旧权限关联
 *  3. 批量插入新的权限关联
 *
 * @param {number} id   - 角色 ID
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export const updateRole = (id, data) => request.put(`/roles/${id}`, data)

/**
 * 删除角色
 *
 * 请求：DELETE /api/roles/{id}
 * 响应：R<Void>
 *
 * 注意：删除角色后，所有持有该角色的用户将失去相应权限（需重新登录生效）
 *
 * @param {number} id - 角色 ID
 * @returns {Promise}
 */
export const deleteRole = (id) => request.delete(`/roles/${id}`)
