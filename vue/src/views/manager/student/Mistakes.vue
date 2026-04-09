<template>
  <div class="student-page page-shell">
    <h2 class="page-title">错题本</h2>
    <p class="page-desc">记录每次错误，逐步形成可解释的复习路径。</p>
    <div class="tag-row">
      <el-tag effect="dark" type="danger">概念混淆</el-tag>
      <el-tag effect="dark" type="warning">推理缺失</el-tag>
      <el-tag effect="dark" type="info">审题偏差</el-tag>
    </div>
    <div class="placeholder">后续将接入错题列表、错因报告与重练入口。</div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.student-page {
  padding: 16px;
}

.tag-row {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.placeholder {
  margin-top: 14px;
  padding: 16px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px dashed #c6d4ee;
  color: #64748b;
}
</style>
