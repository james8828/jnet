import serviceAxios from '@utils/request'; // 引入封装好的 axios 实例
import {R} from '@types/common'
import {API_PREFIX} from '@constants/api-constants';
import {User} from '@types/system.ts';

export async function getUserInfo(userId: string | number): Promise<R<void>> {
    // 输入验证
    if (userId === null || userId === undefined || userId === '') {
        throw new Error('userId cannot be null, undefined, or empty');
    }

    // 类型验证和路径安全处理
    const validatedUserId = String(userId);

    // 防止路径遍历攻击
    if (validatedUserId.includes('..') || validatedUserId.includes('/')) {
        throw new Error('Invalid userId format');
    }

    try {
        return await serviceAxios.get(API_PREFIX.SYSTEM + '/user/' + encodeURIComponent(validatedUserId));
    } catch (error) {
        console.error('Error fetching user info:', error);
        throw error;
    }
}


export async function pageUser(page: any): Promise<ResponseData<void>> {
    // return serviceAxios.post(API_PREFIX.SYSTEM + '/user/pageUser', {"size": page.pageSize, "current": page.currentPage})
    return serviceAxios.post(API_PREFIX.SYSTEM + '/user/pageUser', page)
}

export async function addOrUpdateUser(user: User): Promise<ResponseData<void>> {
    return serviceAxios.post(API_PREFIX.SYSTEM + '/user/addOrUpdateUser', user)
}

export async function deleteUser(userId: any): Promise<ResponseData<void>> {
    return serviceAxios.delete(API_PREFIX.SYSTEM + '/user/' + userId)
}