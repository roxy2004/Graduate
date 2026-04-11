import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 30000, // 后台接口超时时间
    withCredentials: true
});

// 请求拦截器
request.interceptors.request.use(
    config => {
        // GET/HEAD 不应带 application/json，否则会触发 CORS 预检，部分环境下表现为 Network Error
        const method = (config.method || 'get').toLowerCase();
        const hasBody = config.data !== undefined && config.data !== null && config.data !== '';
        if (!config.headers['Content-Type']) {
            if (method === 'get' || method === 'head') {
                delete config.headers['Content-Type'];
            } else if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
                delete config.headers['Content-Type'];
            } else if (hasBody || method === 'post' || method === 'put' || method === 'patch' || method === 'delete') {
                config.headers['Content-Type'] = 'application/json;charset=utf-8';
            }
        }
        return config;
    },
    error => {
        console.error('请求拦截器错误：', error);
        return Promise.reject(error);
    }
);

// 响应拦截器
request.interceptors.response.use(
    response => {
    let res = response.data;
    // 兼容服务端返回的字符串数据
    if (typeof res === 'string') {
        if (res) {
            try {
                res = JSON.parse(res);
            } catch (e) {
                return Promise.reject({ message: '服务端返回了非 JSON 数据', response });
            }
        }
    }
    return res;
    },
    error => {
    const status = error.response?.status;
    if (status === 401) {
        ElMessage.error('请先登录');
    } else if (status === 403) {
        ElMessage.error('没有操作权限');
    } else if (status === 404) {
        ElMessage.error('未找到请求接口')
    } else if (status === 500) {
        ElMessage.error('系统异常，请查看后端控制台报错')
    } else {
        const msg =
            error.message === 'Network Error'
                ? '网络异常（常见原因：后端未启动、端口不一致、或浏览器跨域拦截）'
                : error.message || '请求失败';
        ElMessage.error(msg);
    }
    return Promise.reject(error)
    }
)
export default request;
