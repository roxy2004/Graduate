<template>
  <div class="student-page page-shell">
    <section class="profile-hero">
      <div class="avatar-wrap">
        <img v-if="avatarSrc" :src="avatarSrc" alt="avatar" class="avatar-img" />
        <div v-else class="avatar">{{ avatarText }}</div>
        <input ref="fileInputRef" type="file" accept=".png,.jpg,.jpeg,.webp" class="file-hidden" @change="onPickAvatar" />
        <el-button size="small" class="avatar-btn" @click="triggerPickAvatar">更换头像</el-button>
      </div>
      <div class="hero-main">
        <h2 class="page-title">个人中心</h2>
        <p class="page-desc">管理账号信息与学习偏好，支持修改密码与头像。</p>
        <div class="tags">
          <span class="tag">学生账号</span>
          <span class="tag tag-online">账号在线</span>
        </div>
      </div>
      <div class="hero-side">
        <div class="hero-k">用户名</div>
        <div class="hero-v">{{ profile.username || "--" }}</div>
      </div>
    </section>

    <div class="account-grid">
      <div class="kv">
        <span>账号状态</span>
        <strong class="ok">在线</strong>
      </div>
      <div class="kv">
        <span>账号角色</span>
        <strong>{{ profile.role || "student" }}</strong>
      </div>
      <div class="kv">
        <span>学习偏好</span>
        <strong>每日推荐练习</strong>
      </div>
    </div>

    <section class="quick-panel">
      <div class="quick-title">修改密码</div>
      <div class="password-form">
        <el-input v-model="passwordForm.oldPassword" show-password placeholder="请输入旧密码" />
        <el-input v-model="passwordForm.newPassword" show-password placeholder="请输入新密码" />
        <el-input v-model="passwordForm.confirmPassword" show-password placeholder="请再次输入新密码" />
      </div>
      <div class="quick-actions">
        <el-button type="primary" :loading="changingPassword" @click="changePassword">保存新密码</el-button>
      </div>
    </section>

    <section class="quick-panel">
      <div class="quick-title">常用操作</div>
      <div class="quick-actions">
        <el-button type="primary" plain @click="router.push('/manager/student/practice/daily')">去做每日训练</el-button>
        <el-button type="primary" plain @click="router.push('/manager/student/mistakes')">查看错题本</el-button>
        <el-button type="primary" plain @click="router.push('/manager/student/profile')">学习者画像</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const router = useRouter();
const fileInputRef = ref(null);
const changingPassword = ref(false);
const avatarVersion = ref(Date.now());
const profile = reactive({
  id: null,
  username: "",
  role: "student",
  avatarUrl: "",
});
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const avatarText = computed(() => {
  const name = (profile.username || "ST").toString().trim();
  if (!name) return "ST";
  const first = name[0];
  const second = name.length > 1 ? name[1] : "";
  return (first + second).toUpperCase();
});

const avatarSrc = computed(() => {
  const raw = (profile.avatarUrl || "").trim();
  if (!raw) return "";
  const base = /^https?:\/\//i.test(raw)
    ? raw
    : `http://localhost:8080${raw.startsWith("/") ? raw : `/${raw}`}`;
  const sep = base.includes("?") ? "&" : "?";
  return `${base}${sep}v=${avatarVersion.value}`;
});

const loadProfile = async () => {
  try {
    const resp = await request.get("/xwd/student/account/profile");
    if (resp?.status === "success") {
      const d = resp.data || {};
      profile.id = d.id ?? null;
      profile.username = d.username || "";
      profile.role = d.role || "student";
      profile.avatarUrl = d.avatarUrl || "";
      avatarVersion.value = Date.now();
      const raw = localStorage.getItem("user");
      if (raw) {
        try {
          const u = JSON.parse(raw);
          u.avatarUrl = profile.avatarUrl;
          u.username = profile.username || u.username;
          localStorage.setItem("user", JSON.stringify(u));
        } catch {
          // ignore
        }
      }
    } else {
      ElMessage.error(resp?.message || "加载个人信息失败");
    }
  } catch {
    ElMessage.error("加载个人信息失败");
  }
};

const triggerPickAvatar = () => {
  fileInputRef.value?.click();
};

const onPickAvatar = async (e) => {
  const file = e?.target?.files?.[0];
  if (!file) return;
  const fd = new FormData();
  fd.append("file", file);
  try {
    const resp = await request.post("/xwd/student/account/avatar", fd, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    if (resp?.status === "success") {
      ElMessage.success(resp?.message || "头像更新成功");
      profile.avatarUrl = resp.avatarUrl || "";
      avatarVersion.value = Date.now();
      await loadProfile();
    } else {
      ElMessage.error(resp?.message || "头像上传失败");
    }
  } catch {
    ElMessage.error("头像上传失败");
  } finally {
    if (fileInputRef.value) fileInputRef.value.value = "";
  }
};

const changePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning("请完整填写密码信息");
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning("两次输入的新密码不一致");
    return;
  }
  changingPassword.value = true;
  try {
    const resp = await request.post("/xwd/student/account/password", {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    });
    if (resp?.status === "success") {
      ElMessage.success(resp?.message || "密码修改成功");
      passwordForm.oldPassword = "";
      passwordForm.newPassword = "";
      passwordForm.confirmPassword = "";
    } else {
      ElMessage.error(resp?.message || "密码修改失败");
    }
  } catch {
    ElMessage.error("密码修改失败");
  } finally {
    changingPassword.value = false;
  }
};

onMounted(() => {
  loadProfile();
});
</script>

<style scoped>
.student-page {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.file-hidden {
  display: none;
}

.avatar-btn {
  border-radius: 999px;
}

.profile-hero {
  display: flex;
  gap: 14px;
  align-items: center;
  border-radius: 14px;
  padding: 16px;
  background: linear-gradient(140deg, #eef2ff 0%, #eff6ff 45%, #ffffff 100%);
  border: 1px solid #dbeafe;
  box-shadow: 0 10px 28px rgba(30, 64, 175, 0.08);
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 800;
  color: #1e3a8a;
  background: #ffffff;
  border: 1px solid #c7d2fe;
}

.avatar-img {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  border: 1px solid #c7d2fe;
  object-fit: cover;
  background: #fff;
}

.hero-main {
  flex: 1;
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.page-desc {
  margin: 6px 0 0;
  color: #64748b;
}

.tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #1d4ed8;
  background: #e0e7ff;
}

.tag-online {
  color: #065f46;
  background: #d1fae5;
}

.hero-side {
  text-align: right;
}

.hero-k {
  color: #64748b;
  font-size: 12px;
}

.hero-v {
  margin-top: 4px;
  color: #1d4ed8;
  font-size: 22px;
  font-weight: 800;
}

.account-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.kv {
  border: 1px solid #d8e1f0;
  border-radius: 12px;
  padding: 14px;
  background: #f8fbff;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kv span {
  color: #64748b;
  font-size: 13px;
}

.kv strong {
  color: #1d4ed8;
}

.kv .ok {
  color: #0f766e;
}

.quick-panel {
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  padding: 12px 14px;
  background: #fafcff;
}

.quick-title {
  font-weight: 700;
  color: #334155;
  margin-bottom: 10px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.password-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

@media (max-width: 960px) {
  .profile-hero {
    flex-wrap: wrap;
    align-items: flex-start;
  }
  .hero-side {
    text-align: left;
  }
  .account-grid {
    grid-template-columns: 1fr;
  }
  .password-form {
    grid-template-columns: 1fr;
  }
}
</style>
