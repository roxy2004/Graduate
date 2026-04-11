<template>
  <div class="session-page page-shell">
    <div class="top-bar">
      <el-button text type="primary" @click="back">← 返回知识点</el-button>
      <div v-if="summary" class="summary">
        {{ summary.name || "知识点" }} · 已刷 {{ practiced }} / {{ total }} 题
      </div>
      <el-button type="danger" plain size="small" @click="clearRecords">清空本题集记录</el-button>
    </div>

    <div v-if="loading" class="placeholder">加载题目…</div>
    <div v-else-if="deck.length === 0" class="placeholder">该知识点下暂无可用题目。</div>
    <template v-else>
      <div class="pager-row">
        <span class="pager">{{ currentIndex + 1 }} / {{ deck.length }}</span>
        <span v-if="currentQuestion" class="live-timer" title="从进入本题页到提交前累计（最多计 60 分钟）">
          本题计时 {{ formatMmSs(currentElapsedSeconds) }}
        </span>
        <span class="pager-hint">左右滑动或按钮切换，无需按顺序做题</span>
      </div>

      <div
        class="viewport"
        @touchstart="onTouchStart"
        @touchend="onTouchEnd"
      >
        <div
          class="strip"
          :style="stripStyle"
        >
          <div
            v-for="item in deck"
            :key="item.id"
            class="slide"
            :style="{ flex: `0 0 calc(100% / ${deck.length})` }"
          >
            <div class="slide-inner">
              <div v-if="recordFor(item)" class="history-banner" :class="recordFor(item).isCorrect ? 'ok' : 'bad'">
                最近一次作答：<strong>{{ recordFor(item).userAnswer }}</strong>
                · {{ recordFor(item).isCorrect ? "正确" : "错误" }}
                <span v-if="priorTimeSpentText(recordFor(item))" class="t-small">
                  · {{ priorTimeSpentText(recordFor(item)) }}
                </span>
                <span v-if="recordFor(item).answeredAt" class="t-small">
                  · {{ formatTime(recordFor(item).answeredAt) }}
                </span>
              </div>

              <div class="card-face">
                <div class="card-tag">{{ item.questionType || "选择题" }}</div>
                <div class="stem">{{ item.content || "（无题干）" }}</div>
              </div>

              <div class="options-block">
                <div
                  v-for="opt in parseOptions(item.options)"
                  :key="opt.key"
                  class="opt-btn"
                  :class="optionClass(item, opt.key)"
                  @click="pick(item.id, opt.key)"
                >
                  <span class="k">{{ opt.key }}</span>
                  <span class="t">{{ opt.value }}</span>
                </div>
              </div>

              <div class="actions">
                <el-button type="primary" :disabled="!picked[item.id] || submittingId === item.id" :loading="submittingId === item.id" @click="submit(item)">
                  提交本题
                </el-button>
              </div>

              <div v-if="localResult[item.id]" class="fresh-result" :class="localResult[item.id].isCorrect ? 'ok' : 'bad'">
                本次提交：{{ localResult[item.id].isCorrect ? "正确" : "错误" }} · 正确答案
                <strong>{{ localResult[item.id].correctAnswer }}</strong>
                <span v-if="localResult[item.id].timeSpent != null" class="t-small">
                  · 本次用时 {{ formatDurationHuman(localResult[item.id].timeSpent) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="nav-row">
        <el-button :disabled="currentIndex <= 0" @click="go(-1)">上一题</el-button>
        <el-button type="primary" plain :disabled="currentIndex >= deck.length - 1" @click="go(1)">下一题</el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/utils/request";

/** 当前停留在该题（滑动到该页）时的开始时间戳，用于提交 timeSpent */
const questionPageEnteredAt = reactive({});

const route = useRoute();
const router = useRouter();

const kpId = computed(() => Number(route.params.kpId));

const loading = ref(true);
const summary = ref(null);
const deck = ref([]);
const currentIndex = ref(0);

const picked = reactive({});
const localResult = reactive({});
const submittingId = ref(null);

const touchStartX = ref(0);

/** 每秒递增，驱动「本题计时」展示 */
const secondTick = ref(0);
let secondTimer = null;

const currentQuestion = computed(() => {
  const d = deck.value;
  const i = currentIndex.value;
  if (!d.length || i < 0 || i >= d.length) return null;
  return d[i];
});

const currentElapsedSeconds = computed(() => {
  secondTick.value;
  const id = currentQuestion.value?.id;
  if (id == null) return 0;
  const start = questionPageEnteredAt[id];
  if (start == null) return 0;
  return Math.min(3600, Math.max(0, Math.floor((Date.now() - start) / 1000)));
});

const formatMmSs = (totalSec) => {
  const s = Math.max(0, Math.min(3600, Number(totalSec) || 0));
  const m = Math.floor(s / 60);
  const sec = s % 60;
  return `${String(m).padStart(2, "0")}:${String(sec).padStart(2, "0")}`;
};

const formatDurationHuman = (totalSec) => {
  const sec = Math.floor(Math.max(0, Math.min(3600, Number(totalSec) || 0)));
  if (sec < 60) return `${sec} 秒`;
  const m = Math.floor(sec / 60);
  const r = sec % 60;
  return r ? `${m} 分 ${r} 秒` : `${m} 分`;
};

const priorTimeSpentText = (rec) => {
  if (!rec) return "";
  const raw = rec.timeSpent ?? rec.time_spent;
  if (raw === null || raw === undefined || raw === "") return "";
  const n = Number(raw);
  if (!Number.isFinite(n) || n < 0) return "";
  return `上次用时 ${formatDurationHuman(n)}`;
};

const markCurrentQuestionPageEntered = () => {
  const item = deck.value[currentIndex.value];
  if (item?.id) {
    questionPageEnteredAt[item.id] = Date.now();
  }
};

watch([currentIndex, () => deck.value.length], () => {
  markCurrentQuestionPageEntered();
});

const practiced = computed(() =>
  Number(summary.value?.practicedQuestions ?? summary.value?.practiced_questions ?? 0)
);
const total = computed(() => Number(summary.value?.totalQuestions ?? summary.value?.total_questions ?? 0));

const stripStyle = computed(() => {
  const n = deck.value.length || 1;
  return {
    width: `${n * 100}%`,
    transform: `translateX(calc(-${currentIndex.value} * 100% / ${n}))`,
    transition: "transform 0.28s ease",
  };
});

const recordFor = (item) => {
  const loc = localResult[item.id];
  if (loc) {
    return {
      userAnswer: loc.userAnswer,
      isCorrect: loc.isCorrect,
      answeredAt: loc.answeredAt,
      timeSpent: loc.timeSpent,
    };
  }
  const last = item.lastAttempt || null;
  if (!last) return null;
  const ts = last.timeSpent ?? last.time_spent;
  if (ts === null || ts === undefined || ts === "") {
    return { ...last };
  }
  return { ...last, timeSpent: Number(ts) || 0 };
};

const loadSummary = async () => {
  try {
    const resp = await request.get(`/xwd/student/practice/knowledge-points/${kpId.value}/summary`);
    if (resp?.status === "success") {
      summary.value = resp.data;
    }
  } catch (e) {
    // ignore
  }
};

const loadDeck = async () => {
  loading.value = true;
  try {
    const resp = await request.get(`/xwd/student/practice/knowledge-points/${kpId.value}/deck`);
    if (resp?.status === "success") {
      const rows = resp.data || [];
      deck.value = rows.map((r) => {
        const la = r.lastAttempt || null;
        const normalizedLast =
          la == null
            ? null
            : {
                ...la,
                timeSpent: la.timeSpent ?? la.time_spent ?? null,
              };
        return { ...r, lastAttempt: normalizedLast };
      });
      Object.keys(picked).forEach((k) => delete picked[k]);
      Object.keys(localResult).forEach((k) => delete localResult[k]);
      currentIndex.value = 0;
      markCurrentQuestionPageEntered();
    } else {
      deck.value = [];
    }
  } catch (e) {
    deck.value = [];
  } finally {
    loading.value = false;
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

const pick = (qid, key) => {
  picked[qid] = key;
};

const optionClass = (item, key) => {
  const rec = recordFor(item);
  const cls = [];
  const p = picked[item.id];
  if (p === key) cls.push("picked");
  if (rec) {
    const ua = String(rec.userAnswer || "").toUpperCase();
    const ca = String(localResult[item.id]?.correctAnswer || item.correctAnswer || "").toUpperCase();
    if (rec.isCorrect && ua === key) cls.push("is-correct");
    if (!rec.isCorrect) {
      if (ua === key) cls.push("is-wrong");
      if (ca && ca === key) cls.push("is-correct");
    }
  }
  return cls;
};

const formatTime = (raw) => {
  if (!raw) return "";
  const d = new Date(raw);
  if (Number.isNaN(d.getTime())) return String(raw);
  return d.toLocaleString();
};

const submit = async (item) => {
  const qid = item.id;
  const ans = picked[qid];
  if (!ans) {
    ElMessage.warning("请选择选项");
    return;
  }
  submittingId.value = qid;
  const started = questionPageEnteredAt[qid];
  const timeSpentSec =
    started == null ? 0 : Math.max(0, Math.min(3600, Math.floor((Date.now() - started) / 1000)));
  try {
    const resp = await request.post(`/xwd/student/practice/knowledge-points/${kpId.value}/attempts`, {
      questionId: qid,
      userAnswer: ans,
      timeSpent: timeSpentSec,
    });
    if (resp?.status === "success") {
      const ok = !!resp.isCorrect;
      const ca = (resp.correctAnswer || "").toString().toUpperCase();
      localResult[qid] = {
        isCorrect: ok,
        correctAnswer: ca,
        userAnswer: ans,
        answeredAt: new Date().toISOString(),
        timeSpent: timeSpentSec,
      };
      const row = deck.value.find((x) => x.id === qid);
      if (row) {
        row.lastAttempt = {
          userAnswer: ans,
          isCorrect: ok,
          answeredAt: localResult[qid].answeredAt,
          timeSpent: timeSpentSec,
        };
        row.correctAnswer = ca;
      }
      await loadSummary();
      questionPageEnteredAt[qid] = Date.now();
      ElMessage.success(ok ? "回答正确" : "回答错误，已记录");
    } else {
      ElMessage.error(resp?.message || "提交失败");
    }
  } catch (e) {
    ElMessage.error("提交失败");
  } finally {
    submittingId.value = null;
  }
};

const go = (delta) => {
  const n = deck.value.length;
  const next = currentIndex.value + delta;
  if (next < 0 || next >= n) return;
  currentIndex.value = next;
};

const onTouchStart = (e) => {
  if (!e.changedTouches?.length) return;
  touchStartX.value = e.changedTouches[0].clientX;
};

const onTouchEnd = (e) => {
  if (!e.changedTouches?.length) return;
  const dx = e.changedTouches[0].clientX - touchStartX.value;
  if (dx > 56) go(-1);
  else if (dx < -56) go(1);
};

const clearRecords = async () => {
  try {
    await ElMessageBox.confirm("将删除该知识点下你的全部做题记录（含错题分析关联），确定？", "清空记录", {
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    const resp = await request.delete(`/xwd/student/practice/knowledge-points/${kpId.value}/records`);
    if (resp?.status === "success") {
      ElMessage.success(resp.message || "已清空");
      await loadSummary();
      await loadDeck();
    } else {
      ElMessage.error(resp?.message || "清空失败");
    }
  } catch (e) {
    ElMessage.error("清空失败");
  }
};

const back = () => {
  router.push("/manager/student/practice");
};

watch(
  () => route.params.kpId,
  async () => {
    await loadSummary();
    await loadDeck();
  }
);

onMounted(async () => {
  secondTimer = setInterval(() => {
    secondTick.value += 1;
  }, 1000);
  await loadSummary();
  await loadDeck();
  markCurrentQuestionPageEntered();
});

onBeforeUnmount(() => {
  if (secondTimer) {
    clearInterval(secondTimer);
    secondTimer = null;
  }
});
</script>

<style scoped>
.session-page {
  padding: 16px;
  max-width: 720px;
  margin: 0 auto;
}
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.summary {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: #475569;
}
.placeholder {
  padding: 24px;
  text-align: center;
  color: #64748b;
  border: 1px dashed #e2e8f0;
  border-radius: 12px;
}
.pager-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
  row-gap: 8px;
}
.pager {
  font-weight: 700;
  color: #1e40af;
  font-size: 15px;
}
.pager-hint {
  font-size: 12px;
  color: #94a3b8;
}
.live-timer {
  font-variant-numeric: tabular-nums;
  font-size: 14px;
  font-weight: 600;
  color: #0f766e;
  white-space: nowrap;
}
.viewport {
  overflow: hidden;
  width: 100%;
  border-radius: 14px;
  touch-action: pan-y;
}
.strip {
  display: flex;
}
.slide {
  box-sizing: border-box;
  padding: 0 4px;
}
.slide-inner {
  min-height: 280px;
}
.history-banner {
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 10px;
  font-size: 13px;
  line-height: 1.45;
}
.history-banner.ok {
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #6ee7b7;
}
.history-banner.bad {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fca5a5;
}
.t-small {
  font-weight: 400;
  opacity: 0.85;
}
.card-face {
  border-radius: 14px;
  padding: 16px;
  border: 1px solid #dbe4f5;
  background: linear-gradient(165deg, #ffffff 0%, #f4f8ff 100%);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
}
.card-tag {
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
}
.stem {
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.55;
  color: #0f172a;
  white-space: pre-wrap;
}
.options-block {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 14px;
}
@media (max-width: 520px) {
  .options-block {
    grid-template-columns: 1fr;
  }
}
.opt-btn {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  background: #fff;
  transition: border-color 0.15s, background 0.15s;
}
.opt-btn:hover {
  border-color: #93c5fd;
  background: #f8fbff;
}
.opt-btn.picked {
  border-color: #2563eb;
  background: #eff6ff;
}
.opt-btn.is-correct {
  border-color: #10b981;
  background: #ecfdf5;
}
.opt-btn.is-wrong {
  border-color: #f87171;
  background: #fef2f2;
}
.opt-btn .k {
  font-weight: 800;
  color: #1d4ed8;
  width: 22px;
  flex-shrink: 0;
}
.opt-btn .t {
  font-size: 14px;
  color: #334155;
  line-height: 1.4;
}
.actions {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
.nav-row {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}
.fresh-result {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
}
.fresh-result.ok {
  background: #ecfdf5;
  color: #065f46;
}
.fresh-result.bad {
  background: #fef2f2;
  color: #991b1b;
}
</style>
