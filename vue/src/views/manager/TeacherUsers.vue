<template>
  <div class="teacher-users">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>学生账号管理</span>
          <el-button type="primary" @click="fetchStudents">刷新</el-button>
        </div>
      </template>

      <el-form :model="createForm" inline>
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" placeholder="学生用户名" />
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="createForm.password" placeholder="初始密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="createStudent">创建学生</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="students" style="width: 100%; margin-top: 12px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="role" label="角色" width="120" />
        <el-table-column label="重置密码" width="280">
          <template #default="scope">
            <div class="password-row">
              <el-input
                v-model="passwordMap[scope.row.id]"
                placeholder="输入新密码"
                show-password
              />
              <el-button type="warning" @click="resetPassword(scope.row.id)">更新</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="删除" width="120">
          <template #default="scope">
            <el-button type="danger" plain @click="deleteStudent(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import request from "@/utils/request";
import { ElMessage } from "element-plus";

const students = ref([]);
const passwordMap = reactive({});
const createForm = reactive({
  username: "",
  password: "",
});

const fetchStudents = async () => {
  try {
    const response = await request.get("/xwd/users/students");
    if (response?.status === "success") {
      students.value = response.data || [];
    } else {
      ElMessage.error(response?.message || "获取学生列表失败");
    }
  } catch (error) {
    ElMessage.error("获取学生列表失败");
  }
};

const createStudent = async () => {
  if (!createForm.username || !createForm.password) {
    ElMessage.warning("请输入用户名和初始密码");
    return;
  }
  try {
    const response = await request.post("/xwd/users/students", createForm);
    if (response?.status === "success") {
      ElMessage.success(response?.message || "创建成功");
      createForm.username = "";
      createForm.password = "";
      await fetchStudents();
    } else {
      ElMessage.error(response?.message || "创建失败");
    }
  } catch (error) {
    ElMessage.error("创建学生失败");
  }
};

const resetPassword = async (studentId) => {
  const password = passwordMap[studentId];
  if (!password) {
    ElMessage.warning("请输入新密码");
    return;
  }
  try {
    const response = await request.put(`/xwd/users/students/${studentId}/password`, { password });
    if (response?.status === "success") {
      ElMessage.success(response?.message || "密码更新成功");
      passwordMap[studentId] = "";
    } else {
      ElMessage.error(response?.message || "更新失败");
    }
  } catch (error) {
    ElMessage.error("更新密码失败");
  }
};

const deleteStudent = async (studentId) => {
  try {
    const response = await request.delete(`/xwd/users/students/${studentId}`);
    if (response?.status === "success") {
      ElMessage.success(response?.message || "删除成功");
      await fetchStudents();
    } else {
      ElMessage.error(response?.message || "删除失败");
    }
  } catch (error) {
    ElMessage.error("删除失败");
  }
};

onMounted(() => {
  fetchStudents();
});
</script>

<style scoped>
.teacher-users {
  background: #fff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.password-row {
  display: flex;
  gap: 8px;
}
</style>
