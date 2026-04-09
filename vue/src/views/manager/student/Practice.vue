<template>
  <div class="student-page page-shell">
    <h2 class="page-title">练习中心</h2>
    <p class="page-desc">按知识点和难度发起练习，持续强化薄弱项。</p>
    <div class="feature-grid">
      <div class="feature-card">
        <div class="label">练习模式</div>
        <div class="value">自适应训练</div>
      </div>
      <div class="feature-card">
        <div class="label">今日目标</div>
        <div class="value">10 题</div>
      </div>
      <div class="feature-card">
        <div class="label">建议难度</div>
        <div class="value">0.5 - 0.7</div>
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

.feature-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.feature-card {
  border: 1px solid #d8e1f0;
  border-radius: 10px;
  padding: 14px;
  background: #f8fbff;
}

.label {
  color: #64748b;
}

.value {
  margin-top: 8px;
  font-size: 18px;
  color: #1d4ed8;
}

@media (max-width: 960px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
