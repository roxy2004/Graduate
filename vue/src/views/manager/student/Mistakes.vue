<template>
  <div class="student-page page-shell">
    <h2 class="page-title">错题本</h2>
    <p class="page-desc">展现错题、错因标签和重做入口，重做正确会自动移除。</p>
    <div class="toolbar">
      <el-button type="primary" plain @click="loadMistakes">刷新</el-button>
      <el-button type="success" :disabled="mistakes.length === 0" @click="startBatchRedo">一键重做全部</el-button>
    </div>

    <div v-if="mistakes.length === 0" class="placeholder">当前没有错题，继续保持。</div>

    <div v-else class="mistake-list">
      <div v-for="item in mistakes" :key="item.id" class="mistake-card">
        <div class="top-row">
          <div class="tag-row">
            <el-tag type="danger" effect="dark">错题</el-tag>
            <el-tag type="warning" effect="dark">{{ item.error_type || '待分析' }}</el-tag>
            <el-tag type="info" effect="plain">{{ item.knowledge_point || '知识点待补充' }}</el-tag>
          </div>
        </div>

        <div class="content">{{ item.content || "（题干缺失）" }}</div>

        <div class="options-grid">
          <div
            v-for="opt in parseOptions(item.options)"
            :key="opt.key"
            class="option-item"
            :class="optionClass(item, opt.key)"
          >
            <span class="option-key">{{ opt.key }}</span>
            <span class="option-text">{{ opt.value }}</span>
          </div>
        </div>

        <div class="redo-row">
          <el-radio-group v-model="redoAnswers[item.id]">
            <el-radio-button label="A" />
            <el-radio-button label="B" />
            <el-radio-button label="C" />
            <el-radio-button label="D" />
          </el-radio-group>
          <el-button type="success" @click="redo(item.id)">提交重做</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="batchRedoVisible" width="820px" :close-on-click-modal="false">
      <template #header>
        <div class="dialog-header">
          <div>
            <div class="dialog-title">刷题模式</div>
            <div class="dialog-sub">可随时退出；答对自动从错题本移除</div>
          </div>
          <div class="dialog-progress">
            {{ batchIndex + 1 }} / {{ batchList.length }}
          </div>
        </div>
      </template>

      <div v-if="currentItem" class="card-box">
        <div class="tag-row">
          <el-tag type="danger" effect="dark">错题</el-tag>
          <el-tag type="warning" effect="dark">{{ currentItem.error_type || '待分析' }}</el-tag>
          <el-tag type="info" effect="plain">{{ currentItem.knowledge_point || '知识点待补充' }}</el-tag>
        </div>

        <div class="content">{{ currentItem.content || "（题干缺失）" }}</div>

        <div class="options-grid">
          <div
            v-for="opt in parseOptions(currentItem.options)"
            :key="opt.key"
            class="option-item"
            :class="batchOptionClass(opt.key)"
            @click="batchSelected = opt.key"
          >
            <span class="option-key">{{ opt.key }}</span>
            <span class="option-text">{{ opt.value }}</span>
          </div>
        </div>

        <div class="batch-actions">
          <div class="left">
            <el-button plain @click="exitBatchRedo">退出</el-button>
          </div>
          <div class="right">
            <el-button plain :disabled="batchIndex === 0" @click="prevBatch">上一题</el-button>
            <el-button type="primary" :disabled="!batchSelected || batchSubmitting" @click="submitBatch">
              {{ batchSubmitting ? "提交中..." : "提交答案" }}
            </el-button>
            <el-button
              type="success"
              plain
              :disabled="!batchFeedback"
              @click="nextBatch"
            >
              下一题
            </el-button>
          </div>
        </div>

        <div v-if="batchFeedback" class="feedback" :class="batchFeedback.ok ? 'ok' : 'bad'">
          {{ batchFeedback.msg }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const mistakes = ref([]);
const redoAnswers = reactive({});

const batchRedoVisible = ref(false);
const batchList = ref([]);
const batchIndex = ref(0);
const batchSelected = ref("");
const batchSubmitting = ref(false);
const batchFeedback = ref(null); // { ok: boolean, msg: string }

const currentItem = computed(() => batchList.value[batchIndex.value] || null);

const loadMistakes = async () => {
  try {
    const response = await request.get("/xwd/student/mistakes");
    if (response?.status === "success") {
      mistakes.value = response.data || [];
    } else {
      ElMessage.error(response?.message || "获取错题失败");
    }
  } catch (e) {
    ElMessage.error("获取错题失败");
  }
};

const parseOptions = (raw) => {
  if (!raw) return [];
  try {
    const obj = typeof raw === "string" ? JSON.parse(raw) : raw;
    return ["A", "B", "C", "D"].map((k) => ({ key: k, value: obj?.[k] || "" }));
  } catch {
    return [];
  }
};

const optionClass = (item, key) => {
  const cls = [];
  const correct = String(item.correct_answer || "").toUpperCase();
  const userAns = String(item.user_answer || "").toUpperCase();
  if (key === correct) cls.push("option-correct");
  if (key === userAns && userAns !== correct) cls.push("option-wrong");
  return cls;
};

const redo = async (recordId) => {
  const answer = redoAnswers[recordId];
  if (!answer) {
    ElMessage.warning("请先选择重做答案");
    return;
  }
  try {
    const response = await request.post(`/xwd/student/mistakes/${recordId}/redo`, { answer });
    if (response?.status === "success") {
      ElMessage.success(response?.message || "提交成功");
      await loadMistakes();
    } else {
      ElMessage.error(response?.message || "提交失败");
    }
  } catch (e) {
    ElMessage.error("提交失败");
  }
};

const startBatchRedo = () => {
  batchList.value = [...mistakes.value];
  batchIndex.value = 0;
  batchSelected.value = "";
  batchFeedback.value = null;
  batchRedoVisible.value = true;
};

const exitBatchRedo = () => {
  batchRedoVisible.value = false;
};

const prevBatch = () => {
  if (batchIndex.value <= 0) return;
  batchIndex.value -= 1;
  batchSelected.value = "";
  batchFeedback.value = null;
};

const nextBatch = () => {
  if (batchIndex.value >= batchList.value.length - 1) {
    batchRedoVisible.value = false;
    return;
  }
  batchIndex.value += 1;
  batchSelected.value = "";
  batchFeedback.value = null;
};

const submitBatch = async () => {
  const item = currentItem.value;
  if (!item) return;
  if (!batchSelected.value) {
    ElMessage.warning("请选择答案");
    return;
  }
  batchSubmitting.value = true;
  batchFeedback.value = null;
  try {
    const response = await request.post(`/xwd/student/mistakes/${item.id}/redo`, { answer: batchSelected.value });
    if (response?.status === "success") {
      const ok = !!response?.isCorrect;
      batchFeedback.value = { ok, msg: response?.message || (ok ? "正确" : "错误") };
      await loadMistakes();
      // 同步最新错题列表到刷题队列（若答对，该题会消失）
      const stillWrong = mistakes.value.map((x) => x.id);
      batchList.value = batchList.value.filter((x) => stillWrong.includes(x.id));
      if (batchIndex.value >= batchList.value.length) {
        batchIndex.value = Math.max(batchList.value.length - 1, 0);
      }
      if (batchList.value.length === 0) {
        batchRedoVisible.value = false;
        ElMessage.success("已清空错题本");
      }
    } else {
      batchFeedback.value = { ok: false, msg: response?.message || "提交失败" };
    }
  } catch (e) {
    batchFeedback.value = { ok: false, msg: "提交失败" };
  } finally {
    batchSubmitting.value = false;
  }
};

const batchOptionClass = (key) => {
  const item = currentItem.value;
  if (!item) return [];
  const cls = [];
  const correct = String(item.correct_answer || "").toUpperCase();
  if (batchSelected.value === key) cls.push("option-selected");
  if (batchFeedback.value) {
    if (key === correct) cls.push("option-correct");
    if (batchSelected.value === key && key !== correct) cls.push("option-wrong");
  }
  return cls;
};

onMounted(() => {
  loadMistakes();
});
</script>

<style scoped>
.student-page {
  padding: 16px;
}

.toolbar {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}

.mistake-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mistake-card {
  border: 1px solid #d8e1f0;
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}

.tag-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.content {
  margin-top: 12px;
  color: #1f2937;
  line-height: 1.6;
}

.options-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.option-item {
  border: 1px solid #dbe4f3;
  border-radius: 10px;
  padding: 10px;
  display: flex;
  gap: 8px;
  background: #f8fbff;
}

.option-key {
  font-weight: 700;
  color: #334155;
}

.option-text {
  color: #1f2937;
}

.option-correct {
  background: rgba(34, 197, 94, 0.16);
  border-color: rgba(34, 197, 94, 0.45);
}

.option-wrong {
  background: rgba(239, 68, 68, 0.16);
  border-color: rgba(239, 68, 68, 0.45);
}

.redo-row {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.dialog-title {
  font-weight: 800;
  color: #1d4ed8;
}

.dialog-sub {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.dialog-progress {
  color: #475569;
  font-weight: 700;
}

.card-box {
  padding: 8px 4px;
}

.option-selected {
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.25) inset;
}

.batch-actions {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.batch-actions .right {
  display: flex;
  gap: 10px;
}

.feedback {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #dbe4f3;
  background: #f8fbff;
  color: #334155;
}

.feedback.ok {
  border-color: rgba(34, 197, 94, 0.45);
  background: rgba(34, 197, 94, 0.12);
}

.feedback.bad {
  border-color: rgba(239, 68, 68, 0.45);
  background: rgba(239, 68, 68, 0.12);
}

.placeholder {
  margin-top: 14px;
  padding: 16px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px dashed #c6d4ee;
  color: #64748b;
}

@media (max-width: 960px) {
  .options-grid {
    grid-template-columns: 1fr;
  }

  .redo-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
