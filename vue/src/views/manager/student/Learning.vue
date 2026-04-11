<template>
  <div class="learning-page page-shell">
    <div class="header-row">
      <div>
        <h2 class="page-title">专项学习</h2>
        <p class="page-desc">基于课程章节的文档/视频学习入口。</p>
      </div>
      <el-button type="primary" plain @click="loadCourses">刷新</el-button>
    </div>

    <div v-if="courses.length === 0" class="placeholder">暂无课程数据，请先在教师端导入课程内容。</div>

    <div v-else class="course-grid">
      <div
        v-for="course in courses"
        :key="course.id"
        class="course-card"
        :class="{ active: selectedCourseId === course.id }"
        @click="selectCourse(course.id)"
      >
        <div class="course-title">{{ course.title }}</div>
        <div class="course-meta">难度：{{ course.level || "basic" }}</div>
        <div class="course-desc">{{ course.description || "暂无简介" }}</div>
      </div>
    </div>

    <el-card class="section-panel">
      <template #header>
        <div class="section-header">
          <span>章节 / 小节</span>
          <span class="small-text">{{ totalSectionCount }} 个小节</span>
        </div>
      </template>

      <div v-if="outlineChapters.length === 0" class="placeholder small">请选择课程查看章节小节。</div>
      <div v-else class="chapter-list">
        <div v-for="ch in outlineChapters" :key="ch.id" class="chapter-block">
          <div class="chapter-head">
            <div class="chapter-title">{{ ch.title }}</div>
            <div class="chapter-head-right">
              <div class="chapter-stats">
                <span>章节累计学习：{{ formatDuration(ch.totalLearnedSeconds) }}</span>
                <span class="dot">·</span>
                <span>章节笔记：{{ ch.totalNoteCount }} 条</span>
              </div>
              <el-button
                size="small"
                type="info"
                plain
                :disabled="!selectedCourseId"
                @click.stop="openChapterNotes(ch)"
              >
                章节笔记
              </el-button>
              <el-button
                size="small"
                plain
                :disabled="!selectedCourseId"
                @click.stop="openChapterMaterials(ch)"
              >
                章节资料
              </el-button>
            </div>
          </div>

          <div v-if="!ch.sections || ch.sections.length === 0" class="placeholder tiny">该章节下暂无小节（教师新建章节后，可将小节归属到该章）。</div>
          <div v-else class="section-list">
            <div v-for="item in ch.sections" :key="item.id" class="section-item">
              <div class="left">
                <div class="title">{{ item.title }}</div>
                <div class="meta">
                  {{ item.sectionType }} · 预估 {{ formatDuration(item.durationSeconds || 0) }}
                  <span class="dot">·</span>
                  已学 {{ formatDuration(item.learnedSeconds || 0) }}
                  <span class="dot">·</span>
                  笔记 {{ item.noteCount || 0 }} 条
                </div>
              </div>
              <div class="actions">
                <el-button size="small" type="primary" plain @click="openResources(item, 'video')">视频</el-button>
                <el-button size="small" type="success" plain @click="openResources(item, 'doc')">文档</el-button>
                <el-button size="small" plain @click="openNoteDialog(item)">记笔记</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="chapterMaterialsVisible" :title="chapterMaterialsTitle" width="560px" @closed="resetChapterMaterials">
      <div v-if="chapterMaterialsLoading" class="chapter-notes-loading">加载中…</div>
      <div v-else-if="chapterMaterialsList.length" class="material-list">
        <div v-for="(r, idx) in chapterMaterialsList" :key="r.id ?? idx" class="material-row">
          <div class="material-meta">
            <span class="type-tag">{{ r.resource_type || r.resourceType }}</span>
            <span v-if="r.resource_scope || r.resourceScope" class="scope-tag">{{ r.resource_scope || r.resourceScope }}</span>
            <span class="material-title">{{ r.title || "未命名" }}</span>
          </div>
          <el-button size="small" type="primary" plain @click="openExternal(pickResourceUrl(r))">打开</el-button>
        </div>
      </div>
      <div v-else class="note-empty">该章节暂无资料。可使用上方小节的「视频 / 文档」，或由教师在「章节资料」中配置。</div>
    </el-dialog>

    <el-dialog v-model="chapterNotesVisible" :title="chapterNotesTitle" width="640px" @closed="resetChapterNotes">
      <div v-if="chapterNotesLoading" class="chapter-notes-loading">加载中…</div>
      <div v-else-if="chapterNotesList.length" class="note-history chapter-notes-scroll">
        <div v-for="(n, idx) in chapterNotesList" :key="n.id ?? idx" class="note-item">
          <template v-if="editingChapterNoteId === n.id">
            <div class="note-item-meta">
              <span class="section-pill">{{ n.sectionTitle ?? n.section_title ?? "小节" }}</span>
            </div>
            <el-input v-model="chapterNoteEditContent" type="textarea" :rows="4" maxlength="2000" show-word-limit class="note-edit-input" />
            <div class="note-edit-time">
              <span class="note-edit-time-label">时间点（秒）</span>
              <el-input-number v-model="chapterNoteEditTimeSec" :min="0" :max="86400" size="small" />
            </div>
            <div class="note-item-actions">
              <el-button size="small" @click="cancelEditChapterNote">取消</el-button>
              <el-button size="small" type="primary" :loading="chapterNoteEditSaving" @click="submitEditChapterNote(n.id)">保存</el-button>
            </div>
          </template>
          <template v-else>
            <div class="note-item-meta">
              <span class="section-pill">{{ n.sectionTitle ?? n.section_title ?? "小节" }}</span>
              <span class="dot">·</span>
              <span>{{ formatNoteTime(n.createdAt ?? n.created_at) }}</span>
              <span class="dot">·</span>
              <span>时间点 {{ n.noteTimeSec ?? n.note_time_sec ?? 0 }}s</span>
            </div>
            <div class="note-item-body">{{ n.noteContent ?? n.note_content }}</div>
            <div v-if="n.id" class="note-item-actions">
              <el-button size="small" link type="primary" @click="startEditChapterNote(n)">修改</el-button>
              <el-button size="small" link type="danger" @click="deleteChapterNote(n)">删除</el-button>
            </div>
          </template>
        </div>
      </div>
      <div v-else class="note-empty">该章节下暂无笔记。</div>
    </el-dialog>

    <el-dialog v-model="resourcePickVisible" :title="resourcePickTitle" width="520px" @closed="resetResourcePick">
      <p v-if="resourcePickHint" class="resource-pick-hint">{{ resourcePickHint }}</p>
      <div class="resource-pick-list">
        <div v-for="(r, idx) in resourcePickList" :key="r.id ?? idx" class="resource-pick-row">
          <div class="resource-pick-meta">
            <span class="type-tag">{{ r.resource_type || r.resourceType }}</span>
            <span class="resource-pick-name">{{ r.title || "未命名" }}</span>
          </div>
          <el-button size="small" type="primary" plain @click="confirmPickResource(r)">打开</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="noteDialogVisible" title="学习笔记" width="560px" @closed="resetNoteDialog">
      <div v-if="noteTarget" class="note-target">小节：{{ noteTarget.title }}</div>
      <div v-if="noteList.length" class="note-history">
        <div class="note-history-title">最近笔记</div>
        <div v-for="(n, idx) in noteList" :key="n.id ?? idx" class="note-item">
          <template v-if="editingSectionNoteId === n.id">
            <el-input v-model="sectionNoteEditContent" type="textarea" :rows="4" maxlength="2000" show-word-limit class="note-edit-input" />
            <div class="note-edit-time">
              <span class="note-edit-time-label">时间点（秒）</span>
              <el-input-number v-model="sectionNoteEditTimeSec" :min="0" :max="86400" size="small" />
            </div>
            <div class="note-item-actions">
              <el-button size="small" @click="cancelEditSectionNote">取消</el-button>
              <el-button size="small" type="primary" :loading="sectionNoteEditSaving" @click="submitEditSectionNote(n.id)">保存</el-button>
            </div>
          </template>
          <template v-else>
            <div class="note-item-meta">
              <span>{{ formatNoteTime(n.createdAt ?? n.created_at) }}</span>
              <span class="dot">·</span>
              <span>时间点 {{ n.noteTimeSec ?? n.note_time_sec ?? 0 }}s</span>
            </div>
            <div class="note-item-body">{{ n.noteContent ?? n.note_content }}</div>
            <div v-if="n.id" class="note-item-actions">
              <el-button size="small" link type="primary" @click="startEditSectionNote(n)">修改</el-button>
              <el-button size="small" link type="danger" @click="deleteSectionNote(n)">删除</el-button>
            </div>
          </template>
        </div>
      </div>
      <div v-else class="note-empty">暂无笔记，先在下方记录一条。</div>
      <el-input v-model="noteDraft" type="textarea" :rows="6" maxlength="2000" show-word-limit placeholder="记录要点、疑问、易错点…" />
      <template #footer>
        <el-button @click="noteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="noteSaving" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/utils/request";

