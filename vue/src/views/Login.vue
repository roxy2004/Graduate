<template>
  <div>
    <h2>登录</h2>
    <el-form :model="loginForm" label-width="80px">
      <el-form-item label="用户名">
        <el-input v-model="loginForm.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleLogin">登录</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import { useRouter } from "vue-router";
import request from "../utils/request";
import { ElMessage } from "element-plus";

const router = useRouter();

const loginForm = reactive({
  username: "",
  password: "",
});

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning("请输入用户名和密码");
    return;
  }

  try {
    const response = await request.post("/xwd/login", loginForm);
    if (response?.status === "success") {
      ElMessage.success(response.message || "登录成功");
      if (response.user) {
        localStorage.setItem("user", JSON.stringify(response.user));
      }
      const role = response?.user?.role;
      if (role === "teacher") {
        await router.push("/manager/teacher/users");
      } else {
        await router.push("/manager/home");
      }
    } else {
      ElMessage.error(response?.message || "登录失败");
    }
  } catch (error) {
    ElMessage.error("登录失败，请检查网络或联系管理员");
  }
};
</script>