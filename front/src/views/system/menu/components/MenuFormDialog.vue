<template>
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="50%" @closed="handleClosed">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">

      <!-- 菜单名称 -->
      <el-row>
        <el-col :span="24">
          <el-form-item label="菜单名称" prop="menuName">
            <el-input v-model="formData.menuName" placeholder="请输入菜单名称" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 父级菜单 -->
      <el-row>
        <el-col :span="24">
          <el-form-item label="父级菜单" prop="parentId">
            <el-tree-select
                v-model="formData.parentId"
                :data="menuTree"
                node-key="menuId"
                default-expand-all
                props.label="menuName"
                props.value="menuId"
                props.children="children"        style="width: 100%"
                placeholder="请选择父级菜单"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="类型" prop="path">
            <el-input v-model="formData.type" placeholder="1" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="排序" prop="path">
            <el-input v-model="formData.orderNum" placeholder="0" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 路由地址 -->
      <el-row>
        <el-col :span="24">
          <el-form-item label="路由地址" prop="path">
            <el-input v-model="formData.path" placeholder="例如：/system/user" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 组件路径 -->
      <el-row>
        <el-col :span="24">
          <el-form-item label="组件路径" prop="component">
            <el-input v-model="formData.component" placeholder="例如：system/user/index.vue" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 权限标识 -->
      <el-row>
        <el-col :span="24">
          <el-form-item label="权限标识" prop="perms">
            <el-input v-model="formData.perms" placeholder="例如：system:user:list" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 显隐状态 -->
      <el-row>
        <el-col :span="12">
          <el-form-item label="是否可见" prop="visible">
            <el-switch v-model="formData.visible" />
          </el-form-item>
        </el-col>

        <!-- 菜单状态 -->
        <el-col :span="12">
          <el-form-item label="启用状态" prop="enabled">
            <el-switch v-model="formData.enabled" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 图标 -->
      <el-row>
        <el-col :span="24">
          <el-form-item label="图标" prop="icon">
            <el-input v-model="formData.icon" placeholder="图标类名或URL" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 备注 -->
      <el-row>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" :rows="3" />
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
import {ref, reactive, onMounted} from 'vue'
import { ElMessage } from 'element-plus'
import type { Menu } from '@/types/systemTypes'
import { addOrUpdateMenu,queryMenu } from '@api/system/menu-api.ts'

const dialogVisible = ref(false)
const formRef = ref()
const isEdit = ref(false)

// 表单数据
const formData = reactive({
  menuId: null as number | null,
  parentId: null as number | null,
  orderNum: null as number | null,
  menuName: '',
  type: 1,
  path: '',
  component: '',
  visible: true,
  enabled: true,
  perms: '',
  icon: '',
  remark: ''
})

// 校验规则
const formRules = reactive({
  menuName: [
    { required: true, message: '菜单名称不能为空', trigger: 'blur' }
  ],
  path: [
    { required: true, message: '路由地址不能为空', trigger: 'blur' }
  ],
  component: [
    { required: true, message: '组件路径不能为空', trigger: 'blur' }
  ],
  perms: [
    { required: false, message: '权限标识不能为空', trigger: 'blur' }
  ]
})

// 提交事件
const emit = defineEmits<{
  (e: 'success'): void
}>()

// 打开方法
const open = (menu?: any) => {
  dialogVisible.value = true
  if (menu) {
    isEdit.value = true
    Object.assign(formData, menu)
  } else {
    isEdit.value = false
    // 手动重置 + resetFields 双保险
    Object.keys(formData).forEach(key => {
      formData[key] = null
    })
  }
}

// 提交表单
const submitForm = async () => {
  try {
    await formRef.value.validate()
    let res
    if (isEdit.value) {
      res = await addOrUpdateMenu(formData)
    } else {
      res = await addOrUpdateMenu(formData)
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

// 菜单树数据
const menuTree = ref<Menu[]>([])

// 加载菜单树
const loadMenuTree = async () => {
  try {
    const res = await queryMenu({})
    if (res.code === 200) {
      menuTree.value = res.data || []
    } else {
      ElMessage.error('菜单树加载失败')
    }
  } catch (error) {
    ElMessage.error('请求菜单树出错')
  }
}

onMounted(() => {
  loadMenuTree()
})

// 关闭清空
const handleClosed = () => {
  formRef.value.resetFields()
}

defineExpose({ open })
</script>
