import serviceAxios from '@utils/request'
import {ResponseData} from '@types/common'
import {Role, Menu} from '@types/system.ts'
import {API_PREFIX} from '@constants/api-constants'

// 基础路径（如果 SystemConstants.VERSION = "/api"）
const BASE_URL = API_PREFIX.SYSTEM + '/role'

/**
 * 新增或更新角色
 */
export function addOrUpdateRole(data: Role): Promise<ResponseData<void>> {
    return serviceAxios.post(BASE_URL + '/addOrUpdateRole',data)
}

/**
 * 根据 ID 删除角色
 */
export function deleteRole(roleId: number | string): Promise<ResponseData<boolean>> {
    return serviceAxios.delete(BASE_URL + '/delete/' + roleId)
}

/**
 * 根据 ID 获取角色详情 + 权限菜单
 */
export function getRoleById(roleId: number | string): Promise<ResponseData<Role & { menus?: Menu[] }>> {
    return serviceAxios.get(BASE_URL + '/getRole/' + roleId)
}

/**
 * 分页查询角色列表
 */
export function pageRole(params: {
    currentPage: number
    pageSize: number
    roleName?: string
    roleKey?: string
    enabled?: boolean
}): Promise<ResponseData<{ records: Role[]; total: number }>> {
    return serviceAxios.post(BASE_URL + '/pageRole', params)
}

/**
 * 查询角色列表（非分页）
 */
export function queryRole(params: {
    roleName?: string
    roleKey?: string
    enabled?: boolean
}): Promise<ResponseData<Role[]>> {
    return serviceAxios.post(BASE_URL + '/queryRole',params)
}
