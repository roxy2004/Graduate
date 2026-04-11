<template>
  <div class="teacher-chapters">
    <el-card class="page-shell">
      <template #header>
        <div class="card-header">
          <div>
            <h3 class="page-title">章节与资料</h3>
            <p class="page-desc">维护章节与小节；视频 / 文档请在小节中配置（同一小节可多条）。章节级入口仅作整章补充资料。</p>
          </div>
          <el-button type="primary" plain @click="reloadAll">刷新</el-button>
        </div>
      </template>

      <div class="toolbar">
        <span class="label">课程</span>
        <el-select v-model="courseId" placeholder="选择课程" style="min-width: 260px" filterable @change="loadChapters">
          <el-option v-for="c in courses" :key="c.id" :label="c.title || c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" :disabled="!courseId" @click="openAddChapter">新增章节</el-button>
      </div>

      <el-table v-if="courseId" :data="chapters" style="width: 100%; margin-top: 14px" v-loading="chaptersLoading">
        <el-table-column prop="sortNo" label="排序" width="90" />
        <el-table-column prop="title" label="章节标题" min-width="220" />
        <el-table-column label="操作" width="420" fixed="right">
          <template #default="{ row }">
            <el-button size="small" plain @click="openEditChapter(row)">编辑</el-button>
            <el-button size="small" type="success" plain @click="openSectionManage(row)">小节</el-button>
            <el-button size="small" type="primary" plain @click="openResources(row)">章节资料</el-button>
            <el-button size="small" type="danger" plain @click="removeChapter(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="hint">请先选择一门课程。</div>
    </el-card>

    <el-dialog v-model="chapterFormVisible" :title="chapterFormMode === 'add' ? '新增章节' : '编辑章节'" width="480px" @closed="resetChapterForm">
      <el-form label-width="88px">
        <el-form-item label="标题">
          <el-input v-model="chapterForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="chapterForm.sortNo" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="chapterSaving" @click="saveChapter">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sectionManageVisible" :title="`小节 · ${sectionChapter?.title || ''}`" width="820px" @closed="resetSectionManage">
      <div class="section-manage-toolbar">
        <el-button type="primary" size="small" :disabled="!sectionChapter" @click="openAddSection">新增小节</el-button>
      </div>
      <el-table :data="managedSections" size="small" style="width: 100%; margin-top: 10px" v-loading="managedSectionsLoading">
        <el-table-column prop="sortNo" label="排序" width="72" />
        <el-table-column prop="title" label="小节标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="预估(分)" width="96">
          <template #default="{ row }">{{ sectionMinutesDisplay(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" plain @click="openEditSection(row)">编辑</el-button>
            <el-button size="small" type="primary" plain @click="openSectionResources(row)">视频/文档</el-button>
            <el-button size="small" type="danger" plain @click="removeSection(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="sectionFormVisible" :title="sectionFormMode === 'add' ? '新增小节' : '编辑小节'" width="480px" @closed="resetSectionForm">
      <el-form label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="sectionForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="sectionForm.sortNo" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="预估时长(分)">
          <el-input-number v-model="sectionForm.estimatedMinutes" :min="1" :max="600" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sectionFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="sectionSaving" @click="saveSection">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="secResDialogVisible" :title="`小节资源 · ${sectionForRes?.title || ''}`" width="720px" @closed="resetSecResDialog">
      <div class="res-toolbar">
        <el-select v-model="secResForm.resourceType" style="width: 110px">
          <el-option label="视频" value="video" />
          <el-option label="文档" value="doc" />
        </el-select>
        <el-input v-model="secResForm.title" placeholder="标题（便于学生区分多个资源）" style="width: 220px" maxlength="200" />
        <el-input v-model="secResForm.url" placeholder="https://..." style="flex: 1" maxlength="2000" />
        <el-button type="primary" :loading="secResAdding" @click="addSectionResource">添加</el-button>
      </div>
      <el-table :data="sectionResources" size="small" style="width: 100%; margin-top: 12px" v-loading="secResLoading">
        <el-table-column prop="resource_type" label="类型" width="90" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="链接" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.resource_url || row.resourceUrl }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" link @click="deleteSectionResource(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="resDialogVisible" :title="`章节级补充资料 · ${resourceChapter?.title || ''}`" width="720px" @closed="resetResDialog">
      <div class="res-toolbar">
        <el-select v-model="resForm.resourceType" style="width: 110px">
          <el-option label="视频" value="video" />
          <el-option label="文档" value="doc" />
        </el-select>
        <el-input v-model="resForm.title" placeholder="标题" style="width: 200px" maxlength="200" />
        <el-input v-model="resForm.url" placeholder="https://..." style="flex: 1" maxlength="2000" />
        <el-button type="primary" :loading="resAdding" @click="addResource">添加</el-button>
      </div>
      <el-table :data="resources" size="small" style="width: 100%; margin-top: 12px" v-loading="resLoading">
        <el-table-column prop="resource_type" label="类型" width="90" />
        <el-table-column label="归属" width="100">
          <template #default="{ row }">
            {{ scopeLabel(row.resource_scope || row.resourceScope) }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="链接" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.resource_url || row.resourceUrl }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" link @click="deleteResource(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/utils/request";

const courses = ref([]);
const courseId = ref(null);
const chapters = ref([]);
const chaptersLoading = ref(false);

const chapterFormVisible = ref(false);
const chapterFormMode = ref("add");
const chapterSaving = ref(false);
const chapterForm = reactive({ id: null, title: "", sortNo: 0 });

const resDialogVisible = ref(false);
const resourceChapter = ref(null);
const resources = ref([]);
const resLoading = ref(false);
const resAdding = ref(false);
const resForm = reactive({ resourceType: "doc", title: "", url: "" });

const sectionManageVisible = ref(false);
const sectionChapter = ref(null);
const managedSections = ref([]);
const managedSectionsLoading = ref(false);

const sectionFormVisible = ref(false);
const sectionFormMode = ref("add");
const sectionSaving = ref(false);
const sectionForm = reactive({ id: null, title: "", sortNo: 0, estimatedMinutes: 10 });

const secResDialogVisible = ref(false);
const sectionForRes = ref(null);
const sectionResources = ref([]);
const secResLoading = ref(false);
const secResAdding = ref(false);
const secResForm = reactive({ resourceType: "doc", title: "", url: "" });

const scopeLabel = (scope) => {
  if (scope === "section") return "小节";
  if (scope === "chapter") return "章节";
  return scope || "—";
};

const sectionMinutesDisplay = (row) => {
  if (row == null) return "—";
  if (row.estimatedMinutes != null && row.estimatedMinutes !== "") {
    return row.estimatedMinutes;
  }
  const ds = row.durationSeconds;
  if (ds != null && ds !== "") {
    return Math.max(1, Math.round(Number(ds) / 60));
  }
  return "—";
};

const loadCourses = async () => {
  try {
    const resp = await request.get("/xwd/teacher/course-manage/courses");
    if (resp?.status === "success") {
      courses.value = resp.data || [];
      if (!courseId.value && courses.value.length > 0) {
        courseId.value = courses.value[0].id;
        await loadChapters();
      }
    } else {
      ElMessage.error(resp?.message || "加载课程失败");
    }
  } catch (e) {
    // 全局拦截器
  }
};

const loadChapters = async () => {
  if (!courseId.value) {
    chapters.value = [];
    return;
  }
  chaptersLoading.value = true;
  try {
    const resp = await request.get(`/xwd/teacher/course-manage/courses/${courseId.value}/chapters`);
    if (resp?.status === "success") {
      chapters.value = resp.data || [];
    } else {
      ElMessage.error(resp?.message || "加载章节失败");
    }
  } finally {
    chaptersLoading.value = false;
  }
};

const reloadAll = async () => {
  await loadCourses();
  await loadChapters();
};

const openAddChapter = () => {
  chapterFormMode.value = "add";
  chapterForm.id = null;
  chapterForm.title = "";
  chapterForm.sortNo = (chapters.value?.length || 0) + 1;
  chapterFormVisible.value = true;
};

const openEditChapter = (row) => {
  chapterFormMode.value = "edit";
  chapterForm.id = row.id;
  chapterForm.title = row.title || "";
  chapterForm.sortNo = row.sortNo ?? 0;
  chapterFormVisible.value = true;
};

const resetChapterForm = () => {
  chapterForm.id = null;
  chapterForm.title = "";
  chapterForm.sortNo = 0;
  chapterSaving.value = false;
};

const saveChapter = async () => {
  if (!courseId.value) return;
  const title = (chapterForm.title || "").trim();
  if (!title) {
    ElMessage.warning("请填写章节标题");
    return;
  }
  chapterSaving.value = true;
  try {
    if (chapterFormMode.value === "add") {
      const resp = await request.post(`/xwd/teacher/course-manage/courses/${courseId.value}/chapters`, {
        title,
        sortNo: chapterForm.sortNo,
      });
      if (resp?.status === "success") {
        ElMessage.success("已新增章节");
        chapterFormVisible.value = false;
        await loadChapters();
      } else {
        ElMessage.error(resp?.message || "保存失败");
      }
    } else {
      const resp = await request.put(`/xwd/teacher/course-manage/chapters/${chapterForm.id}`, {
        title,
        sortNo: chapterForm.sortNo,
      });
      if (resp?.status === "success") {
        ElMessage.success("已保存");
        chapterFormVisible.value = false;
        await loadChapters();
      } else {
        ElMessage.error(resp?.message || "保存失败");
      }
    }
  } finally {
    chapterSaving.value = false;
  }
};

const removeChapter = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除章节「${row.title}」及其章节下配置的资源？`, "确认", { type: "warning" });
  } catch {
    return;
  }
  try {
    const resp = await request.delete(`/xwd/teacher/course-manage/chapters/${row.id}`);
    if (resp?.status === "success") {
      ElMessage.success("已删除");
      await loadChapters();
    } else {
      ElMessage.error(resp?.message || "删除失败");
    }
  } catch (e) {
    // 全局拦截器
  }
};

const loadResources = async (chapterId) => {
  resLoading.value = true;
  try {
    const resp = await request.get(`/xwd/teacher/course-manage/chapters/${chapterId}/resources`);
    if (resp?.status === "success") {
      resources.value = resp.data || [];
    } else {
      resources.value = [];
      ElMessage.error(resp?.message || "加载资源失败");
    }
  } finally {
    resLoading.value = false;
  }
};

const openResources = async (row) => {
  resourceChapter.value = row;
  resForm.resourceType = "doc";
  resForm.title = "";
  resForm.url = "";
  resDialogVisible.value = true;
  await loadResources(row.id);
};

const resetResDialog = () => {
  resourceChapter.value = null;
  resources.value = [];
  resForm.title = "";
  resForm.url = "";
  resForm.resourceType = "doc";
};

const addResource = async () => {
  const ch = resourceChapter.value;
  if (!ch?.id) return;
  const url = (resForm.url || "").trim();
  if (!url) {
    ElMessage.warning("请填写资源链接");
    return;
  }
  resAdding.value = true;
  try {
    const resp = await request.post(`/xwd/teacher/course-manage/chapters/${ch.id}/resources`, {
      resourceType: resForm.resourceType,
      title: (resForm.title || "").trim() || (resForm.resourceType === "video" ? "视频" : "文档"),
      url,
    });
    if (resp?.status === "success") {
      ElMessage.success("已添加");
      resForm.url = "";
      resForm.title = "";
      await loadResources(ch.id);
    } else {
      ElMessage.error(resp?.message || "添加失败");
    }
  } finally {
    resAdding.value = false;
  }
};

const deleteResource = async (row) => {
  const id = row.id;
  if (!id) return;
  try {
    await ElMessageBox.confirm("确定删除该条资源？", "确认", { type: "warning" });
  } catch {
    return;
  }
  try {
    const resp = await request.delete(`/xwd/teacher/course-manage/resources/${id}`);
    if (resp?.status === "success") {
      ElMessage.success("已删除");
      await loadResources(resourceChapter.value.id);
    } else {
      ElMessage.error(resp?.message || "删除失败");
    }
  } catch (e) {
    // 全局拦截器
  }
};

const loadManagedSections = async () => {
  const ch = sectionChapter.value;
  if (!ch?.id || !courseId.value) {
    managedSections.value = [];
    return;
  }
  managedSectionsLoading.value = true;
  try {
    const resp = await request.get(
      `/xwd/teacher/course-manage/courses/${courseId.value}/chapters/${ch.id}/sections`
    );
    if (resp?.status === "success") {
      managedSections.value = resp.data || [];
    } else {
      managedSections.value = [];
      ElMessage.error(resp?.message || "加载小节失败");
    }
  } finally {
    managedSectionsLoading.value = false;
  }
};

const openSectionManage = async (row) => {
  sectionChapter.value = row;
  sectionManageVisible.value = true;
  await loadManagedSections();
};

const resetSectionManage = () => {
  sectionChapter.value = null;
  managedSections.value = [];
};

const openAddSection = () => {
  sectionFormMode.value = "add";
  sectionForm.id = null;
  sectionForm.title = "";
  sectionForm.sortNo = (managedSections.value?.length || 0) + 1;
  sectionForm.estimatedMinutes = 10;
  sectionFormVisible.value = true;
};

const openEditSection = (row) => {
  sectionFormMode.value = "edit";
  sectionForm.id = row.id;
  sectionForm.title = row.title || "";
  sectionForm.sortNo = row.sortNo ?? 0;
  sectionForm.estimatedMinutes = row.estimatedMinutes ?? Math.max(1, Math.round((row.durationSeconds || 60) / 60));
  sectionFormVisible.value = true;
};

const resetSectionForm = () => {
  sectionForm.id = null;
  sectionForm.title = "";
  sectionForm.sortNo = 0;
  sectionForm.estimatedMinutes = 10;
  sectionSaving.value = false;
};

const saveSection = async () => {
  const ch = sectionChapter.value;
  if (!ch?.id || !courseId.value) return;
  const title = (sectionForm.title || "").trim();
  if (!title) {
    ElMessage.warning("请填写小节标题");
    return;
  }
  sectionSaving.value = true;
  try {
    if (sectionFormMode.value === "add") {
      const resp = await request.post(
        `/xwd/teacher/course-manage/courses/${courseId.value}/chapters/${ch.id}/sections`,
        {
          title,
          sortNo: sectionForm.sortNo,
          estimatedMinutes: sectionForm.estimatedMinutes,
        }
      );
      if (resp?.status === "success") {
        ElMessage.success("已新增小节");
        sectionFormVisible.value = false;
        await loadManagedSections();
      } else {
        ElMessage.error(resp?.message || "保存失败");
      }
    } else {
      const resp = await request.put(`/xwd/teacher/course-manage/sections/${sectionForm.id}`, {
        title,
        sortNo: sectionForm.sortNo,
        estimatedMinutes: sectionForm.estimatedMinutes,
      });
      if (resp?.status === "success") {
        ElMessage.success("已保存");
        sectionFormVisible.value = false;
        await loadManagedSections();
      } else {
        ElMessage.error(resp?.message || "保存失败");
      }
    }
  } finally {
    sectionSaving.value = false;
  }
};

const removeSection = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除小节「${row.title}」及其下全部视频/文档？`, "确认", { type: "warning" });
  } catch {
    return;
  }
  try {
    const resp = await request.delete(`/xwd/teacher/course-manage/sections/${row.id}`);
    if (resp?.status === "success") {
      ElMessage.success("已删除");
      await loadManagedSections();
    } else {
      ElMessage.error(resp?.message || "删除失败");
    }
  } catch (e) {
    // 全局拦截器
  }
};

