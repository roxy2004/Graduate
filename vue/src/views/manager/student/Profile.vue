<template>
  <div class="student-page page-shell">
    <h2 class="page-title">学习画像</h2>
    <p class="page-desc">以数据刻画掌握度，定位薄弱知识点。</p>
    <div class="progress-list">
      <div class="progress-item">
        <span>树与图</span>
        <el-progress :percentage="72" :stroke-width="8" />
      </div>
      <div class="progress-item">
        <span>排序与查找</span>
        <el-progress :percentage="64" :stroke-width="8" />
      </div>
      <div class="progress-item">
        <span>栈与队列</span>
        <el-progress :percentage="83" :stroke-width="8" />
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.student-page {
  padding: 16px;
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
</style>
