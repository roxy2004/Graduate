<template>
  <div class="manager-layout">
    <div class="manager-header page-shell">
      <div class="header-left">
        <div class="user-chip clickable-chip" @click="goStudentAccount">
          <img v-if="avatarSrc" :src="avatarSrc" alt="avatar" class="avatar-img" />
          <div v-else class="avatar-fallback">{{ avatarText }}</div>
          <div class="user-meta">
            <div class="user-level">{{ userLevel }}</div>
            <div class="user-name">{{ user?.username || "未命名" }}</div>
          </div>
        </div>
        <el-button v-if="user?.role === 'teacher'" text @click="goTeacherUsers">学生管理</el-button>
        <el-button v-if="user?.role === 'teacher'" text @click="goTeacherQuestions">题目管理</el-button>
        <el-button v-if="user?.role === 'teacher'" text @click="goTeacherChapters">章节资料</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentDashboard">学生首页</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentLearning">专项学习</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentPractice">练习中心</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentMistakes">错题本</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentProfile">学习画像</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentRecommendations">预测推荐</el-button>
        <el-button v-if="user?.role === 'student'" text @click="goStudentAccount">个人中心</el-button>
      </div>
      <div class="header-right">
        <el-button text @click="logout">退出</el-button>
      </div>
    </div>
    <div class="manager-content">
      <RouterView />
    </div>
    <StudentDeepSeekChat v-if="user?.role === 'student'" />
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import request from "@/utils/request";
import StudentDeepSeekChat from "@/components/StudentDeepSeekChat.vue";

const router = useRouter();
const user = computed(() => JSON.parse(localStorage.getItem("user") || "null"));
const userLevel = computed(() => {
  const lv = user.value?.level || user.value?.learningLevel || "L2";
  return `等级 ${lv}`;
});
const avatarText = computed(() => {
  const name = (user.value?.username || "ST").toString().trim();
  if (!name) return "ST";
  return (name.slice(0, 2)).toUpperCase();
});
const avatarSrc = computed(() => {
  const raw = (user.value?.avatarUrl || "").toString().trim();
  if (!raw) return "";
  if (/^https?:\/\//i.test(raw)) return raw;
  return `http://localhost:8080${raw.startsWith("/") ? raw : `/${raw}`}`;
});

const goTeacherUsers = async () => {
  await router.push("/manager/teacher/users");
};

const goTeacherQuestions = async () => {
  await router.push("/manager/teacher/questions");
};

const goTeacherChapters = async () => {
  await router.push("/manager/teacher/chapters");
};

const goStudentDashboard = async () => {
  await router.push("/manager/student/dashboard");
};

const goStudentLearning = async () => {
  await router.push("/manager/student/learning");
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

.user-name {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid #dbeafe;
  background: #f8fbff;
}

.clickable-chip {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.clickable-chip:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.14);
}

.avatar-img,
.avatar-fallback {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #1d4ed8;
  background: #e0e7ff;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.user-level {
  font-size: 11px;
  color: #64748b;
}

.manager-content {
  padding: 0;
}
</style>
