<template>
  <div class="records-page page-shell">
    <div class="header-row">
      <div>
        <h2 class="page-title">已做题记录</h2>
        <p class="page-desc">绿色为正确，红色为错误。支持单条删除与一键清空。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" plain @click="loadRecords">刷新</el-button>
        <el-button type="danger" plain @click="clearAll">清空全部</el-button>
      </div>
    </div>

    <div v-if="records.length === 0" class="empty-box">
      暂无做题记录
    </div>

    <div class="records-list" v-else>
      <div
        v-for="item in records"
        :key="item.id"
        class="record-card"
        :class="Number(item.is_correct) === 1 ? 'ok' : 'bad'"
      >
        <div class="record-top">
          <div class="meta">
            <span class="qid">#{{ item.question_id }}</span>
            <span class="time">{{ formatTime(item.created_at) }}</span>
          </div>
          <el-button type="danger" text @click="deleteOne(item.id)">删除</el-button>
        </div>

        <div class="content">
          {{ item.content || '（题干缺失）' }}
        </div>

        <div class="record-bottom">
          <div class="chips">
            <span class="chip">我的答案：{{ item.user_answer }}</span>
            <span class="chip">用时：{{ item.time_spent ?? '-' }}s</span>
          </div>
          <div class="status">
            {{ Number(item.is_correct) === 1 ? '正确' : '错误' }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/utils/request";

const records = ref([]);

const loadRecords = async () => {
  try {
    const response = await request.get("/xwd/student/records");
    if (response?.status === "success") {
      records.value = response.data || [];
    } else {
      ElMessage.error(response?.message || "获取记录失败");
    }
  } catch (e) {
    ElMessage.error("获取记录失败");
  }
};

const deleteOne = async (id) => {
  try {
    await ElMessageBox.confirm("确定删除该条做题记录吗？", "提示", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }

  try {
    const response = await request.delete(`/xwd/student/records/${id}`);
    if (response?.status === "success") {
      ElMessage.success("删除成功");
      await loadRecords();
    } else {
      ElMessage.error(response?.message || "删除失败");
    }
  } catch (e) {
    ElMessage.error("删除失败");
  }
};

const clearAll = async () => {
  if (records.value.length === 0) return;
  try {
    await ElMessageBox.confirm("确定清空全部做题记录吗？该操作不可恢复。", "警告", {
      type: "error",
      confirmButtonText: "清空",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }

  try {
    const response = await request.delete("/xwd/student/records");
    if (response?.status === "success") {
      ElMessage.success(`已清空 ${response?.count ?? 0} 条`);
      await loadRecords();
    } else {
      ElMessage.error(response?.message || "清空失败");
    }
  } catch (e) {
    ElMessage.error("清空失败");
  }
};

const formatTime = (value) => {
  if (!value) return "-";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}`;
};

onMounted(() => {
  loadRecords();
});
</script>

<style scoped>
.records-page {
  padding: 18px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.empty-box {
  margin-top: 14px;
  padding: 18px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px dashed #c6d4ee;
  color: #64748b;
}

.records-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-card {
  border-radius: 12px;
  padding: 14px 14px 12px;
  border: 1px solid #d8e1f0;
  background: #fff;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.record-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(11, 18, 32, 0.08);
}

.record-card.ok {
  background: linear-gradient(180deg, rgba(34, 197, 94, 0.12), rgba(255, 255, 255, 0.9));
  border-color: rgba(34, 197, 94, 0.35);
}

.record-card.bad {
  background: linear-gradient(180deg, rgba(239, 68, 68, 0.12), rgba(255, 255, 255, 0.9));
  border-color: rgba(239, 68, 68, 0.35);
}

.record-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
}

.qid {
  color: #1d4ed8;
  font-weight: 700;
}

.content {
  margin-top: 10px;
  color: #1f2937;
  line-height: 1.6;
}

.record-bottom {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  font-size: 12px;
  color: #475569;
  background: rgba(148, 163, 184, 0.18);
  border: 1px solid rgba(148, 163, 184, 0.25);
  padding: 4px 8px;
  border-radius: 999px;
}

.status {
  font-weight: 700;
  color: #0f172a;
}
</style>

