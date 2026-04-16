<template>
  <div class="student-page page-shell">
    <header class="practice-header">
      <div class="practice-header-text">
        <h2 class="page-title">练习中心</h2>
        <p class="page-desc">按知识点卡片刷题；未做过的题目优先。也可一键进入每日推荐 10 题或随机 10 题练习。</p>
      </div>
      <el-button type="primary" plain class="refresh-btn" @click="refreshAll">刷新列表</el-button>
    </header>

    <div v-if="loading" class="placeholder">加载中…</div>
    <template v-else>
      <section
        v-if="hasAnyQuestions"
        class="random-strip daily-strip"
        role="button"
        tabindex="0"
        @click="goDailyTen"
        @keydown.enter.prevent="goDailyTen"
      >
        <div class="random-strip-icon daily-icon" aria-hidden="true">DAY</div>
        <div class="random-strip-body">
          <div class="random-strip-title">每日推荐 10 题</div>
          <div class="random-strip-desc">固定每日推荐题单，做完会标记“今日已做”。当前进度：{{ dailyDoneCount }}/{{ dailyTotalCount }}。</div>
        </div>
        <el-button type="primary" class="random-strip-cta" @click.stop="goDailyTen">开始</el-button>
      </section>

      <section
        v-if="hasAnyQuestions"
        class="random-strip"
        role="button"
        tabindex="0"
        @click="goRandomTen"
        @keydown.enter.prevent="goRandomTen"
      >
        <div class="random-strip-icon" aria-hidden="true">10</div>
        <div class="random-strip-body">
          <div class="random-strip-title">随机 10 题</div>
          <div class="random-strip-desc">从全库已上架题目中随机抽 10 道（仅未做过的题），可跨多个知识点。</div>
        </div>
        <el-button type="primary" class="random-strip-cta" @click.stop="goRandomTen">开始</el-button>
      </section>

      <div v-if="list.length === 0" class="placeholder muted">暂无带题目的知识点，请教师导入题目并关联知识点。</div>
      <div v-else class="kp-grid">
        <article v-for="row in list" :key="rowKey(row)" class="kp-card">
          <div class="kp-card-top">
            <h3 class="kp-title">{{ row.name || "未命名" }}</h3>
            <div class="kp-stats">
              <span class="pill">已刷 {{ num(row.practicedQuestions ?? row.practiced_questions) }}</span>
              <span class="pill pill-total">共 {{ num(row.totalQuestions ?? row.total_questions) }} 题</span>
            </div>
          </div>
          <el-progress
            :percentage="progressPct(row)"
            :stroke-width="8"
            :show-text="false"
            class="kp-progress"
            color="#3b82f6"
          />
          <el-button type="primary" class="kp-action" @click="goPractice(row)">卡片练习</el-button>
        </article>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const router = useRouter();
const list = ref([]);
const loading = ref(false);
const dailyDoneCount = ref(0);
const dailyTotalCount = ref(10);

const rowKey = (row) => row.id ?? row.ID;
const num = (v) => {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
};

const hasAnyQuestions = computed(() => list.value.some((row) => num(row.totalQuestions ?? row.total_questions) > 0));

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

const loadDailyProgress = async () => {
  try {
    const resp = await request.get("/xwd/student/practice/daily-deck?limit=10");
    if (resp?.status === "success") {
      const rows = resp.data || [];
      dailyTotalCount.value = rows.length || 10;
      dailyDoneCount.value = rows.filter((r) => {
        const v = r?.doneToday;
        return v === true || v === 1 || String(v).toLowerCase() === "true";
      }).length;
    } else {
      dailyDoneCount.value = 0;
      dailyTotalCount.value = 10;
    }
  } catch (e) {
    dailyDoneCount.value = 0;
    dailyTotalCount.value = 10;
  }
};

const refreshAll = async () => {
  await Promise.all([loadList(), loadDailyProgress()]);
};

const goPractice = (row) => {
  const id = row.id ?? row.ID;
  if (id == null) return;
  router.push(`/manager/student/practice/kp/${id}`);
};

const goRandomTen = () => {
  if (!hasAnyQuestions.value) {
    ElMessage.warning("当前没有可练习的题目");
    return;
  }
  router.push("/manager/student/practice/random");
};

const goDailyTen = () => {
  if (!hasAnyQuestions.value) {
    ElMessage.warning("当前没有可练习的题目");
    return;
  }
  router.push("/manager/student/practice/daily");
};

onMounted(() => {
  refreshAll();
});
</script>

<style scoped>
.student-page {
  padding: 20px 18px 28px;
  max-width: 1120px;
  margin: 0 auto;
}
.practice-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.page-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #0f172a;
}
.page-desc {
  margin: 0;
  max-width: 640px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.55;
}
.refresh-btn {
  flex-shrink: 0;
  margin-top: 2px;
}
.placeholder {
  padding: 28px 20px;
  border-radius: 14px;
  text-align: center;
  color: #64748b;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}
.placeholder.muted {
  margin-top: 14px;
  background: #fafafa;
  border-color: #e2e8f0;
}
.random-strip {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 22px;
  border-radius: 16px;
  cursor: pointer;
  outline: none;
  border: 1px solid #c7d2fe;
  background: linear-gradient(110deg, #eef2ff 0%, #e0f2fe 42%, #f8fafc 100%);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.8) inset, 0 12px 32px rgba(37, 99, 235, 0.12);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}
.daily-strip {
  border-color: #99f6e4;
  background: linear-gradient(110deg, #ecfeff 0%, #e0f2fe 42%, #f8fafc 100%);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.8) inset, 0 12px 32px rgba(13, 148, 136, 0.12);
}
.daily-strip:hover {
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.85) inset, 0 16px 40px rgba(13, 148, 136, 0.16);
  border-color: #5eead4;
}
.daily-icon {
  color: #0f766e;
  background: linear-gradient(145deg, #fff 0%, #ccfbf1 100%);
  border-color: #99f6e4;
  font-size: 14px;
  letter-spacing: 0.03em;
}
.random-strip:hover {
  transform: translateY(-1px);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.85) inset, 0 16px 40px rgba(37, 99, 235, 0.16);
  border-color: #a5b4fc;
}
.random-strip:focus-visible {
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.35), 0 12px 32px rgba(37, 99, 235, 0.12);
}
.random-strip-icon {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 18px;
  color: #1e3a8a;
  background: linear-gradient(145deg, #fff 0%, #e0e7ff 100%);
  border: 1px solid #c7d2fe;
}
.random-strip-body {
  flex: 1;
  min-width: 0;
}
.random-strip-title {
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 4px;
}
.random-strip-desc {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}
.random-strip-cta {
  flex-shrink: 0;
  padding-left: 22px;
  padding-right: 22px;
  border-radius: 10px;
  font-weight: 700;
}
.kp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.kp-card {
  border-radius: 16px;
  padding: 18px 18px 16px;
  background: #fff;
  border: 1px solid #e8eef7;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.kp-card:hover {
  border-color: #bfdbfe;
  box-shadow: 0 14px 36px rgba(37, 99, 235, 0.1);
  transform: translateY(-2px);
}
.kp-card-top {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kp-title {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.kp-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pill {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
}
.pill-total {
  background: #f1f5f9;
  color: #475569;
}
.kp-progress {
  margin-top: -4px;
}
.kp-action {
  width: 100%;
  margin-top: 2px;
  border-radius: 10px;
  font-weight: 700;
}
</style>
