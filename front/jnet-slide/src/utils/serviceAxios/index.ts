import axios from 'axios';

// 创建 axios 请求实例
const serviceAxios = axios.create({
    //baseURL: process.env.API_BASE_URL || '', // 使用环境变量设置基础请求地址
    baseURL: '', // 使用环境变量设置基础请求地址
    timeout: 10000, // 请求超时设置
    withCredentials: false, // 跨域请求是否需要携带 cookie
});

// 创建请求拦截
serviceAxios.interceptors.request.use(
    (config) => {
        // 设置请求头
        if (!config.headers?.['Content-Type']) {
            config.headers['Content-Type'] = 'application/json';
        }

        // 其他请求预处理逻辑
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// 创建响应拦截
serviceAxios.interceptors.response.use(
    (response) => {
        const { data } = response;
        // 处理自己的业务逻辑，比如判断 token 是否过期等等
        return data;
    },
    (error) => {
        const message = getErrorMessage(error);
        return Promise.reject(message);
    }
);

function getErrorMessage(error: any): string {
    if (error && error.response) {
        switch (error.response.status) {
            case 302:
                return "接口重定向了！";
            case 400:
                return "参数不正确！";
            case 401:
                return "您未登录，或者登录已经超时，请先登录！";
            case 403:
                return "您没有权限操作！";
            case 404:
                return `请求地址出错: ${error.response.config.url}`;
            case 408:
                return "请求超时！";
            case 409:
                return "系统已存在相同数据！";
            case 500:
                return "服务器内部错误！";
            case 501:
                return "服务未实现！";
            case 502:
                return "网关错误！";
            case 503:
                return "服务不可用！";
            case 504:
                return "服务暂时无法访问，请稍后再试！";
            case 505:
                return "HTTP 版本不受支持！";
            default:
                return "异常问题，请联系管理员！";
        }
    }
    return "未知错误！";
}

export default serviceAxios;
