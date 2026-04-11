<template>
  <div class="student-page page-shell">
    <h2 class="page-title">练习中心</h2>
    <p class="page-desc">按知识点刷题；未做过的题目优先。进入练习后将以卡片形式作答。</p>
    <div class="toolbar">
      <el-button type="primary" plain @click="loadList">刷新</el-button>
    </div>

    <div v-if="loading" class="placeholder">加载中…</div>
    <div v-else-if="list.length === 0" class="placeholder">暂无带题目的知识点，请教师导入题目并关联知识点。</div>
    <div v-else class="kp-grid">
      <div v-for="row in list" :key="rowKey(row)" class="kp-card">
        <div class="kp-name">{{ row.name || "未命名" }}</div>
        <div class="kp-meta">
          已刷 <strong>{{ num(row.practicedQuestions ?? row.practiced_questions) }}</strong> /
          共 <strong>{{ num(row.totalQuestions ?? row.total_questions) }}</strong> 题
        </div>
        <el-progress
          :percentage="progressPct(row)"
          :stroke-width="10"
          :show-text="false"
          class="kp-bar"
        />
        <el-button type="primary" plain class="kp-go" @click="goPractice(row)">卡片练习</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const router = useRouter();
const list = ref([]);
const loading = ref(false);

const rowKey = (row) => row.id ?? row.ID;
const num = (v) => {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
};

const progressPct = (row) => {
  const total = num(row.totalQuestions ?? row.total_questions);
  if (total <= 0) return 0;
  const done = num(row.practicedQuestions ?? row.practiced_questions);
  return Math.min(100, Math.round((done / total) * 100));
};

const loadList = async () => {
  loading.value = true;
  try {
    const resp = await request.get("/xwd/student/practice/knowledge-points");
    if (resp?.status === "success") {
      list.value = resp.data || [];
    } else {
      ElMessage.error(resp?.message || "加载失败");
    }
  } catch (e) {
    ElMessage.error("加载失败");
  } finally {
    loading.value = false;
  }
};

const goPractice = (row) => {
  const id = row.id ?? row.ID;
  if (id == null) return;
  router.push(`/manager/student/practice/kp/${id}`);
};

onMounted(() => {
  loadList();
});
</script>

<style scoped>
.student-page {
  padding: 16px;
}
.page-title {
  margin: 0 0 6px;
  font-size: 22px;
  color: #0f172a;
}
.page-desc {
  margin: 0 0 14px;
  color: #64748b;
  font-size: 14px;
}
.toolbar {
  margin-bottom: 12px;
}
.placeholder {
  padding: 20px;
  border: 1px dashed #c6d4ee;
  border-radius: 10px;
  color: #64748b;
}
.kp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}
.kp-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
  background: #fbfdff;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kp-name {
  font-weight: 700;
  color: #0f172a;
  font-size: 16px;
}
.kp-meta {
  font-size: 13px;
  color: #475569;
}
.kp-meta strong {
  color: #2563eb;
}
.kp-bar {
  margin-top: 4px;
}
.kp-go {
  align-self: flex-start;
  margin-top: 4px;
}
</style>
