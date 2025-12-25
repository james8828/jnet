<template>
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="50%" @closed="handleClosed">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">

      <!-- 用户名 -->
      <el-row>
        <el-col :span="12">
          <el-form-item label="用户名" prop="userName">
            <el-input v-model="formData.userName" />
          </el-form-item>
        </el-col>

        <!-- 昵称 -->
        <el-col :span="12">
          <el-form-item label="昵称" prop="nickName">
            <el-input v-model="formData.nickName" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 密码 -->
      <el-row>
        <el-col :span="12">
          <el-form-item label="密码" prop="password">
            <el-input v-model="formData.password" type="password" show-password />
          </el-form-item>
        </el-col>

        <!-- 启用状态 -->
        <el-col :span="12">
          <el-form-item label="启用状态" prop="enabled">
            <el-switch v-model="formData.enabled" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 性别 -->
      <el-row>
        <el-col :span="12">
          <el-form-item label="性别" prop="sex">
            <el-select v-model="formData.sex" placeholder="请选择性别" style="width: 100%">
              <el-option label="男" :value="0" />
              <el-option label="女" :value="1" />
            </el-select>
          </el-form-item>
        </el-col>

        <!-- 用户类型 -->
        <el-col :span="12">
          <el-form-item label="用户类型" prop="type">
            <el-select v-model="formData.type" placeholder="请选择类型" style="width: 100%">
              <el-option label="管理员" value="admin" />
              <el-option label="普通用户" value="user" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 手机号 -->
      <el-row>
        <el-col :span="12">
          <el-form-item label="手机号" prop="mobile">
            <el-input v-model="formData.mobile" />
          </el-form-item>
        </el-col>

        <!-- 公司 -->
        <el-col :span="12">
          <el-form-item label="所属公司" prop="company">
            <el-input v-model="formData.company" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 头像 -->
      <el-row>
        <el-col :span="12">
          <el-form-item label="头像地址" prop="headImgUrl">
            <el-input v-model="formData.headImgUrl" />
          </el-form-item>
        </el-col>

        <!-- OpenID -->
        <el-col :span="12">
          <el-form-item label="OpenID" prop="openId">
            <el-input v-model="formData.openId" />
          </el-form-item>
        </el-col>
      </el-row>

    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { addOrUpdateUser } from '@api/system/user-api.ts'

const dialogVisible = ref(false)
const formRef = ref()
const isEdit = ref(false)

// 表单数据
const formData = reactive({
  userId: null as number | null,
  userName: '',
  password: '',
  nickName: '',
  headImgUrl: '',
  mobile: '',
  sex: 0,
  enabled: true,
  type: 'user',
  company: '',
  openId: ''
})

// 校验规则
const formRules = reactive({
  userName: [
    { required: true, message: '用户名不能为空', trigger: 'blur' }
  ],
  nickName: [
    { required: true, message: '昵称不能为空', trigger: 'blur' }
  ],
  password: [
    { required: !isEdit.value, message: '密码不能为空', trigger: 'blur' }
  ]
})

// 提交事件
const emit = defineEmits<{
  (e: 'success'): void
}>()

// 打开弹窗方法
const open = (user?: any) => {
  dialogVisible.value = true
  if (user) {
    isEdit.value = true
    Object.assign(formData, user)
    // 编辑时密码不强制校验
    formRules.password = []
  } else {
    isEdit.value = false
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = async () => {
  try {
    await formRef.value.validate()
    let res
    if (isEdit.value) {
      res = await addOrUpdateUser(formData)
    } else {
      res = await addOrUpdateUser(formData)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      emit('success')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error('表单校验失败或请求出错')
  }
}

// 关闭清空
const handleClosed = () => {
  formRef.value.resetFields()
}

defineExpose({ open })
</script>