const loadSectionResources = async (sectionId) => {
  secResLoading.value = true;
  try {
    const resp = await request.get(`/xwd/teacher/course-manage/sections/${sectionId}/resources`);
    if (resp?.status === "success") {
      sectionResources.value = resp.data || [];
    } else {
      sectionResources.value = [];
      ElMessage.error(resp?.message || "加载资源失败");
    }
  } finally {
    secResLoading.value = false;
  }
};

const openSectionResources = async (row) => {
  sectionForRes.value = row;
  secResForm.resourceType = "doc";
  secResForm.title = "";
  secResForm.url = "";
  secResDialogVisible.value = true;
  await loadSectionResources(row.id);
};

const resetSecResDialog = () => {
  sectionForRes.value = null;
  sectionResources.value = [];
  secResForm.title = "";
  secResForm.url = "";
  secResForm.resourceType = "doc";
};

const addSectionResource = async () => {
  const sec = sectionForRes.value;
  if (!sec?.id) return;
  const url = (secResForm.url || "").trim();
  if (!url) {
    ElMessage.warning("请填写资源链接");
    return;
  }
  secResAdding.value = true;
  try {
    const resp = await request.post(`/xwd/teacher/course-manage/sections/${sec.id}/resources`, {
      resourceType: secResForm.resourceType,
      title: (secResForm.title || "").trim() || (secResForm.resourceType === "video" ? "视频" : "文档"),
      url,
    });
    if (resp?.status === "success") {
      ElMessage.success("已添加");
      secResForm.url = "";
      secResForm.title = "";
      await loadSectionResources(sec.id);
    } else {
      ElMessage.error(resp?.message || "添加失败");
    }
  } finally {
    secResAdding.value = false;
  }
};

const deleteSectionResource = async (row) => {
  const id = row.id;
  if (!id) return;
  try {
    await ElMessageBox.confirm("确定删除该条资源？", "确认", { type: "warning" });
  } catch {
    return;
  }
  try {
    const resp = await request.delete(`/xwd/teacher/course-manage/resources/${id}`);
    if (resp?.status === "success") {
      ElMessage.success("已删除");
      await loadSectionResources(sectionForRes.value.id);
    } else {
      ElMessage.error(resp?.message || "删除失败");
    }
  } catch (e) {
    // 全局拦截器
  }
};

onMounted(() => {
  loadCourses();
});
</script>

<style scoped>
.teacher-chapters {
  background: transparent;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.toolbar .label {
  color: #64748b;
  font-size: 13px;
}
.hint {
  margin-top: 12px;
  color: #94a3b8;
  font-size: 14px;
}
.res-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.section-manage-toolbar {
  display: flex;
  justify-content: flex-end;
}
</style>
