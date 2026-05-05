<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="模型标识" prop="modelCode">
        <el-input v-model="queryParams.modelCode" placeholder="请输入模型标识" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="模型名称" prop="modelName">
        <el-input v-model="queryParams.modelName" placeholder="请输入模型名称" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="供应商" prop="provider">
        <el-select v-model="queryParams.provider" placeholder="请选择供应商" clearable>
          <el-option label="DeepSeek" value="deepseek"/>
          <el-option label="OpenAI" value="openai"/>
          <el-option label="DashScope" value="dashscope"/>
          <el-option label="自定义" value="custom"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['yy:admin:ai:quota']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>

    <el-table v-loading="loading" :data="modelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="排序" prop="sortOrder" width="60" align="center"/>
      <el-table-column label="模型标识" prop="modelCode" min-width="140" show-overflow-tooltip/>
      <el-table-column label="模型名称" prop="modelName" min-width="120" show-overflow-tooltip/>
      <el-table-column label="供应商" prop="provider" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="providerTag(scope.row.provider)" size="small">{{ scope.row.provider }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="API地址" prop="endpoint" min-width="220" show-overflow-tooltip/>
      <el-table-column label="能力" prop="capabilities" width="140" show-overflow-tooltip>
        <template slot-scope="scope">
          <el-tag v-for="c in (scope.row.capabilities || '').split(',')" :key="c" size="mini" style="margin-right:2px">{{ c }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="多模态" prop="isMultimodal" width="70" align="center">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isMultimodal" disabled :active-value="1" :inactive-value="0"/>
        </template>
      </el-table-column>
      <el-table-column label="最大Token" prop="maxTokens" width="90" align="center"/>
      <el-table-column label="启用" prop="isEnabled" width="60" align="center">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isEnabled" disabled :active-value="1" :inactive-value="0"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['yy:admin:ai:quota']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['yy:admin:ai:quota']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模型标识" prop="modelCode">
          <el-input v-model="form.modelCode" placeholder="如 deepseek-chat, gpt-4o"/>
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="如 DeepSeek Chat, GPT-4o"/>
        </el-form-item>
        <el-form-item label="供应商" prop="provider">
          <el-select v-model="form.provider" placeholder="请选择">
            <el-option label="DeepSeek" value="deepseek"/>
            <el-option label="OpenAI" value="openai"/>
            <el-option label="DashScope" value="dashscope"/>
            <el-option label="自定义" value="custom"/>
          </el-select>
        </el-form-item>
        <el-form-item label="API地址" prop="endpoint">
          <el-input v-model="form.endpoint" placeholder="https://api.deepseek.com/v1/chat/completions"/>
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..."/>
        </el-form-item>
        <el-form-item label="能力标签" prop="capabilities">
          <el-input v-model="form.capabilities" placeholder="text,image,vision (逗号分隔)"/>
        </el-form-item>
        <el-form-item label="多模态" prop="isMultimodal">
          <el-switch v-model="form.isMultimodal" :active-value="1" :inactive-value="0"/>
        </el-form-item>
        <el-form-item label="最大Token" prop="maxTokens">
          <el-input-number v-model="form.maxTokens" :min="100" :max="128000" :step="100"/>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999"/>
        </el-form-item>
        <el-form-item label="启用" prop="isEnabled">
          <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAiModel, getAiModel, addAiModel, updateAiModel, delAiModel } from '@/api/yy/aiModel'

export default {
  name: 'YyAiModelConfig',
  data() {
    return {
      loading: true,
      showSearch: true,
      ids: [],
      modelList: [],
      total: 0,
      title: '',
      open: false,
      queryParams: { pageNum: 1, pageSize: 10, modelCode: null, modelName: null, provider: null },
      form: {},
      rules: {
        modelCode: [{ required: true, message: '模型标识不能为空', trigger: 'blur' }],
        modelName: [{ required: true, message: '模型名称不能为空', trigger: 'blur' }],
        endpoint: [{ required: true, message: 'API地址不能为空', trigger: 'blur' }],
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listAiModel(this.queryParams).then(res => {
        this.modelList = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    providerTag(provider) {
      const map = { deepseek: '', openai: 'success', dashscope: 'warning', custom: 'info' }
      return map[provider] || 'info'
    },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { isMultimodal: 0, maxTokens: 4096, sortOrder: 0, isEnabled: 1 } },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(i => i.id) },
    handleAdd() { this.reset(); this.open = true; this.title = '新增AI模型配置' },
    handleUpdate(row) {
      this.reset()
      getAiModel(row.id).then(res => { this.form = res.data; this.open = true; this.title = '修改AI模型配置' })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateAiModel(this.form).then(() => { this.$modal.msgSuccess('修改成功'); this.open = false; this.getList() })
          } else {
            addAiModel(this.form).then(() => { this.$modal.msgSuccess('新增成功'); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除模型配置「' + row.modelName + '」？').then(() => {
        return delAiModel(row.id)
      }).then(() => { this.getList(); this.$modal.msgSuccess('删除成功') })
    }
  }
}
</script>
