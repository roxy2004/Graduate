<template>
  <div class="student-page page-shell">
    <h2 class="page-title">个人中心</h2>
    <p class="page-desc">管理账号信息和学习偏好。</p>
    <div class="account-grid">
      <div class="kv"><span>账号状态</span><strong>在线</strong></div>
      <div class="kv"><span>学习等级</span><strong>L2</strong></div>
      <div class="kv"><span>最近登录</span><strong>今天</strong></div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.student-page {
  padding: 16px;
}

.account-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.kv {
  border: 1px solid #d8e1f0;
  border-radius: 10px;
  padding: 12px;
  background: #f8fbff;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kv span {
  color: #64748b;
}

.kv strong {
  color: #1d4ed8;
}

@media (max-width: 960px) {
  .account-grid {
    grid-template-columns: 1fr;
  }
}
</style>
