<template>
  <div class="teacher-questions">
    <el-card class="page-shell">
      <template #header>
        <div class="card-header">
          <div>
            <h3 class="page-title">题目管理</h3>
            <p class="page-desc">批量导入、查看和删除题目</p>
          </div>
          <el-button type="primary" @click="fetchQuestions">刷新</el-button>
        </div>
      </template>

      <div class="import-row">
        <el-upload :show-file-list="false" :before-upload="beforeCsvUpload">
          <el-button type="success">批量导入题目（CSV）</el-button>
        </el-upload>
        <span class="import-tip">
          模板：content,image_url,options,correct_answer,difficulty,knowledge_point_ids
        </span>
      </div>

      <el-table :data="questions" style="width: 100%; margin-top: 12px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="content" label="题干" min-width="420" show-overflow-tooltip />
        <el-table-column prop="correctAnswer" label="答案" width="90" />
        <el-table-column prop="difficulty" label="难度" width="100" />
        <el-table-column prop="knowledgePointIds" label="知识点ID" width="160" />
        <el-table-column label="删除" width="120">
          <template #default="scope">
            <el-button type="danger" plain @click="deleteQuestion(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const questions = ref([]);

const fetchQuestions = async () => {
  try {
    const response = await request.get("/xwd/questions");
    if (response?.status === "success") {
      questions.value = response.data || [];
    } else {
      ElMessage.error(response?.message || "获取题目列表失败");
    }
  } catch (error) {
    ElMessage.error("获取题目列表失败");
  }
};

const deleteQuestion = async (questionId) => {
  try {
    const response = await request.delete(`/xwd/questions/${questionId}`);
    if (response?.status === "success") {
      ElMessage.success(response?.message || "删除成功");
      await fetchQuestions();
    } else {
      ElMessage.error(response?.message || "删除失败");
    }
  } catch (error) {
    ElMessage.error("删除题目失败");
  }
};

const beforeCsvUpload = async (file) => {
  if (!file.name.toLowerCase().endsWith(".csv")) {
    ElMessage.warning("请上传 CSV 文件");
    return false;
  }
  const formData = new FormData();
  formData.append("file", file);
  try {
    const response = await request.post("/xwd/questions/import", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    if (response?.status === "success") {
      ElMessage.success(`导入成功，共 ${response?.count ?? 0} 道题`);
      await fetchQuestions();
    } else {
      ElMessage.error(response?.message || "导入失败");
    }
  } catch (error) {
    ElMessage.error("导入失败");
  }
  return false;
};

onMounted(() => {
  fetchQuestions();
});
</script>

<style scoped>
.teacher-questions {
  background: transparent;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.import-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.import-tip {
  color: #909399;
  font-size: 12px;
}
</style>
