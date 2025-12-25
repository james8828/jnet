import serviceAxios from '@utils/request'; // 引入封装好的 axios 实例
import {ResponseData} from '@types/common'
import {API_PREFIX} from '@constants/api-constants';
import {User} from '@types/system.ts';

export async function getUserInfo(userId: any): Promise<ResponseData<void>> {
    return serviceAxios.get(API_PREFIX.SYSTEM + '/user/' + userId)
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