const courses = ref([]);
const outlineChapters = ref([]);
const selectedCourseId = ref(null);

const totalSectionCount = computed(() =>
  (outlineChapters.value || []).reduce((n, ch) => n + (ch.sections?.length || 0), 0)
);

const noteDialogVisible = ref(false);
const noteTarget = ref(null);
const noteDraft = ref("");
const noteSaving = ref(false);
const noteList = ref([]);

const chapterNotesVisible = ref(false);
const chapterNotesTitle = ref("章节笔记");
const chapterNotesLoading = ref(false);
const chapterNotesList = ref([]);
const chapterNotesChapterId = ref(null);

const editingChapterNoteId = ref(null);
const chapterNoteEditContent = ref("");
const chapterNoteEditTimeSec = ref(0);
const chapterNoteEditSaving = ref(false);

const editingSectionNoteId = ref(null);
const sectionNoteEditContent = ref("");
const sectionNoteEditTimeSec = ref(0);
const sectionNoteEditSaving = ref(false);

const chapterMaterialsVisible = ref(false);
const chapterMaterialsTitle = ref("章节资料");
const chapterMaterialsLoading = ref(false);
const chapterMaterialsList = ref([]);

const resourcePickVisible = ref(false);
const resourcePickTitle = ref("");
const resourcePickHint = ref("");
const resourcePickList = ref([]);
let resourcePickRefreshCourseId = null;

