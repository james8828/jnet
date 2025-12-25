import serviceAxios from '@utils/request'; // 引入封装好的 axios 实例
import {R,Page} from '@types/common.ts'
import {Menu,MenuPageQuery} from '@types/system.ts'
import { API_PREFIX } from '@constants/api-constants';

export async function getMenuInfo(menuId: string): Promise<R<Menu>> {
    return serviceAxios.get(API_PREFIX.SYSTEM + '/menu/' + menuId)
}

export async function pageMenu(page: MenuPageQuery): Promise<Page<Menu>> {
    return serviceAxios.post(API_PREFIX.SYSTEM + '/menu/pageMenu',page)
}

export async function addOrUpdateMenu(menu: Menu): Promise<R<boolean>> {
    return serviceAxios.post(API_PREFIX.SYSTEM + '/menu/addOrUpdateMenu', menu)
}

export async function queryMenu(menu: MenuPageQuery): Promise<R<Menu[]>> {
    return serviceAxios.post(API_PREFIX.SYSTEM + '/menu/queryMenu', menu)
}

export async function deleteMenu(menuId: number): Promise<R<boolean>> {
    return serviceAxios.delete(API_PREFIX.SYSTEM + '/menu/' + menuId)
}

