<template>
  <div class="manager-layout">
    <div class="manager-header">
      <div class="header-left">
        <span class="title">学习系统</span>
        <el-button text @click="goHome">首页</el-button>
        <el-button v-if="user?.role === 'teacher'" text @click="goTeacherUsers">学生管理</el-button>
        <el-button v-if="user?.role === 'teacher'" text @click="goTeacherQuestions">题目管理</el-button>
      </div>
      <div class="header-right">
        <span class="user-name">{{ user?.username }} ({{ user?.role }})</span>
        <el-button text @click="logout">退出</el-button>
      </div>
    </div>
    <div class="manager-content">
      <RouterView />
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import request from "@/utils/request";

const router = useRouter();
const user = computed(() => JSON.parse(localStorage.getItem("user") || "null"));

const goHome = async () => {
  await router.push("/manager/home");
};

const goTeacherUsers = async () => {
  await router.push("/manager/teacher/users");
};

const goTeacherQuestions = async () => {
  await router.push("/manager/teacher/questions");
};

const logout = async () => {
  try {
    await request.post("/xwd/logout");
  } finally {
    localStorage.removeItem("user");
    await router.push("/login");
  }
};
</script>

<style scoped>
.manager-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.manager-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #409eff;
  color: #fff;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  margin-right: 8px;
}

.user-name {
  font-size: 14px;
}

.manager-content {
  padding: 20px;
}
</style>