let activeSessionId = null;
let activeSectionId = null;
let sessionStartedAt = 0;
let lastTickAt = 0;
let heartbeatTimer = null;

const formatDuration = (totalSeconds) => {
  const sec = Math.max(0, Math.floor(Number(totalSeconds) || 0));
  if (sec < 60) return `${sec}s`;
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  if (m < 60) return `${m}m${s}s`;
  const h = Math.floor(m / 60);
  const mm = m % 60;
  return `${h}h${mm}m`;
};

const formatNoteTime = (raw) => {
  if (!raw) return "-";
  const d = new Date(raw);
  if (Number.isNaN(d.getTime())) return String(raw);
  return d.toLocaleString();
};

const normalizeNoteRow = (n) => ({
  id: n.id,
  noteContent: n.noteContent ?? n.note_content ?? "",
  noteTimeSec: n.noteTimeSec ?? n.note_time_sec ?? 0,
  createdAt: n.createdAt ?? n.created_at,
});

const normalizeChapterNoteRow = (n) => ({
  id: n.id,
  sectionId: n.sectionId ?? n.section_id,
  sectionTitle: n.sectionTitle ?? n.section_title ?? "",
  noteContent: n.noteContent ?? n.note_content ?? "",
  noteTimeSec: n.noteTimeSec ?? n.note_time_sec ?? 0,
  createdAt: n.createdAt ?? n.created_at,
});

const refreshChapterNotesList = async () => {
  if (!selectedCourseId.value || !chapterNotesChapterId.value) return;
  try {
    const resp = await request.get(
      `/xwd/student/learning/courses/${selectedCourseId.value}/chapters/${chapterNotesChapterId.value}/notes?limit=100`
    );
    if (resp?.status === "success") {
      chapterNotesList.value = (resp.data || []).map(normalizeChapterNoteRow);
    }
  } catch (e) {
    // ignore
  }
};

const refreshSectionNotesList = async () => {
  if (!noteTarget.value?.id) return;
  try {
    const listed = await request.get(`/xwd/student/learning/sections/${noteTarget.value.id}/notes?limit=20`);
    if (listed?.status === "success") {
      noteList.value = (listed.data || []).map(normalizeNoteRow);
    }
  } catch (e) {
    // ignore
  }
};

