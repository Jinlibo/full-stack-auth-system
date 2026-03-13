/**
 * 权限管理 API 模块
 *
 * 封装系统权限的 CRUD 接口。
 *
 * 权限节点类型说明：
 *  - type=1（菜单）：对应前端路由和侧边栏导航菜单项
 *  - type=2（按钮）：对应页面内的操作按钮（如"新增用户"按钮）
 *  - type=3（API）：对应后端接口访问权限，与 @PreAuthorize 注解中的标识对应
 *
 * 权限树结构（递归嵌套）：
 *  {
 *    id, permissionName, permissionKey, parentId, type, path, icon, sortOrder,
 *    children: [ ... ]  // 子权限节点（只有非叶子节点才有此字段）
 *  }
 *
 * 接口权限要求：
 *  - getPermissionTree：公开（无需特定权限，角色分配对话框中也需要用到）
 *  - createPermission / updatePermission / deletePermission：system:permission
 */

import request from '../utils/request'

/**
 * 获取完整权限树（用于展示和分配）
 *
 * 请求：GET /api/permissions/tree
 * 响应：R<List<Map<String, Object>>>
 *   返回树形结构，已按 sortOrder 字段排序
 *   顶级节点的 parentId 为 0
 *
 * 使用场景：
 *  1. 权限管理页面：树形表格展示所有权限
 *  2. 角色编辑对话框：权限树复选框（el-tree），允许为角色分配权限
 *
 * @returns {Promise}
 */
export const getPermissionTree = () => request.get('/permissions/tree')

/**
 * 创建权限节点
 *
 * 请求：POST /api/permissions
 * 请求体：{
 *   permissionName:  权限名称（用于展示，如"用户列表"）
 *   permissionKey:   权限标识（全局唯一，如"system:user:query"，用于 @PreAuthorize）
 *   parentId:        父权限 ID（0 表示顶级权限）
 *   type:            权限类型（1=菜单，2=按钮，3=API）
 *   path:            路由路径或接口路径（可选）
 *   icon:            图标名称（可选，用于菜单图标展示）
 *   sortOrder:       排序号（同层级节点中数值越小越靠前）
 * }
 * 响应：R<Void>
 *
 * 后端校验：permissionKey 不允许重复
 *
 * @param {Object} data - 权限创建数据
 * @returns {Promise}
 */
export const createPermission = (data) => request.post('/permissions', data)

/**
 * 更新权限节点信息
 *
 * 请求：PUT /api/permissions/{id}
 * 请求体：同 createPermission（所有字段均可修改）
 * 响应：R<Void>
 *
 * 注意：修改 permissionKey 会影响所有通过 @PreAuthorize 引用了该标识的接口，
 *       以及角色的权限关联记录，谨慎操作
 *
 * @param {number} id   - 权限节点 ID
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export const updatePermission = (id, data) => request.put(`/permissions/${id}`, data)

/**
 * 删除权限节点
 *
 * 请求：DELETE /api/permissions/{id}
 * 响应：R<Void>
 *
 * 后端保护：
 *  - 若该节点下存在子权限（parentId = 当前 id），拒绝删除并返回错误
 *  - 需先删除所有子权限，才能删除父权限节点
 *
 * @param {number} id - 权限节点 ID
 * @returns {Promise}
 */
export const deletePermission = (id) => request.delete(`/permissions/${id}`)
