<template>
  <div class="student-page page-shell">
    <h2 class="page-title">学习画像</h2>
    <p class="page-desc">动态掌握度柱状图（可点击），快速定位薄弱知识点并跳转练习。</p>

    <div class="toolbar">
      <el-button type="primary" plain @click="loadProfile">刷新</el-button>
    </div>

    <div v-if="loading" class="placeholder">画像计算中…</div>
    <div v-else-if="list.length === 0" class="placeholder">暂无画像数据，请先到练习中心完成一些作答。</div>
    <template v-else>
      <div class="summary-row">
        <div class="summary-card">
          <div class="k">已建画像知识点</div>
          <div class="v">{{ profile?.knowledgePointCount ?? list.length }}</div>
        </div>
        <div class="summary-card weak">
          <div class="k">薄弱 TOP</div>
          <div class="v">{{ weakNames || "暂无" }}</div>
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-head">
          <span>知识点掌握度柱状图</span>
          <span class="chart-tip">点击任一柱子可跳到对应卡片练习</span>
        </div>
        <div class="chart-controls">
          <el-radio-group v-model="sortMode" size="small">
            <el-radio-button label="weak">薄弱优先</el-radio-button>
            <el-radio-button label="strong">高分优先</el-radio-button>
            <el-radio-button label="practice">按练习次数</el-radio-button>
          </el-radio-group>
          <el-switch
            v-model="weakOnly"
            inline-prompt
            active-text="仅看薄弱"
            inactive-text="全部"
            size="small"
          />
        </div>
        <TransitionGroup name="bar-move" tag="div" class="bar-chart">
          <div
            v-for="item in chartList"
            :key="'bar-' + item.kpId"
            class="bar-row"
            :class="{ pulse: pulseOnSort, active: activeKpId === item.kpId }"
            :style="{ '--bar-delay': `${barDelayMs(item.kpId)}ms` }"
            @click="goPractice(item, true)"
            :title="barTitle(item)"
          >
            <div class="bar-label">{{ item.kpName || `知识点#${item.kpId}` }}</div>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: `${toPercent(item.mastery)}%` }" :class="barClass(item.mastery)" />
            </div>
            <div class="bar-val">{{ toPercent(item.mastery) }}%</div>
          </div>
        </TransitionGroup>
      </div>

      <TransitionGroup name="card-move" tag="div" class="progress-list">
        <div v-for="item in listView" :key="item.kpId" class="progress-item">
          <div class="head">
            <span class="name">{{ item.kpName || `知识点#${item.kpId}` }}</span>
            <div class="head-right">
              <el-tag size="small" :type="tagType(item.mastery)">{{ tagText(item.mastery) }}</el-tag>
              <el-button text type="primary" size="small" @click="goPractice(item, false)">去练习</el-button>
            </div>
          </div>
          <el-progress :percentage="toPercent(item.mastery)" :stroke-width="8" />
          <div class="meta">
            <span>练习 {{ item.practicedCount || 0 }} 题</span>
            <span>正确率 {{ toAccuracy(item.accuracy) }}%</span>
            <span>置信度 {{ toPercent(item.confidence) }}%</span>
          </div>
        </div>
      </TransitionGroup>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const router = useRouter();
const loading = ref(false);
const profile = ref(null);
const list = ref([]);
const sortMode = ref("weak");
const weakOnly = ref(false);
const pulseOnSort = ref(false);
const activeKpId = ref(null);

const weakNames = computed(() => {
  const rows = profile.value?.weakTop || [];
  return rows.map((x) => x.kpName).filter(Boolean).join(" / ");
});

const chartList = computed(() => {
  return listView.value.slice(0, 12);
});

const listView = computed(() => {
  let rows = [...list.value];
  if (weakOnly.value) {
    rows = rows.filter((x) => Number(x.mastery ?? 0) < 0.45);
  }
  if (sortMode.value === "strong") {
    rows.sort((a, b) => Number(b.mastery ?? 0) - Number(a.mastery ?? 0));
  } else if (sortMode.value === "practice") {
    rows.sort((a, b) => Number(b.practicedCount ?? 0) - Number(a.practicedCount ?? 0));
  } else {
    rows.sort((a, b) => Number(a.mastery ?? 0) - Number(b.mastery ?? 0));
  }
  return rows;
});

const toPercent = (v) => {
  const n = Number(v);
  if (!Number.isFinite(n)) return 0;
  return Math.max(0, Math.min(100, Math.round(n * 100)));
};

const toAccuracy = (v) => {
  const n = Number(v);
  if (!Number.isFinite(n)) return 0;
  return Math.max(0, Math.min(100, Math.round(n * 10) / 10));
};

const tagText = (mastery) => {
  const m = Number(mastery);
  if (!Number.isFinite(m)) return "未知";
  if (m < 0.45) return "薄弱";
  if (m < 0.7) return "待巩固";
  return "较好";
};

const tagType = (mastery) => {
  const m = Number(mastery);
  if (!Number.isFinite(m)) return "info";
  if (m < 0.45) return "danger";
  if (m < 0.7) return "warning";
  return "success";
};

const barClass = (mastery) => {
  const m = Number(mastery);
  if (!Number.isFinite(m)) return "bar-mid";
  if (m < 0.45) return "bar-low";
  if (m < 0.7) return "bar-mid";
  return "bar-high";
};