const openChapterNotes = async (ch) => {
  if (!selectedCourseId.value || !ch.id) {
    ElMessage.warning("无法定位章节，请刷新页面后重试");
    return;
  }
  chapterNotesChapterId.value = ch.id;
  chapterNotesTitle.value = `章节笔记 · ${ch.title}`;
  chapterNotesList.value = [];
  chapterNotesLoading.value = true;
  chapterNotesVisible.value = true;
  try {
    const resp = await request.get(
      `/xwd/student/learning/courses/${selectedCourseId.value}/chapters/${ch.id}/notes?limit=100`
    );
    if (resp?.status === "success") {
      chapterNotesList.value = (resp.data || []).map(normalizeChapterNoteRow);
    } else {
      ElMessage.error(resp?.message || "加载章节笔记失败");
    }
  } catch (e) {
    // 全局拦截器已提示
  } finally {
    chapterNotesLoading.value = false;
  }
};

const resetChapterNotes = () => {
  chapterNotesTitle.value = "章节笔记";
  chapterNotesList.value = [];
  chapterNotesLoading.value = false;
  chapterNotesChapterId.value = null;
  editingChapterNoteId.value = null;
  chapterNoteEditContent.value = "";
  chapterNoteEditTimeSec.value = 0;
  chapterNoteEditSaving.value = false;
};

const startEditChapterNote = (n) => {
  if (!n?.id) return;
  editingChapterNoteId.value = n.id;
  chapterNoteEditContent.value = n.noteContent ?? n.note_content ?? "";
  chapterNoteEditTimeSec.value = Number(n.noteTimeSec ?? n.note_time_sec ?? 0);
};

const cancelEditChapterNote = () => {
  editingChapterNoteId.value = null;
  chapterNoteEditContent.value = "";
  chapterNoteEditTimeSec.value = 0;
};

const submitEditChapterNote = async (noteId) => {
  const text = (chapterNoteEditContent.value || "").trim();
  if (!text) {
    ElMessage.warning("笔记内容不能为空");
    return;
  }
  chapterNoteEditSaving.value = true;
  try {
    const resp = await request.put(`/xwd/student/learning/notes/${noteId}`, {
      content: text,
      timeSec: Math.max(0, Math.floor(Number(chapterNoteEditTimeSec.value) || 0)),
    });
    if (resp?.status === "success") {
      ElMessage.success("已更新");
      cancelEditChapterNote();
      await refreshChapterNotesList();
      if (selectedCourseId.value) {
        await loadCourseOutline(selectedCourseId.value);
      }
    } else {
      ElMessage.error(resp?.message || "更新失败");
    }
  } catch (e) {
    // 全局拦截器
  } finally {
    chapterNoteEditSaving.value = false;
  }
};

const deleteChapterNote = async (n) => {
  if (!n?.id) return;
  try {
    await ElMessageBox.confirm("确定删除这条笔记？", "确认", { type: "warning" });
  } catch {
    return;
  }
  try {
    const resp = await request.delete(`/xwd/student/learning/notes/${n.id}`);
    if (resp?.status === "success") {
      ElMessage.success("已删除");
      if (editingChapterNoteId.value === n.id) {
        cancelEditChapterNote();
      }
      await refreshChapterNotesList();
      if (selectedCourseId.value) {
        await loadCourseOutline(selectedCourseId.value);
      }
    } else {
      ElMessage.error(resp?.message || "删除失败");
    }
  } catch (e) {
    // 全局拦截器
  }
};

const openExternal = (url) => {
  const u = (url || "").trim();
  if (!u) {
    ElMessage.warning("链接无效");
    return;
  }
  window.open(u, "_blank", "noopener,noreferrer");
};

const openChapterMaterials = (ch) => {
  if (!ch?.id) {
    ElMessage.warning("无法定位章节");
    return;
  }
  chapterMaterialsTitle.value = `章节资料 · ${ch.title}`;
  chapterMaterialsList.value = [...(ch.resources || [])];
  chapterMaterialsLoading.value = false;
  chapterMaterialsVisible.value = true;
};

const resetChapterMaterials = () => {
  chapterMaterialsTitle.value = "章节资料";
  chapterMaterialsList.value = [];
  chapterMaterialsLoading.value = false;
};

const takeElapsedSeconds = () => {
  if (!activeSessionId || !lastTickAt) return 0;
  const delta = Math.max(0, Math.floor((Date.now() - lastTickAt) / 1000));
  lastTickAt = Date.now();
  return delta;
};

