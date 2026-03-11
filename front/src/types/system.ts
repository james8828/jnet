import { PageQuery } from '@types/common'

export interface MenuPageQuery extends PageQuery {
    menuName: string
    path: string
    component: string
    visible: boolean
    enabled: boolean
    perms: string
    icon: string
    remark: string
    type: string
    parentId: number
    roleIds: number[]
}

export type OptionItem = {
    key?: any,
    value: any,
    label: string,
    data?: any
} & Record<string, any>

/**
 * 部门
 */
export type Department = {
    deptId: number,
    deptName: string,
    email: string,
    phone: string,
    ancestors: string,
    leader: string,
    status: string
}

/**
 * 用户
 */
export type User = {
    userId: number,
    userName: string,
    admin: boolean,
    status: string,
    sex: string,
    email: string,
    phonenumber: string,
    loginIp: string,
    loginDate: string,
    avatar: string,
    nickName: string,
    deptId: number,
    dept: Department | void
}


/**
 * 菜单路由配置项
 */
export type RouterMenuConfig = {
    menuId: any,
    perms: string,
    parentId: number,
    menuName: string,
    menuNameEn: string,
    name: string,
    path: string,
    hidden: boolean,
    redirect: string,
    component: any,
    alwaysShow: boolean,
    meta: {
        title: string,
        icon: string,
        noCache: boolean,
        link: null,
        perms: string,
        fullWidth: boolean,
        noFit: boolean,
        noHeader: boolean,

        menuId: any,
        parentId: any,

        // 完整路径
        fullPath: string,
        // 当前节点的深度（第几层children中），路径可能出现空路径的情况，一般用作将部分路由分组，
        // 需要根据路径和深度判断层级位置
        nodeDepth: number
    },
    children: RouterMenuConfig[]
}

// 文件建议路径：src/types/system/menu.ts 或 @/types/system/menu.ts

export type Menu = {
    menuId?: number | null;        // 菜单ID（可为空，新增时不需要）
    parentId?: number | null;
    type?: number | null;
    orderNum?: number | null;
    menuName: string;             // 菜单名称（必填）
    path: string;                 // 路由地址（如 /system/user）
    component: string;            // 组件路径（如 system/user/index.vue）
    visible: boolean;             // 是否显示在菜单栏
    enabled: boolean;             // 启用状态（0正常 1停用）
    perms?: string | null;         // 权限标识（如 system:user:list）
    icon?: string | null;          // 图标类名或URL
    remark?: string | null;        // 备注信息
    createBy?: number | null;      // 创建人ID
    createTime?: string | null;    // 创建时间（ISO格式字符串）
    updateBy?: number | null;      // 更新人ID
    updateTime?: string | null;    // 更新时间（ISO格式字符串）
}

export interface Role {
    roleId?: number
    roleName?: string
    roleKey?: string
    enabled?: boolean
    delFlag?: boolean
    remark?: string
    menus?: Menu[]
    createTime?: string
    updateTime?: string
}


export interface RolePageQuery {
    currentPage: number
    pageSize: number
    roleName?: string
    roleKey?: string
    enabled?: boolean
}