const barDelayMs = (kpId) => {
  const idx = chartList.value.findIndex((x) => x.kpId === kpId);
  if (idx < 0) return 0;
  return Math.min(40 * idx, 360);
};

const barTitle = (item) => {
  const name = item?.kpName || `知识点#${item?.kpId ?? "-"}`;
  const practiced = item?.practicedCount || 0;
  const accuracy = toAccuracy(item?.accuracy);
  const last = formatDateTime(item?.lastPracticedAt);
  return `${name}\n掌握度 ${toPercent(item?.mastery)}%\n练习 ${practiced} 题\n正确率 ${accuracy}%\n最近练习 ${last}\n点击进入卡片练习`;
};

const formatDateTime = (raw) => {
  if (!raw) return "暂无";
  const d = new Date(raw);
  if (Number.isNaN(d.getTime())) return String(raw);
  return d.toLocaleString();
};

const goPractice = (item, withDelay = false) => {
  const kpId = item?.kpId ?? item?.id;
  if (kpId == null) return;
  if (!withDelay) {
    router.push(`/manager/student/practice/kp/${kpId}`);
    return;
  }
  activeKpId.value = kpId;
  setTimeout(() => {
    router.push(`/manager/student/practice/kp/${kpId}`);
    setTimeout(() => {
      if (activeKpId.value === kpId) {
        activeKpId.value = null;
      }
    }, 260);
  }, 120);
};

const loadProfile = async () => {
  loading.value = true;
  try {
    const resp = await request.get("/xwd/student/profile");
    if (resp?.status === "success") {
      profile.value = resp.data || {};
      list.value = profile.value.knowledgePoints || [];
    } else {
      ElMessage.error(resp?.message || "加载画像失败");
    }
  } catch (e) {
    ElMessage.error("加载画像失败");
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadProfile();
});

watch([sortMode, weakOnly], () => {
  pulseOnSort.value = true;
  setTimeout(() => {
    pulseOnSort.value = false;
  }, 360);
});
</script>

<style scoped>
.student-page {
  padding: 16px;
}

.toolbar {
  margin-top: 12px;
}

.summary-row {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 12px;
}

.summary-card {
  padding: 12px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #d8e1f0;
}

.summary-card.weak {
  border-color: #fbcfe8;
  background: #fdf2f8;
}

.summary-card .k {
  color: #64748b;
  font-size: 12px;
}

.summary-card .v {
  margin-top: 8px;
  color: #0f172a;
  font-weight: 700;
}

.chart-card {
  margin-top: 14px;
  padding: 12px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #d8e1f0;
}

.chart-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.chart-head span:first-child {
  font-weight: 700;
  color: #1f2937;
}

.chart-tip {
  color: #64748b;
  font-size: 12px;
}

.chart-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-row {
  display: grid;
  grid-template-columns: 150px 1fr 56px;
  gap: 8px;
  align-items: center;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  transition: background-color 0.15s ease;
  animation: bar-stagger-in 0.38s ease both;
  animation-delay: var(--bar-delay, 0ms);
}

.bar-row:hover {
  background: #f8fbff;
}

.bar-row.active {
  background: rgba(219, 234, 254, 0.8);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.35);
  transform: translateX(2px);
}

.bar-row.pulse {
  animation: bar-pulse 0.36s ease;
}

.bar-label {
  color: #334155;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-track {
  position: relative;
  height: 10px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease, background-color 0.25s ease;
}

.bar-low {
  background: linear-gradient(90deg, #ef4444, #f97316);
}

.bar-mid {
  background: linear-gradient(90deg, #f59e0b, #eab308);
}

.bar-high {
  background: linear-gradient(90deg, #10b981, #22c55e);
}

.bar-val {
  font-size: 12px;
  text-align: right;
  color: #475569;
  font-weight: 600;
}

.bar-move-move,
.card-move-move {
  transition: transform 0.3s ease;
}

.bar-move-enter-active,
.bar-move-leave-active,
.card-move-enter-active,
.card-move-leave-active {
  transition: all 0.25s ease;
}

.bar-move-enter-from,
.bar-move-leave-to,
.card-move-enter-from,
.card-move-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.bar-move-leave-active,
.card-move-leave-active {
  position: absolute;
}

@keyframes bar-stagger-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bar-pulse {
  0% {
    box-shadow: inset 0 0 0 0 rgba(37, 99, 235, 0.12);
    background: rgba(219, 234, 254, 0);
  }
  50% {
    box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.35);
    background: rgba(219, 234, 254, 0.45);
  }
  100% {
    box-shadow: inset 0 0 0 0 rgba(37, 99, 235, 0.12);
    background: rgba(219, 234, 254, 0);
  }
}

.progress-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.progress-item {
  padding: 12px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #d8e1f0;
}

.progress-item span {
  display: block;
  margin-bottom: 8px;
  color: #334155;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name {
  margin-bottom: 0 !important;
  font-weight: 600;
}

.meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #64748b;
  font-size: 12px;
}

.placeholder {
  margin-top: 14px;
  padding: 16px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px dashed #c6d4ee;
  color: #64748b;
}

@media (max-width: 768px) {
  .summary-row {
    grid-template-columns: 1fr;
  }

  .bar-row {
    grid-template-columns: 110px 1fr 44px;
  }

  .chart-controls {
    align-items: flex-start;
  }
}
</style>