const sendHeartbeat = async (deltaSeconds) => {
  if (!activeSessionId) return;
  if (!deltaSeconds) return;
  try {
    await request.post(`/xwd/student/learning/sessions/${activeSessionId}/heartbeat`, {
      deltaSeconds,
    });
  } catch (e) {
    // 心跳失败不打扰学习流程
  }
};

const sendEnd = async (finalDeltaSeconds) => {
  if (!activeSessionId) return;
  const sid = activeSessionId;
  try {
    await request.post(`/xwd/student/learning/sessions/${sid}/end`, {
      finalDeltaSeconds: finalDeltaSeconds || 0,
    });
  } catch (e) {
    // ignore
  }
};

const stopTracking = async () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
  const finalDelta = takeElapsedSeconds();
  await sendEnd(finalDelta);
  activeSessionId = null;
  activeSectionId = null;
  sessionStartedAt = 0;
  lastTickAt = 0;
};

const beginTracking = async (sessionId, sectionId) => {
  await stopTracking();
  activeSessionId = sessionId;
  activeSectionId = sectionId;
  sessionStartedAt = Date.now();
  lastTickAt = Date.now();
  heartbeatTimer = setInterval(() => {
    const delta = takeElapsedSeconds();
    void sendHeartbeat(delta);
  }, 15000);
};

const loadCourses = async () => {
  try {
    const response = await request.get("/xwd/student/learning/courses");
    if (response?.status === "success") {
      courses.value = response.data || [];
      if (courses.value.length > 0 && !selectedCourseId.value) {
        await selectCourse(courses.value[0].id);
      }
    } else {
      ElMessage.error(response?.message || "获取课程失败");
    }
  } catch (e) {
    ElMessage.error("获取课程失败");
  }
};

const loadCourseOutline = async (courseId) => {
  selectedCourseId.value = courseId;
  try {
    const response = await request.get(`/xwd/student/learning/courses/${courseId}/outline`);
    if (response?.status === "success") {
      outlineChapters.value = response.data?.chapters || [];
    } else {
      ElMessage.error(response?.message || "获取学习大纲失败");
      outlineChapters.value = [];
    }
  } catch (e) {
    outlineChapters.value = [];
    ElMessage.error("获取学习大纲失败");
  }
};

const selectCourse = async (courseId) => {
  await stopTracking();
  await loadCourseOutline(courseId);
};

const pickResourceUrl = (row) => row?.resource_url || row?.resourceUrl || row?.url;

const resetResourcePick = () => {
  resourcePickTitle.value = "";
  resourcePickHint.value = "";
  resourcePickList.value = [];
  resourcePickRefreshCourseId = null;
};

const confirmPickResource = async (r) => {
  const url = pickResourceUrl(r);
  if (!url) {
    ElMessage.warning("该资源缺少链接地址");
    return;
  }
  window.open(url, "_blank", "noopener,noreferrer");
  resourcePickVisible.value = false;
  const cid = resourcePickRefreshCourseId;
  resetResourcePick();
  if (cid) {
    await loadCourseOutline(cid);
  }
};

const openResources = async (section, type) => {
  let sessionId = null;
  try {
    const started = await request.post(`/xwd/student/learning/sections/${section.id}/start`, {
      deviceInfo: "web",
    });
    sessionId = started?.sessionId ?? null;
    if (started?.status === "success" && sessionId) {
      await beginTracking(sessionId, section.id);
    }
  } catch (e) {
    // 会话记录失败不阻塞学习打开
  }

  try {
    const response = await request.get(`/xwd/student/learning/sections/${section.id}/resources`);
    if (response?.status !== "success") {
      ElMessage.error(response?.message || "获取学习资源失败");
      return;
    }
    const resources = response.data || [];
    if (resources.length === 0) {
      ElMessage.warning("该小节暂未配置学习资源");
      return;
    }
    const list = resources.filter((r) => (r.resource_type || r.resourceType) === type);
    if (list.length === 0) {
      ElMessage.warning(type === "video" ? "该小节暂无视频资源" : "该小节暂无文档资源");
      return;
    }
    const label = type === "video" ? "视频" : "文档";
    if (list.length === 1) {
      const url = pickResourceUrl(list[0]);
      if (!url) {
        ElMessage.warning("该资源缺少链接地址");
        return;
      }
      window.open(url, "_blank", "noopener,noreferrer");
    } else {
      resourcePickTitle.value = `${section.title || "小节"} · 选择${label}`;
      resourcePickHint.value = `该小节有 ${list.length} 个${label}，请按标题选择要打开的一条。`;
      resourcePickList.value = list;
      resourcePickRefreshCourseId = selectedCourseId.value;
      resourcePickVisible.value = true;
      return;
    }
  } catch (e) {
    ElMessage.error("打开学习资源失败");
  }

  if (selectedCourseId.value) {
    await loadCourseOutline(selectedCourseId.value);
  }
};

