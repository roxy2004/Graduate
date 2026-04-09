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
        // 设置默认请求头，上传文件时保留 multipart/form-data
        if (!config.headers['Content-Type']) {
            config.headers['Content-Type'] = 'application/json;charset=utf-8';
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
    if ( typeof res === 'string') {
        res = res ? JSON.parse(res) : res
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
        ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
    }
)
export default request;
