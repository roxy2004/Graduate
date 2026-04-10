<template>
  <div class="student-page page-shell">
    <h2 class="page-title">预测推荐</h2>
    <p class="page-desc">当前展示规则版推荐路线，后续将切换大模型增强版。</p>
    <el-button type="primary" plain @click="loadRecommendation">刷新推荐</el-button>

    <div v-if="!route" class="placeholder">暂无推荐路线，请先进行专项学习与练习。</div>
    <div v-else class="placeholder">
      <div class="title">{{ route.title }}</div>
      <p class="page-desc">{{ route.summary || "请按顺序完成推荐学习与练习任务。" }}</p>
      <ul class="item-list">
        <li v-for="item in items" :key="item.id">
          <span class="type">{{ item.itemType }}</span>
          <span>{{ item.reason || `任务 #${item.itemId}` }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const route = ref(null);
const items = ref([]);

const loadRecommendation = async () => {
  try {
    const response = await request.get("/xwd/student/recommendations/latest");
    if (response?.status === "success") {
      route.value = response?.data?.route || null;
      items.value = response?.data?.items || [];
    } else {
      ElMessage.error(response?.message || "获取推荐失败");
    }
  } catch (e) {
    ElMessage.error("获取推荐失败");
  }
};

loadRecommendation();
</script>

<style scoped>
.student-page {
  padding: 16px;
}

.placeholder {
  margin-top: 14px;
  padding: 16px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px dashed #c6d4ee;
  color: #64748b;
}

.title {
  color: #1d4ed8;
  font-weight: 700;
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
</style>

