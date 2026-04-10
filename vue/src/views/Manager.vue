<template>
  <div class="manager-layout">
    <div class="manager-header page-shell">
      <div class="header-left">
        <span class="title">&gt;_ Learning System</span>
        <el-button v-if="user?.role === 'teacher'" text @click="goTeacherUsers">学生管理</el-button>
        <el-button v-if="user?.role === 'teacher'" text @click="goTeacherQuestions">题目管理</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentDashboard">学生首页</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentPractice">练习中心</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentMistakes">错题本</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentProfile">学习画像</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentRecommendations">预测推荐</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentAccount">个人中心</el-button>
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

const goTeacherUsers = async () => {
  await router.push("/manager/teacher/users");
};

const goTeacherQuestions = async () => {
  await router.push("/manager/teacher/questions");
};

const goStudentDashboard = async () => {
  await router.push("/manager/student/dashboard");
};

const goStudentPractice = async () => {
  await router.push("/manager/student/practice");
};

const goStudentMistakes = async () => {
  await router.push("/manager/student/mistakes");
};

const goStudentProfile = async () => {
  await router.push("/manager/student/profile");
};

const goStudentRecommendations = async () => {
  await router.push("/manager/student/recommendations");
};

const goStudentAccount = async () => {
  await router.push("/manager/student/account");
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
  padding: 14px;
}

.manager-header {
  min-height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  margin-bottom: 14px;
  background: rgba(255, 255, 255, 0.94);
  color: #1e293b;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title {
  font-size: 17px;
  font-weight: 600;
  margin-right: 8px;
  color: #2563eb;
}

.user-name {
  font-size: 14px;
  color: #64748b;
}

.manager-content {
  padding: 0;
}
</style>