const startEditSectionNote = (n) => {
  if (!n?.id) return;
  editingSectionNoteId.value = n.id;
  sectionNoteEditContent.value = n.noteContent ?? n.note_content ?? "";
  sectionNoteEditTimeSec.value = Number(n.noteTimeSec ?? n.note_time_sec ?? 0);
};

const cancelEditSectionNote = () => {
  editingSectionNoteId.value = null;
  sectionNoteEditContent.value = "";
  sectionNoteEditTimeSec.value = 0;
};

const submitEditSectionNote = async (noteId) => {
  const text = (sectionNoteEditContent.value || "").trim();
  if (!text) {
    ElMessage.warning("笔记内容不能为空");
    return;
  }
  sectionNoteEditSaving.value = true;
  try {
    const resp = await request.put(`/xwd/student/learning/notes/${noteId}`, {
      content: text,
      timeSec: Math.max(0, Math.floor(Number(sectionNoteEditTimeSec.value) || 0)),
    });
    if (resp?.status === "success") {
      ElMessage.success("已更新");
      cancelEditSectionNote();
      await refreshSectionNotesList();
      if (selectedCourseId.value) {
        await loadCourseOutline(selectedCourseId.value);
      }
    } else {
      ElMessage.error(resp?.message || "更新失败");
    }
  } catch (e) {
    // 全局拦截器
  } finally {
    sectionNoteEditSaving.value = false;
  }
};

const deleteSectionNote = async (n) => {
  if (!n?.id) return;
  try {
    await ElMessageBox.confirm("确定删除这条笔记？", "确认", { type: "warning" });
  } catch {
    return;
  }
  try {
    const resp = await request.delete(`/xwd/student/learning/notes/${n.id}`);
    if (resp?.status === "success") {
      ElMessage.success("已删除");
      if (editingSectionNoteId.value === n.id) {
        cancelEditSectionNote();
      }
      await refreshSectionNotesList();
      if (selectedCourseId.value) {
        await loadCourseOutline(selectedCourseId.value);
      }
    } else {
      ElMessage.error(resp?.message || "删除失败");
    }
  } catch (e) {
    // 全局拦截器
  }
};

const openNoteDialog = async (section) => {
  cancelEditSectionNote();
  noteTarget.value = section;
  noteDraft.value = "";
  noteDialogVisible.value = true;
  noteList.value = [];
  try {
    const resp = await request.get(`/xwd/student/learning/sections/${section.id}/notes?limit=20`);
    if (resp?.status === "success") {
      noteList.value = (resp.data || []).map(normalizeNoteRow);
    } else {
      ElMessage.error(resp?.message || "加载笔记失败");
    }
  } catch (e) {
    // axios 全局拦截器已提示；此处避免重复弹窗
  }
};

const resetNoteDialog = () => {
  noteTarget.value = null;
  noteDraft.value = "";
  noteSaving.value = false;
  noteList.value = [];
  cancelEditSectionNote();
};

const saveNote = async () => {
  if (!noteTarget.value) return;
  const sectionId = noteTarget.value.id;
  const content = (noteDraft.value || "").trim();
  if (!content) {
    ElMessage.warning("请先输入笔记内容");
    return;
  }
  noteSaving.value = true;
  try {
    const timeSec =
      activeSectionId === sectionId && sessionStartedAt
        ? Math.max(0, Math.floor((Date.now() - sessionStartedAt) / 1000))
        : 0;
    const resp = await request.post(`/xwd/student/learning/sections/${sectionId}/notes`, {
      content,
      timeSec,
    });
    if (resp?.status === "success") {
      ElMessage.success("已保存");
      noteDraft.value = "";
      await refreshSectionNotesList();
      if (selectedCourseId.value) {
        await loadCourseOutline(selectedCourseId.value);
      }
    } else {
      ElMessage.error(resp?.message || "保存失败");
    }
  } catch (e) {
    // 全局拦截器已提示
  } finally {
    noteSaving.value = false;
  }
};

