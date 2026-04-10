<template>
  <div class="dashboard-page page-shell">
    <div class="header-row">
      <h2>学习总览</h2>
      <el-button type="primary" plain @click="loadStats">刷新数据</el-button>
    </div>

    <div class="cards-grid">
      <div class="stat-card card-blue clickable" @click="router.push('/manager/student/records')">
        <div class="card-title">已做题数</div>
        <div class="card-value">{{ animatedSolvedCount }}</div>
        <div class="card-sub">累计完成练习题目（点击查看记录）</div>
      </div>

      <div class="stat-card card-green">
        <div class="card-title">正确率</div>
        <div class="card-value">{{ animatedAccuracy }}%</div>
        <div class="card-sub">基于历史答题准确度</div>
      </div>

      <div class="stat-card card-purple">
        <div class="card-title">学习天数</div>
        <div class="card-value">{{ animatedLearningDays }}</div>
        <div class="card-sub">有学习记录的天数</div>
      </div>
    </div>

    <el-card class="tips-card">
      <template #header>
        <span>学习建议</span>
      </template>
      <p>继续保持练习节奏，建议优先复习近期错题并完成每日 10 题训练。</p>
      <div class="action-row">
        <el-button type="primary" @click="router.push('/manager/student/learning')">去专项学习</el-button>
        <el-button type="success" @click="router.push('/manager/student/practice')">去练习中心</el-button>
        <el-button type="warning" @click="router.push('/manager/student/mistakes')">查看错题本</el-button>
      </div>
    </el-card>

    <el-card class="tips-card">
      <template #header>
        <span>学习路线推荐</span>
      </template>
      <div v-if="!latestRoute" class="empty-tip">暂无推荐路线，系统将基于学习数据自动生成。</div>
      <div v-else>
        <div class="route-title">{{ latestRoute.title }}</div>
        <p class="page-desc">{{ latestRoute.summary || "建议按以下步骤完成今日学习任务。" }}</p>
        <ul class="route-list">
          <li v-for="item in routeItems" :key="item.id">
            <span class="item-type">{{ item.itemType }}</span>
            <span>{{ item.reason || `任务 #${item.itemId}` }}</span>
          </li>
        </ul>
        <el-button type="primary" plain @click="router.push('/manager/student/recommendations')">查看完整推荐</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const router = useRouter();

const animatedSolvedCount = ref(0);
const animatedAccuracy = ref(0);
const animatedLearningDays = ref(0);
const latestRoute = ref(null);
const routeItems = ref([]);

const animateNumber = (targetRef, endValue, duration = 700, decimals = 0) => {
  const startValue = Number(targetRef.value) || 0;
  const startTime = performance.now();
  const step = (now) => {
    const progress = Math.min((now - startTime) / duration, 1);
    const current = startValue + (endValue - startValue) * progress;
    targetRef.value = decimals > 0 ? Number(current.toFixed(decimals)) : Math.round(current);
    if (progress < 1) {
      requestAnimationFrame(step);
    }
  };
  requestAnimationFrame(step);
};

const loadStats = async () => {
  try {
    const response = await request.get("/xwd/student/home/overview");
    if (response?.status === "success") {
      const data = response?.data?.stats || {};
      animateNumber(animatedSolvedCount, Number(data.solvedCount || 0));
      animateNumber(animatedAccuracy, Number(data.accuracy || 0), 700, 1);
      animateNumber(animatedLearningDays, Number(data.learningDays || 0));
      const recommendation = response?.data?.recommendation || {};
      latestRoute.value = recommendation.route || null;
      routeItems.value = recommendation.items || [];
    } else {
      ElMessage.error(response?.message || "获取学习总览失败");
    }
  } catch (error) {
    ElMessage.error("获取学习总览失败");
  }
};

onMounted(() => {
  loadStats();
});
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  border-radius: 12px;
  padding: 18px;
  color: #fff;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.clickable {
  cursor: pointer;
}

.clickable:active {
  transform: translateY(-1px) scale(0.99);
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.14);
}

.card-blue {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.card-green {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.card-purple {
  background: linear-gradient(135deg, #8e44ad, #b37bd6);
}

.card-title {
  font-size: 14px;
  opacity: 0.95;
}

.card-value {
  margin-top: 8px;
  font-size: 34px;
  font-weight: 700;
}

.card-sub {
  margin-top: 6px;
  font-size: 12px;
  opacity: 0.9;
}

.tips-card {
  background: #fbfcff;
  border-radius: 12px;
}

.action-row {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.route-title {
  font-weight: 700;
  color: #1d4ed8;
}

.route-list {
  margin: 8px 0 12px;
  padding-left: 18px;
  color: #334155;
}

.route-list li {
  margin: 4px 0;
}

.item-type {
  display: inline-block;
  margin-right: 8px;
  color: #64748b;
  font-size: 12px;
  border: 1px solid #c7d8f7;
  border-radius: 999px;
  padding: 1px 6px;
}

.empty-tip {
  color: #64748b;
}

@media (max-width: 960px) {
  .cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
