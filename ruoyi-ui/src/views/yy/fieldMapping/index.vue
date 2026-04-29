<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="选择平台" prop="platformId">
        <el-select v-model="queryParams.platformId" placeholder="请选择平台" @change="handlePlatformChange" filterable>
          <el-option
            v-for="item in platformList"
            :key="item.pId"
            :label="item.platformName"
            :value="item.pId"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">加载映射</el-button>
      </el-form-item>
    </el-form>

    <el-card v-if="queryParams.platformId" shadow="never" style="margin-top: 10px;">
      <div slot="header" style="display: flex; justify-content: space-between; align-items: center;">
        <div style="display: flex; align-items: center; gap: 16px;">
          <span>字段映射配置 - {{ currentPlatformName }}</span>
          
          <!-- 配置进度 -->
          <el-tag :type="progressType" size="small">
            已配置 {{ mappedCount }}/{{ totalCount }} 个标准字段
          </el-tag>
          
          <div style="display: flex; align-items: center; width: 340px;">
            <div
              style="background-color: #F5F7FA; border: 1px solid #DCDFE6; border-radius: 4px; padding: 0 15px; height: 32px; display: flex; align-items: center; margin-right: 8px; color: #909399; font-size: 13px; white-space: nowrap; box-sizing: border-box;">
              <span>数据入口</span>
              <el-tooltip placement="top" :open-delay="200">
                <i class="el-icon-question" style="margin-left: 4px; cursor: pointer;"></i>
                <div slot="content" style="line-height: 1.8; max-width: 380px;">
                  <div><b>数据入口路径</b>：指定商品数组在 JSON 中的位置</div>
                  <div style="margin-top: 4px;">不填则自动检测 data/items/list 等字段</div>
                  <div style="margin-top: 8px;"><b>示例：</b></div>
                  <div><code>{\"data\": [...]}</code> → 填 <code>data</code></div>
                  <div><code>{\"data\": {\"wholesales\": [...]}}</code> → 填 <code>data.wholesales</code></div>
                  <div><code>{\"result\": {\"list\": [...]}}</code> → 填 <code>result.list</code></div>
                  <div><code>[...]</code> 直接是数组 → 不填</div>
                </div>
              </el-tooltip>
            </div>
            <el-input v-model="entryPath" placeholder="如: data.wholesales" size="small" clearable style="flex: 1;">
            </el-input>
          </div>
        </div>
        <div>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">添加映射</el-button>
          <el-button type="success" icon="el-icon-check" @click="handleSaveAll" :loading="saveLoading">保存全部</el-button>
          <el-button type="warning" icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </div>
      </div>

      <el-table :data="mappingList" border style="width: 100%;" v-loading="loading">
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="标准字段" prop="standardField" width="220" align="center">
          <template slot-scope="scope">
            <el-select 
              v-model="scope.row.standardField" 
              placeholder="选择标准字段" 
              style="width: 100%;" 
              filterable
              @change="handleFieldChange(scope.row)"
            >
              <el-option-group v-for="(fields, group) in fieldGroups" :key="group" :label="group">
                <el-option
                  v-for="field in fields"
                  :key="field.value"
                  :label="field.label"
                  :value="field.value"
                  :disabled="isFieldUsed(field.value, scope.$index)"
                  :class="{ 'field-used': isFieldUsed(field.value, scope.$index) && field.value !== scope.row.standardField }"
                >
                  <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span>
                      <span v-if="isFieldUsed(field.value, scope.$index) && field.value !== scope.row.standardField" style="color: #E6A23C;">
                        ⚠️ {{ field.label }}
                      </span>
                      <span v-else>{{ field.label }}</span>
                    </span>
                    <span style="color: #8492a6; font-size: 12px;">
                      {{ field.value }}
                      <span v-if="isFieldUsed(field.value, scope.$index) && field.value !== scope.row.standardField" style="color: #E6A23C; margin-left: 4px;">
                        [已配置]
                      </span>
                    </span>
                  </div>
                </el-option>
              </el-option-group>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="platformField" min-width="170">
          <template slot="header">
            <span>平台字段名</span>
            <el-tooltip placement="top" :open-delay="200">
              <i class="el-icon-question" style="margin-left: 4px; color: #909399; cursor: pointer;"></i>
              <div slot="content" style="line-height: 1.8; max-width: 420px;">
                <div><b>支持一对多映射：</b></div>
                <div>用逗号分隔多个字段，按顺序取第一个有值的</div>
                <div style="margin-top: 4px;"><b>示例：</b></div>
                <div><code>brand,brandName,brand_name</code></div>
                <div style="margin-top: 8px;"><b>支持的路径语法：</b></div>
                <div>drugname — 直接取顶层字段</div>
                <div>joinCarMap.manufacturer — 点号取嵌套字段</div>
                <div>stepPrice[0] — 取数组第1个元素</div>
                <div>stepPrice[-1] — 取数组最后1个元素</div>
                <div>joinCarMap.discountTags[0].tagTitle — 数组元素的子字段</div>
                <div>joinCarMap.discountTags[*].word — 所有元素的字段，逗号拼接</div>
              </div>
            </el-tooltip>
          </template>
          <template slot-scope="scope">
            <el-input v-model="scope.row.platformField" placeholder="如: brand,brandName 或 joinCarMap.manufacturer" />
          </template>
        </el-table-column>
        <el-table-column label="数据类型" prop="fieldType" width="130" align="center">
          <template slot-scope="scope">
            <el-select v-model="scope.row.fieldType" style="width: 100%;">
              <el-option label="字符串" value="string" />
              <el-option label="数字" value="number" />
              <el-option label="金额" value="decimal" />
              <el-option label="日期" value="date" />
              <el-option label="布尔" value="boolean" />
              <el-option label="JSON" value="json" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="必填" prop="isRequired" width="80" align="center">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.isRequired" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="130" align="center">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" active-text="启" inactive-text="停" />
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sortOrder" width="130" align="center">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.sortOrder" :min="0" :max="999" controls-position="right" size="small" style="width: 100px;" />
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" width="160">
          <template slot-scope="scope">
            <el-input v-model="scope.row.remark" placeholder="备注" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="scope">
            <el-button type="text" style="color: #F56C6C;" icon="el-icon-delete" @click="handleDelete(scope.$index)"></el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 未配置字段提示 -->
      <div v-if="unmappedFields.length > 0" style="margin-top: 12px; padding: 12px; background: #FDF6EC; border-radius: 4px; border: 1px solid #E6A23C;">
        <div style="color: #E6A23C; font-weight: bold; margin-bottom: 8px;">
          <i class="el-icon-warning"></i> 以下 {{ unmappedFields.length }} 个标准字段尚未配置映射：
        </div>
        <el-tag 
          v-for="field in unmappedFields" 
          :key="field.value" 
          size="small" 
          style="margin: 2px 4px;"
          type="info"
        >
          {{ field.label }} ({{ field.value }})
        </el-tag>
      </div>
    </el-card>

    <!-- 数据测试卡片 -->
    <el-card v-if="queryParams.platformId" shadow="never" style="margin-top: 10px;">
      <div slot="header" style="display: flex; justify-content: space-between; align-items: center;">
        <span>数据测试 - {{ currentPlatformName }}</span>
        <div>
          <el-radio-group v-model="testForm.mode" size="small" style="margin-right: 12px;">
            <el-radio-button label="raw">原始JSON</el-radio-button>
            <el-radio-button label="encrypt">加密数据</el-radio-button>
          </el-radio-group>
          <el-button type="primary" icon="el-icon-s-promotion" @click="handleTestDecrypt" :loading="testLoading">
            {{ testForm.mode === 'raw' ? '测试映射' : '解密并映射' }}
          </el-button>
        </div>
      </div>

      <el-row :gutter="20">
        <el-col :span="12">
          <div style="margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
            <span style="color: #606266; font-weight: bold;">
              {{ testForm.mode === 'raw' ? '原始 JSON 数据' : '加密数据' }}
            </span>
            <span v-if="parsedInputJson" style="color: #909399; font-size: 12px;">
              已解析 {{ parsedNodeCount }} 个节点
            </span>
          </div>
          <el-input
            v-model="testForm.inputData"
            type="textarea"
            :rows="testForm.mode === 'raw' && parsedInputJson ? 5 : 10"
            :placeholder="testForm.mode === 'raw' ? '粘贴平台返回的原始 JSON 数据，下方自动显示结构视图' : '粘贴平台加密后的数据'"
          />
          <div v-if="testForm.mode === 'raw' && parsedInputJson"
               style="background: #f8f9fa; padding: 12px; border-radius: 0 0 4px 4px; border: 1px solid #DCDFE6; border-top: none; max-height: 350px; overflow: auto; font-size: 13px; font-family: Consolas, Monaco, monospace;">
            <div style="margin-bottom: 6px; color: #909399; font-size: 12px; font-family: sans-serif;">
              结构视图（点击节点展开/折叠，查看数据入口路径）
            </div>
            <vue-json-pretty :data="parsedInputJson" :deep="3" />
          </div>
          <div style="margin-top: 8px; display: flex; align-items: center; gap: 12px;">
            <el-select v-model="testForm.selectedApiId" placeholder="选择接口（自动填入口路径）" style="width: 260px;" clearable @change="handleApiChange">
              <el-option
                v-for="api in platformApiList"
                :key="api.apiId"
                :label="api.apiName + (api.apiCode ? ' (' + api.apiCode + ')' : '')"
                :value="api.apiId"
              />
            </el-select>
            <el-select v-if="testForm.mode === 'encrypt'" v-model="testForm.dataEncryptType" placeholder="加密方式" style="width: 180px;">
              <el-option label="未加密" :value="0" />
              <el-option label="AES-128-ECB" :value="1" />
              <el-option label="AES-128-CBC" :value="2" />
              <el-option label="AES-128-GCM" :value="3" />
              <el-option label="DES-ECB" :value="4" />
              <el-option label="RSA-PRIVATE-KEY" :value="5" />
              <el-option label="RSA-PUBLIC-KEY" :value="6" />
              <el-option label="SM4-ECB" :value="7" />
              <el-option label="SM4-CBC" :value="8" />
              <el-option label="BASE64" :value="9" />
            </el-select>
          </div>
        </el-col>
        <el-col :span="12">
          <div style="margin-bottom: 8px; color: #606266; font-weight: bold;">
            映射结果
            <span v-if="testResult.mappedCount > 0" style="color: #67C23A; font-size: 12px;">
              （{{ testResult.mappedCount }} 条）
            </span>
            <span v-if="testResult.hitFields > 0" style="color: #909399; font-size: 12px; margin-left: 8px;">
              字段命中 {{ testResult.hitFields }}/{{ testResult.configuredFields }}
              <span v-if="testResult.missFields > 0" style="color: #E6A23C;">
                （{{ testResult.missFields }} 个未命中）
              </span>
            </span>
          </div>
          <div v-if="testResult.products.length > 0">
            <el-table :data="testResult.products" border size="small" max-height="300">
              <el-table-column 
                v-for="col in testResultColumns" 
                :key="col.prop"
                :label="col.label" 
                :prop="col.prop" 
                :width="col.width" 
                :min-width="col.minWidth"
                :align="col.align"
                show-overflow-tooltip 
              />
            </el-table>
          </div>
          <div v-else-if="testResult.raw" style="color: #909399; padding: 20px; text-align: center;">
            解密成功但未匹配到映射规则，请检查字段映射配置
          </div>
          <div v-else style="color: #C0C4CC; padding: 20px; text-align: center;">
            粘贴加密数据后点击「解密并映射」
          </div>
        </el-col>
      </el-row>

      <!-- 原始解密数据（JSON 树浏览） -->
      <el-collapse v-if="testResult.raw" style="margin-top: 12px;">
        <el-collapse-item title="查看原始解密数据">
          <div style="background: #f8f9fa; padding: 12px; border-radius: 4px; max-height: 500px; overflow: auto; font-size: 13px; font-family: Consolas, Monaco, monospace;">
            <vue-json-pretty :data="testResult.raw" :deep="3" />
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <el-card v-if="!queryParams.platformId" shadow="never" style="margin-top: 10px;">
      <div slot="header">
        <span>使用说明</span>
      </div>
      <div style="color: #606266; line-height: 1.8;">
        <p>1. 先在上方选择要配置的平台</p>
        <p>2. 点击"加载映射"查看该平台的字段映射配置</p>
        <p>3. 左侧"标准字段"是系统定义的标准字段名，右侧"平台字段名"填写该平台实际返回的字段名</p>
        <p>4. 例如：标准字段为 <code>price_current</code>，药师帮的平台字段填 <code>y_price</code>，药九九填 <code>md_price</code></p>
        <p>5. <b>已配置的字段会显示 ⚠️ 标记，请勿重复配置</b></p>
        <p>6. <b>支持嵌套和数组路径语法：</b></p>
        <div style="padding-left: 20px;">
          <p><code>a.b.c</code> — 点号取嵌套对象字段，如 <code>joinCarMap.manufacturer</code></p>
          <p><code>arr[0]</code> — 取数组指定下标元素，支持负数 <code>arr[-1]</code> 取最后一个</p>
          <p><code>a.b[0].c</code> — 组合使用，如 <code>joinCarMap.discountTags[0].tagTitle</code></p>
          <p><code>a.b[*].c</code> — 取数组所有元素的 c 字段，返回逗号分隔字符串</p>
        </div>
        <p>7. 配置完成后点击"保存全部"生效</p>
      </div>
    </el-card>
  </div>
</template>

<script>
import { listFieldMapping, getFieldMappingByPlatform, batchSaveFieldMapping, delFieldMapping } from '@/api/yy/fieldMapping'
import { listPlatform } from '@/api/yy/platform'
import { listPlatformApi } from '@/api/yy/platformApi'
import { decryptAndMap } from '@/api/yy/platformData'

import VueJsonPretty from 'vue-json-pretty'
import 'vue-json-pretty/lib/styles.css'

import { FIELD_GROUPS, FIELD_OPTIONS, TEST_RESULT_COLUMNS, STANDARD_FIELDS } from './standardFields'

export default {
  name: 'FieldMapping',
  components: { VueJsonPretty },
  data() {
    return {
      loading: false,
      saveLoading: false,
      testLoading: false,
      platformList: [],
      mappingList: [],
      originalList: [],
      currentPlatformName: '',
      entryPath: '',
      platformApiList: [],
      // 配置驱动
      fieldGroups: FIELD_GROUPS,
      standardFields: FIELD_OPTIONS,
      allStandardFields: STANDARD_FIELDS,
      testResultColumns: TEST_RESULT_COLUMNS,
      queryParams: {
        platformId: null
      },
      testForm: {
        mode: 'raw',
        inputData: '',
        selectedApiId: undefined,
        dataEncryptType: 9
      },
      testResult: {
        raw: null,
        rawFormatted: '',
        products: [],
        mappedCount: 0,
        configuredFields: 0,
        hitFields: 0,
        missFields: 0,
        message: ''
      }
    }
  },
  computed: {
    parsedInputJson() {
      if (this.testForm.mode !== 'raw' || !this.testForm.inputData) return null
      try {
        return JSON.parse(this.testForm.inputData)
      } catch (e) {
        return null
      }
    },
    parsedNodeCount() {
      const countNodes = (obj) => {
        if (obj === null || typeof obj !== 'object') return 1
        let count = 1
        const entries = Array.isArray(obj) ? obj : Object.values(obj)
        for (const v of entries) {
          count += countNodes(v)
        }
        return count
      }
      return this.parsedInputJson ? countNodes(this.parsedInputJson) : 0
    },
    // 已使用的标准字段集合
    usedFields() {
      const used = new Set()
      this.mappingList.forEach(item => {
        if (item.standardField) {
          used.add(item.standardField)
        }
      })
      return used
    },
    // 已配置字段数量
    mappedCount() {
      return this.usedFields.size
    },
    // 总标准字段数量
    totalCount() {
      return this.allStandardFields.length
    },
    // 进度类型
    progressType() {
      const ratio = this.mappedCount / this.totalCount
      if (ratio >= 0.8) return 'success'
      if (ratio >= 0.5) return 'warning'
      return 'danger'
    },
    // 未配置的字段列表
    unmappedFields() {
      return this.allStandardFields.filter(f => !this.usedFields.has(f.value))
    }
  },
  created() {
    this.loadPlatforms()
  },
  methods: {
    loadPlatforms() {
      listPlatform().then(res => {
        this.platformList = res.rows || res.data || res || []
      })
    },
    handlePlatformChange(val) {
      const p = this.platformList.find(item => item.pId === val)
      this.currentPlatformName = p ? p.platformName : ''
      this.handleQuery()
      this.loadApis(val)
    },
    loadApis(platformId) {
      this.platformApiList = []
      this.testForm.selectedApiId = undefined
      if (!platformId) return
      listPlatformApi({ platformId: platformId }).then(res => {
        this.platformApiList = res.rows || res.data?.rows || res.data || res || []
      }).catch(() => {})
    },
    handleApiChange(apiId) {
      if (!apiId) return
      const api = this.platformApiList.find(a => a.apiId === apiId)
      if (api && api.entryPath) {
        this.entryPath = api.entryPath
        this.$message.info('已自动填入入口路径: ' + api.entryPath)
      }
    },
    // 判断字段是否已被使用（排除当前行）
    isFieldUsed(fieldValue, currentIndex) {
      return this.mappingList.some((item, index) => 
        index !== currentIndex && item.standardField === fieldValue
      )
    },
    // 字段变更时的处理
    handleFieldChange(row) {
      // 可以在这里添加自动设置数据类型的逻辑
      const fieldConfig = this.allStandardFields.find(f => f.value === row.standardField)
      if (fieldConfig && fieldConfig.type) {
        row.fieldType = fieldConfig.type
      }
    },
    handleQuery() {
      if (!this.queryParams.platformId) return
      this.loading = true
      getFieldMappingByPlatform(this.queryParams.platformId).then(res => {
        const list = res.list || res.data?.list || []
        this.mappingList = list.map(item => ({
          ...item,
          isRequired: item.isRequired === 1 ? 1 : 0,
          status: item.status !== 0 ? 1 : 0
        }))
        this.originalList = JSON.parse(JSON.stringify(this.mappingList))
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleAdd() {
      // 找到第一个未使用的字段
      const unusedField = this.allStandardFields.find(f => !this.usedFields.has(f.value))
      
      this.mappingList.unshift({
        id: undefined,
        platformId: this.queryParams.platformId,
        standardField: unusedField ? unusedField.value : '',
        platformField: '',
        fieldType: unusedField ? unusedField.type : 'string',
        isRequired: 0,
        sortOrder: 0,
        status: 1,
        remark: '',
        entryPath: ''
      })
      this.mappingList.forEach((item, i) => {
        item.sortOrder = i * 10
      })
    },
    handleDelete(index) {
      this.$confirm('确定删除这条映射吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.mappingList.splice(index, 1)
      }).catch(() => {})
    },
    handleSaveAll() {
      if (!this.queryParams.platformId) return

      const filledRows = this.mappingList.filter(row =>
        row.standardField && row.platformField
      )

      if (filledRows.length === 0) {
        this.$message.warning('请至少配置一条有效的映射关系')
        return
      }

      const fieldSet = new Set()
      for (const row of filledRows) {
        if (fieldSet.has(row.standardField)) {
          const label = this.standardFields.find(f => f.value === row.standardField)?.label || row.standardField
          this.$message.error('标准字段「' + label + '」重复配置，同一平台每个标准字段只能映射一次')
          return
        }
        fieldSet.add(row.standardField)
      }

      const saveData = filledRows.map(item => ({
        id: item.id || undefined,
        platformId: this.queryParams.platformId,
        standardField: item.standardField,
        platformField: item.platformField,
        fieldType: item.fieldType || 'string',
        isRequired: item.isRequired ? 1 : 0,
        sortOrder: item.sortOrder || 0,
        status: item.status !== 0 ? 1 : 0,
        remark: item.remark || '',
        entryPath: this.entryPath || ''
      }))

      this.saveLoading = true
      batchSaveFieldMapping(this.queryParams.platformId, saveData)
        .then(res => {
          this.$message.success('保存成功')
          this.handleQuery()
        })
        .finally(() => {
          this.saveLoading = false
        })
    },
    handleReset() {
      this.mappingList = JSON.parse(JSON.stringify(this.originalList))
      this.$message.info('已重置')
    },
    handleTestDecrypt() {
      if (!this.queryParams.platformId) {
        this.$message.warning('请先选择平台')
        return
      }
      if (!this.testForm.inputData) {
        this.$message.warning('请粘贴数据')
        return
      }

      const platform = this.platformList.find(p => p.pId === this.queryParams.platformId)
      if (!platform) {
        this.$message.error('平台信息获取失败')
        return
      }

      this.testLoading = true
      this.testResult = { raw: null, rawFormatted: '', products: [], mappedCount: 0, configuredFields: 0, hitFields: 0, missFields: 0, message: '' }

      const params = { platformCode: platform.platformCode, dataSource: 1 }
      if (this.entryPath) {
        params.entryPath = this.entryPath
      }
      if (this.testForm.mode === 'raw') {
        params.rawData = this.testForm.inputData
      } else {
        params.encryptData = this.testForm.inputData
        params.dataEncryptType = this.testForm.dataEncryptType
      }

      decryptAndMap(params).then(res => {
        const data = res.data || res
        this.testResult.raw = data.raw || null
        this.testResult.rawFormatted = typeof data.raw === 'object'
          ? JSON.stringify(data.raw, null, 2)
          : String(data.raw || '')
        this.testResult.products = data.products || []
        this.testResult.mappedCount = data.mappedCount || 0
        this.testResult.configuredFields = data.configuredFields || 0
        this.testResult.hitFields = data.hitFields || 0
        this.testResult.missFields = data.missFields || 0
        this.testResult.message = data.message || ''

        if (this.testResult.mappedCount > 0) {
          this.$message.success('映射成功，共 ' + this.testResult.mappedCount + ' 条标准商品')
        } else if (this.testResult.raw) {
          this.$message.warning(this.testResult.message || '数据已解析，但未映射到商品（检查映射规则或数据格式）')
        }
      }).catch(err => {
        this.$message.error('操作失败: ' + (err || '未知错误'))
      }).finally(() => {
        this.testLoading = false
      })
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}
code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  color: #e6a23c;
  font-size: 13px;
}
/* 已使用字段的样式 */
.field-used {
  background-color: #FEF6EC !important;
}
</style>
