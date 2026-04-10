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
          <span>小节列表</span>
          <span class="small-text">{{ sections.length }} 个小节</span>
        </div>
      </template>

      <div v-if="sections.length === 0" class="placeholder small">请选择课程查看章节小节。</div>
      <div v-else class="section-list">
        <div v-for="item in sections" :key="item.id" class="section-item">
          <div class="left">
            <div class="title">{{ item.title }}</div>
            <div class="meta">{{ item.chapterTitle }} · {{ item.sectionType }} · {{ item.durationSeconds || 0 }}s</div>
          </div>
          <el-button size="small" type="primary" plain @click="startLearning(item)">开始学习</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const courses = ref([]);
const sections = ref([]);
const selectedCourseId = ref(null);

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

const selectCourse = async (courseId) => {
  selectedCourseId.value = courseId;
  try {
    const response = await request.get(`/xwd/student/learning/courses/${courseId}/sections`);
    if (response?.status === "success") {
      sections.value = response.data || [];
    } else {
      ElMessage.error(response?.message || "获取小节失败");
    }
  } catch (e) {
    ElMessage.error("获取小节失败");
  }
};

const startLearning = async (section) => {
  try {
    await request.post(`/xwd/student/learning/sections/${section.id}/start`, {
      deviceInfo: "web",
    });
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
    const first = resources[0];
    const url = first?.resource_url || first?.resourceUrl;
    if (!url) {
      ElMessage.warning("该资源缺少链接地址");
      return;
    }
    window.open(url, "_blank", "noopener,noreferrer");
  } catch (e) {
    ElMessage.error("打开学习资源失败");
  }
};

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
@media (max-width: 960px) {
  .course-grid {
    grid-template-columns: 1fr;
  }
}
</style>

