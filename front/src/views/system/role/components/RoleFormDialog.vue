<template>
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="40%">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
      <el-row>
        <el-col :span="24">
          <el-form-item label="角色名称" prop="roleName">
            <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row>
        <el-col :span="24">
          <el-form-item label="角色标识" prop="roleKey">
            <el-input v-model="formData.roleKey" placeholder="请输入角色标识" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row>
        <el-col :span="12">
          <el-form-item label="启用状态" prop="enabled">
            <el-switch v-model="formData.enabled" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 菜单权限 -->
      <el-row>
        <el-col :span="24">
          <el-form-item label="菜单权限">
            <el-tree-select
                v-model="formData.menus"
                :data="menuTree"
                node-key="menuId"
                default-expand-all
                check-strictly
                show-checkbox
                multiple
                :props="{ label: 'menuName', value: 'menuId', children: 'children' }"
                style="width: 100%"
                placeholder="请选择菜单权限"
            />
          </el-form-item>
        </el-col>
      </el-row>

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
import {Menu, Role} from '@/types/systemTypes'
import { addOrUpdateRole } from '@api/system/role-api.ts'
import { queryMenu } from '@api/system/menu-api.ts' // 引入获取菜单树的方法

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const emit = defineEmits<{
  (e: 'success'): void
}>()

// 表单数据
const formData = reactive({
  roleId: null as number | null,
  roleName: '',
  roleKey: '',
  enabled: true,
  remark: '',
  menus: []
})

// 表单规则
const formRules = reactive({
  roleName: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
  roleKey: [{ required: true, message: '角色标识不能为空', trigger: 'blur' }]
})

// 提交表单
const submitForm = async () => {
  try {
    await formRef.value.validate()
    const res = await addOrUpdateRole(formData)
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

// 打开方法
const open = async (role?: Partial<Role>) => {
  dialogVisible.value = true
  isEdit.value = !!role?.roleId
  if (role) {
    Object.assign(formData, role)
    //遍历role.menus数组，转换为menuId数组
    selectedMenuIds.value = role.menus?.map((menu) => menu.menuId)
    formData.menus = selectedMenuIds
  } else {
    // 默认值
    formData.roleId = null
    formData.roleName = ''
    formData.roleKey = ''
    formData.enabled = true
    formData.remark = ''
    selectedMenuIds.value = []
  }
}


// 菜单树数据
const menuTree = ref<Menu[]>([])

// 选中的菜单 IDs
const selectedMenuIds = ref<number[]>([])
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

defineExpose({ open })
</script>