const endWithKeepalive = () => {
  if (!activeSessionId) return;
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
  const sid = activeSessionId;
  const finalDelta = takeElapsedSeconds();
  const url = `http://localhost:8080/xwd/student/learning/sessions/${sid}/end`;
  const body = JSON.stringify({ finalDeltaSeconds: finalDelta || 0 });
  try {
    fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body,
      credentials: "include",
      keepalive: true,
    }).catch(() => {});
  } catch (e) {
    // ignore
  }
  activeSessionId = null;
  activeSectionId = null;
  sessionStartedAt = 0;
  lastTickAt = 0;
};

onMounted(() => {
  window.addEventListener("pagehide", endWithKeepalive);
});

onBeforeUnmount(() => {
  window.removeEventListener("pagehide", endWithKeepalive);
  void stopTracking();
});

loadCourses();
</script>

<style scoped>
.learning-page {
  padding: 16px;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.course-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.course-card {
  border: 1px solid #d8e1f0;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
  cursor: pointer;
}
.course-card.active {
  border-color: rgba(37, 99, 235, 0.45);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15) inset;
}
.course-title {
  font-weight: 700;
}
.course-meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}
.course-desc {
  margin-top: 8px;
  color: #475569;
}
.section-panel {
  margin-top: 14px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.small-text {
  color: #64748b;
  font-size: 12px;
}
.chapter-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chapter-block {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px;
  background: #fbfdff;
}
.chapter-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  padding: 6px 6px 10px;
}
.chapter-head-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}
.chapter-title {
  font-weight: 800;
  color: #0f172a;
}
.chapter-stats {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}
.chapter-notes-loading {
  padding: 16px 6px;
  color: #64748b;
  font-size: 14px;
}
.chapter-notes-scroll {
  margin: 0;
  max-height: min(60vh, 420px);
}
.section-pill {
  font-weight: 600;
  color: #334155;
}
.dot {
  margin: 0 6px;
  color: #94a3b8;
}
.section-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.section-item {
  border: 1px solid #d8e1f0;
  border-radius: 10px;
  padding: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
.meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}
.placeholder {
  margin-top: 14px;
  padding: 14px;
  border: 1px dashed #c6d4ee;
  border-radius: 10px;
  color: #64748b;
}
.placeholder.small {
  margin-top: 0;
}
.placeholder.tiny {
  margin: 8px 6px;
  padding: 8px 10px;
  font-size: 13px;
  color: #64748b;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
  background: #fff;
}
.scope-tag {
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}
.note-target {
  margin-bottom: 10px;
  color: #475569;
  font-size: 13px;
}
.note-history {
  margin: 10px 0 12px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  max-height: 220px;
  overflow: auto;
}
.note-history-title {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}
.note-item {
  padding: 8px 0;
  border-top: 1px dashed #e2e8f0;
}
.note-item:first-of-type {
  border-top: none;
  padding-top: 0;
}
.note-item-meta {
  font-size: 12px;
  color: #64748b;
}
.note-item-body {
  margin-top: 6px;
  color: #0f172a;
  white-space: pre-wrap;
  line-height: 1.45;
}
.note-item-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.note-edit-input {
  margin-top: 8px;
}
.note-edit-time {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.note-edit-time-label {
  font-size: 12px;
  color: #64748b;
}
.note-empty {
  margin: 10px 0 12px;
  color: #64748b;
  font-size: 13px;
}
.material-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: min(50vh, 360px);
  overflow: auto;
}
.material-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
}
.material-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}
.type-tag {
  font-size: 12px;
  color: #0369a1;
  background: #e0f2fe;
  padding: 2px 8px;
  border-radius: 999px;
  flex-shrink: 0;
}
.material-title {
  font-size: 14px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.resource-pick-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}
.resource-pick-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: min(55vh, 400px);
  overflow: auto;
}
.resource-pick-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
}
.resource-pick-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}
.resource-pick-name {
  font-size: 14px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 960px) {
  .course-grid {
    grid-template-columns: 1fr;
  }
  .chapter-head {
    flex-direction: column;
    align-items: flex-start;
  }
  .chapter-head-right {
    width: 100%;
    justify-content: space-between;
  }
  .chapter-stats {
    white-space: normal;
  }
}
</style>

