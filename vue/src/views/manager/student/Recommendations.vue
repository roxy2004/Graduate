<template>
  <div class="student-page page-shell">
    <div class="head-row">
      <div>
        <h2 class="page-title">个性化推荐与预测</h2>
        <p class="page-desc">基于掌握度、正确率、练习次数与遗忘间隔进行排序推荐，并给出短期表现预测。</p>
      </div>
      <div class="head-actions">
        <el-segmented
          v-model="viewMode"
          :options="modeOptions"
          size="small"
        />
        <el-button type="primary" plain :loading="loadingRule || loadingAi" @click="loadRecommendation">刷新推荐</el-button>
      </div>
    </div>
    <div class="stage-tip">
      <span>规则结果先展示；AI 理由异步加载</span>
      <span v-if="loadingAi">AI 增强加载中...</span>
      <span v-else-if="hasAiData">AI 增强已就绪</span>
    </div>

    <section v-if="activePrediction" class="prediction-grid">
      <div class="metric-card">
        <div class="metric-label">当前正确率</div>
        <div class="metric-value">{{ activePrediction.currentAccuracyPercent }}%</div>
      </div>
      <div class="metric-card metric-accent">
        <div class="metric-label">预测正确率（未来 {{ activePrediction.horizon || 10 }} 题）</div>
        <div class="metric-value">{{ activePrediction.predictedAccuracyPercent }}%</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">预测答对题数</div>
        <div class="metric-value">{{ activePrediction.predictedCorrectNext10 }} / 10</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">预计掌握度增益</div>
        <div class="metric-value">+{{ activePrediction.predictedMasteryGainPercent }}%</div>
        <div class="metric-sub">预测置信度：{{ activePrediction.confidenceLabel || "低" }}</div>
      </div>
    </section>
    <section v-if="activeAiInsights" class="ai-box">
      <div class="ai-title">AI 增强建议</div>
      <div class="ai-line">{{ activeAiInsights.summary }}</div>
      <div class="ai-sub">{{ activeAiInsights.predictionComment }}</div>
    </section>

    <div v-if="activePersonalized.length === 0" class="placeholder">暂无可生成的推荐，请先完成几次练习。</div>
    <section v-else class="rec-list">
      <article v-for="(it, idx) in activePersonalized" :key="it.kpId || idx" class="rec-card">
        <div class="rec-top">
          <div class="rank">TOP {{ idx + 1 }}</div>
          <div class="score">优先级 {{ toPercent(it.priorityScore) }}%</div>
        </div>
        <h3 class="rec-title">{{ it.kpName || `知识点 #${it.kpId}` }}</h3>
        <p class="rec-reason">{{ it.reason || "建议继续巩固练习" }}</p>
        <div class="chips">
          <span class="chip">掌握度 {{ num(it.masteryPercent) }}%</span>
          <span class="chip">正确率 {{ num(it.accuracyPercent) }}%</span>
          <span class="chip">置信度 {{ num(it.confidencePercent) }}%</span>
          <span class="chip">已刷 {{ num(it.practicedCount) }}</span>
          <span class="chip">剩余 {{ num(it.remainingQuestions) }}</span>
        </div>
        <el-button type="primary" plain @click="goAction(it)">去练习</el-button>
      </article>
    </section>

    <section class="daily-box">
      <div class="daily-head">
        <h3>每日推荐 10 题</h3>
        <span class="daily-sub">按个性化优先级抽题，每题附推荐理由</span>
      </div>
      <div v-if="activeDailyQuestions.length === 0" class="placeholder">今日暂无可推荐题目，请先进行练习。</div>
      <div v-else class="daily-list">
        <article v-for="(q, idx) in activeDailyQuestions" :key="q.questionId || idx" class="daily-item">
          <div class="daily-top">
            <span class="q-no">#{{ idx + 1 }}</span>
            <span class="q-kp">{{ q.knowledgePointName || "综合知识点" }}</span>
            <span v-if="q.doneToday" class="q-flag done">今日已做</span>
            <span v-else-if="q.isRedo" class="q-flag redo">已做过</span>
            <span v-else class="q-flag fresh">新题</span>
            <span v-if="Number(q.wrongCount || 0) > 0" class="q-flag wrong">错题重做</span>
          </div>
          <div class="q-content">{{ shortText(q.content) }}</div>
          <div class="q-reason">{{ q.reason }}</div>
          <el-button type="primary" text @click="goAction(q)">去做这题</el-button>
        </article>
      </div>
    </section>

    <section v-if="route || items.length" class="legacy-box">
      <div class="legacy-title">历史推荐路线</div>
      <p v-if="route" class="legacy-summary">{{ route.title }} · {{ route.summary || "按顺序完成推荐任务。" }}</p>
      <ul class="item-list">
        <li v-for="item in items" :key="item.id">
          <span class="type">{{ item.itemType }}</span>
          <span>{{ item.reason || `任务 #${item.itemId}` }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request";
import router from "@/router";

const route = ref(null);
const items = ref([]);
const rulePayload = ref(null);
const aiPayload = ref(null);
const loadingRule = ref(false);
const loadingAi = ref(false);
const viewMode = ref("rule");
const modeOptions = [
  { label: "规则结果", value: "rule" },
  { label: "AI 理由", value: "ai" },
];

const hasAiData = computed(() => !!aiPayload.value);
const activePayload = computed(() => {
  if (viewMode.value === "ai" && aiPayload.value) return aiPayload.value;
  return rulePayload.value || aiPayload.value || {};
});
const activePersonalized = computed(() => activePayload.value?.personalized || []);
const activePrediction = computed(() => activePayload.value?.prediction || null);
const activeDailyQuestions = computed(() => activePayload.value?.dailyQuestions || []);
const activeAiInsights = computed(() => {
  if (viewMode.value === "ai") return aiPayload.value?.aiInsights || null;
  return rulePayload.value?.aiInsights || null;
});

const num = (v) => {
  const n = Number(v);
  return Number.isFinite(n) ? (Math.round(n * 10) / 10).toFixed(1).replace(".0", "") : "0";
};

const toPercent = (v) => {
  const n = Number(v);
  if (!Number.isFinite(n)) return 0;
  return Math.round(Math.max(0, Math.min(1, n)) * 100);
};

const goAction = (item) => {
  if (item?.actionPath) {
    router.push(item.actionPath);
  }
};

const shortText = (s) => {
  const raw = (s || "").toString().replace(/\s+/g, " ").trim();
  if (!raw) return "（题干为空）";
  return raw.length > 70 ? `${raw.slice(0, 70)}...` : raw;
};

const loadRecommendation = async () => {
  loadingRule.value = true;
  loadingAi.value = true;
  aiPayload.value = null;
  try {
    const fastResp = await request.get("/xwd/student/recommendations/latest?includeAi=false");
    if (fastResp?.status === "success") {
      const data = fastResp?.data || {};
      route.value = data?.route || null;
      items.value = data?.items || [];
      rulePayload.value = data;
    } else {
      ElMessage.error(fastResp?.message || "获取推荐失败");
      loadingRule.value = false;
      loadingAi.value = false;
      return;
    }
  } catch (e) {
    ElMessage.error("获取推荐失败");
    loadingRule.value = false;
    loadingAi.value = false;
    return;
  } finally {
    loadingRule.value = false;
  }

  try {
    const aiResp = await request.get("/xwd/student/recommendations/latest?includeAi=true");
    if (aiResp?.status === "success") {
      const data = aiResp?.data || {};
      aiPayload.value = data;
      if (viewMode.value === "ai" && !hasAiData.value) {
        viewMode.value = "rule";
      }
    }
  } catch (e) {
    // ignore AI stage failure, keep rule result
  } finally {
    loadingAi.value = false;
  }
};

loadRecommendation();
</script>

<style scoped>
.student-page {
  padding: 16px;
  max-width: 1080px;
  margin: 0 auto;
}

.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stage-tip {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.prediction-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 12px;
}

.metric-card {
  border: 1px solid #dbeafe;
  background: #f8fbff;
  border-radius: 12px;
  padding: 14px;
}

.metric-accent {
  border-color: #93c5fd;
  background: linear-gradient(165deg, #eff6ff 0%, #f8fbff 100%);
}

.metric-label {
  color: #475569;
  font-size: 12px;
}

.metric-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 800;
  color: #1d4ed8;
}

.metric-sub {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.rec-list {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.rec-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rec-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rank {
  font-size: 12px;
  font-weight: 700;
  color: #1e40af;
}

.score {
  font-size: 12px;
  color: #475569;
}

.rec-title {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}

.rec-reason {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.45;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  background: #eff6ff;
  color: #2563eb;
}

.placeholder {
  margin-top: 14px;
  padding: 16px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px dashed #c6d4ee;
  color: #64748b;
}

.item-list {
  margin: 10px 0 0;
  padding-left: 18px;
}

.item-list li {
  margin: 4px 0;
  color: #334155;
}

.type {
  display: inline-block;
  margin-right: 8px;
  color: #64748b;
  font-size: 12px;
}

.legacy-box {
  margin-top: 18px;
  padding: 14px;
  border-radius: 10px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
}

.legacy-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.legacy-summary {
  margin: 6px 0 0;
  font-size: 13px;
  color: #475569;
}

.daily-box {
  margin-top: 18px;
}

.daily-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.daily-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.daily-sub {
  color: #64748b;
  font-size: 12px;
}

.daily-list {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 10px;
}

.daily-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  background: #fff;
}

.daily-top {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
}

.q-flag {
  font-size: 11px;
  border-radius: 999px;
  padding: 2px 7px;
}

.q-flag.redo {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fdba74;
}

.q-flag.done {
  background: #ecfeff;
  color: #0f766e;
  border: 1px solid #99f6e4;
}

.q-flag.fresh {
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #6ee7b7;
}

.q-flag.wrong {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fca5a5;
}

.q-no {
  font-weight: 700;
  color: #1d4ed8;
  font-size: 12px;
}

.q-kp {
  font-size: 12px;
  color: #475569;
  background: #eff6ff;
  border-radius: 999px;
  padding: 2px 8px;
}

.q-content {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.45;
  margin-bottom: 6px;
}

.q-reason {
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
  min-height: 34px;
}

.ai-box {
  margin-top: 12px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: linear-gradient(165deg, #f8fbff 0%, #eef6ff 100%);
  padding: 12px 14px;
}

.ai-title {
  font-size: 12px;
  font-weight: 700;
  color: #1d4ed8;
}

.ai-line {
  margin-top: 6px;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.45;
}

.ai-sub {
  margin-top: 4px;
  color: #475569;
  font-size: 12px;
}
</style>

