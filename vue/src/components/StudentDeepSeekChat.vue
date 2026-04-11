<template>
  <div class="ds-root">
    <div v-show="!minimized" class="ds-panel">
      <div class="ds-header">
        <span class="ds-title">DeepSeek 学习助手</span>
        <div class="ds-actions">
          <el-button text type="primary" size="small" @click="clearChat">清空</el-button>
          <el-button text size="small" @click="minimized = true">收起</el-button>
        </div>
      </div>
      <div ref="msgBoxRef" class="ds-messages">
        <div v-if="messages.length === 0" class="ds-hint">可向助手提问课程相关概念、错题思路等。</div>
        <div v-for="(m, idx) in messages" :key="idx" class="ds-row" :class="m.role">
          <span class="ds-label">{{ m.role === "user" ? "我" : "助手" }}</span>
          <div class="ds-bubble">{{ m.content }}</div>
        </div>
        <div v-if="loading" class="ds-row assistant">
          <span class="ds-label">助手</span>
          <div class="ds-bubble ds-pulse">正在思考…</div>
        </div>
      </div>
      <div class="ds-inputbar">
        <el-input
          v-model="draft"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 5 }"
          placeholder="输入后发送（Shift+Enter 换行）"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" class="ds-send" :loading="loading" @click="send">发送</el-button>
      </div>
      <div class="ds-resize-tip">右下角拖动可缩放</div>
    </div>
    <button
      v-show="minimized"
      type="button"
      class="ds-fab"
      title="打开 DeepSeek 学习助手"
      aria-label="打开 DeepSeek 学习助手"
      @click="minimized = false"
    >
      AI
    </button>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const minimized = ref(false);
const messages = ref([]);
const draft = ref("");
const loading = ref(false);
const msgBoxRef = ref(null);

const scrollBottom = () => {
  nextTick(() => {
    const el = msgBoxRef.value;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  });
};

watch(messages, () => scrollBottom(), { deep: true });
watch(loading, () => scrollBottom());

const send = async () => {
  const text = (draft.value || "").trim();
  if (!text || loading.value) {
    return;
  }
  messages.value = [...messages.value, { role: "user", content: text }];
  draft.value = "";
  loading.value = true;
  try {
    const resp = await request.post(
      "/xwd/student/deepseek/chat",
      { messages: messages.value },
      { timeout: 120000 }
    );
    if (resp?.status === "success") {
      const reply = (resp.message ?? "").toString();
      messages.value = [...messages.value, { role: "assistant", content: reply || "（无回复内容）" }];
    } else {
      ElMessage.error(resp?.message || "发送失败");
      messages.value = messages.value.slice(0, -1);
    }
  } catch {
    messages.value = messages.value.slice(0, -1);
  } finally {
    loading.value = false;
  }
};

const clearChat = () => {
  messages.value = [];
};
</script>

<style scoped>
.ds-root {
  font-size: 14px;
}

.ds-panel {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 4000;
  width: 400px;
  height: 480px;
  min-width: 280px;
  min-height: 300px;
  max-width: calc(100vw - 24px);
  max-height: calc(100vh - 24px);
  resize: both;
  overflow: auto;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.18);
  border: 1px solid #e2e8f0;
}

.ds-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(90deg, #f8fafc, #eff6ff);
  border-radius: 16px 16px 0 0;
}

.ds-title {
  font-weight: 700;
  color: #1e3a8a;
}

.ds-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.ds-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
}

.ds-hint {
  font-size: 13px;
  color: #64748b;
  padding: 8px 4px;
}

.ds-row {
  margin-bottom: 12px;
}

.ds-row.user .ds-bubble {
  background: #dbeafe;
  color: #0f172a;
}

.ds-row.assistant .ds-bubble {
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #0f172a;
}

.ds-label {
  display: block;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.ds-bubble {
  padding: 10px 12px;
  border-radius: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.ds-pulse {
  animation: ds-pulse 1.2s ease-in-out infinite;
}

@keyframes ds-pulse {
  0%,
  100% {
    opacity: 0.65;
  }
  50% {
    opacity: 1;
  }
}

.ds-inputbar {
  flex-shrink: 0;
  padding: 10px 12px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fff;
}

.ds-send {
  align-self: flex-end;
}

.ds-resize-tip {
  flex-shrink: 0;
  text-align: center;
  font-size: 11px;
  color: #94a3b8;
  padding: 0 8px 8px;
  user-select: none;
}

.ds-fab {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 4000;
  width: 52px;
  height: 52px;
  padding: 0;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-weight: 800;
  font-size: 13px;
  letter-spacing: 0.02em;
  color: #fff;
  background: linear-gradient(145deg, #2563eb, #6d28d9);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.45);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.ds-fab:hover {
  transform: scale(1.06);
  box-shadow: 0 10px 28px rgba(37, 99, 235, 0.55);
}
</style>
