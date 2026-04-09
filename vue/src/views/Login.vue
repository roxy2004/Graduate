<template>
  <div class="login-page">
    <div class="bg-words" aria-hidden="true">
      <span class="bg-word">计算机</span>
      <span class="bg-word">科学</span>
      <span class="bg-word">与</span>
      <span class="bg-word">技术</span>
    </div>
    <div class="login-panel page-shell">
      <div class="login-head">
        <h2 class="page-title">Learning Console</h2>
        <p class="page-desc">请输入账号密码进入系统</p>
      </div>
      <el-form :model="loginForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" @click="handleLogin">登录系统</el-button>
        </el-form-item>
      </el-form>
    </div>
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
        localStorage.setItem("user", JSON.stringify(response.user));//保存用户 ID
      }
      const role = response?.user?.role;
      if (role === "teacher") {
        await router.push("/manager/teacher/users");
      } else if (role === "student") {
        await router.push("/manager/student/dashboard");
      } else {
        await router.push("/manager/student/dashboard");
      }
    } else {
      ElMessage.error(response?.message || "登录失败");
    }
  } catch (error) {
    ElMessage.error("登录失败，请检查网络或联系管理员");
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.login-panel {
  width: 420px;
  padding: 26px 26px 18px;
  position: relative;
  z-index: 2;
}

.login-head {
  margin-bottom: 16px;
}

.login-btn {
  width: 100%;
}

.bg-words {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: flex-start;    /* 改为顶部对齐 */
  gap: 22px;
  pointer-events: none;
  z-index: 1;
  padding-top: 18vh;          /* 距离顶部一定高度，可根据需要微调 */
}

.bg-word {
  font-size: clamp(36px, 6vw, 84px);
  font-weight: 700;
  letter-spacing: 4px;
  color: rgba(37, 99, 235, 0.14);
  text-shadow: 0 10px 22px rgba(30, 64, 175, 0.1);
  opacity: 0;
  transform: translateY(10px);
  animation: wordReveal 1s ease forwards, wordFloat 3.5s ease-in-out infinite;
}

.bg-word:nth-child(1) {
  animation-delay: 0s, 1s;
}

.bg-word:nth-child(2) {
  animation-delay: 0.35s, 1.35s;
}

.bg-word:nth-child(3) {
  animation-delay: 0.7s, 1.7s;
}

.bg-word:nth-child(4) {
  animation-delay: 1.05s, 2.05s;
}

@keyframes wordReveal {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes wordFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}
</style>