<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属平台" prop="platformId">
        <el-select v-model="queryParams.platformId" placeholder="请选择平台" clearable>
          <el-option v-for="p in platformOptions" :key="p.pId" :label="p.platformName" :value="p.pId" />
        </el-select>
      </el-form-item>
      <el-form-item label="API名称" prop="apiName">
        <el-input v-model="queryParams.apiName" placeholder="请输入API名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="API类型" prop="apiType">
        <el-select v-model="queryParams.apiType" placeholder="请选择" clearable>
          <el-option label="搜索" value="search" />
          <el-option label="详情" value="detail" />
          <el-option label="库存" value="stock" />
          <el-option label="价格" value="price" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['yy:platformApi:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['yy:platformApi:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['yy:platformApi:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="platformApiList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="所属平台" align="center" width="120">
        <template slot-scope="scope">{{ getPlatformName(scope.row.platformId) }}</template>
      </el-table-column>
      <el-table-column label="API名称" align="center" prop="apiName" width="120" />
      <el-table-column label="编码" align="center" prop="apiCode" width="100" />
      <el-table-column label="请求地址" align="center" prop="apiUrl" :show-overflow-tooltip="true" />
      <el-table-column label="方法" align="center" prop="apiMethod" width="80">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.apiMethod === 'GET' ? 'success' : scope.row.apiMethod === 'POST' ? '' : 'warning'">{{ scope.row.apiMethod }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="apiType" width="80">
        <template slot-scope="scope">
          <el-tag size="mini" :type="apiTypeTag(scope.row.apiType)">{{ apiTypeLabel(scope.row.apiType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isActive" :active-value="1" :inactive-value="0"
            @change="handleStatusChange(scope.row)" v-hasPermi="['yy:platformApi:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
      <el-table-column label="签名模式" align="center" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.usePageSign === 1 ? 'warning' : 'info'">
            {{ scope.row.usePageSign === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['yy:platformApi:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['yy:platformApi:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="750px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属平台" prop="platformId">
              <el-select v-model="form.platformId" placeholder="请选择平台" style="width:100%">
                <el-option v-for="p in platformOptions" :key="p.pId" :label="p.platformName" :value="p.pId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API类型" prop="apiType">
              <el-select v-model="form.apiType" placeholder="请选择" style="width:100%">
                <el-option label="搜索" value="search" />
                <el-option label="详情" value="detail" />
                <el-option label="库存" value="stock" />
                <el-option label="价格" value="price" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="API名称" prop="apiName">
              <el-input v-model="form.apiName" placeholder="如：药品搜索" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API编码" prop="apiCode">
              <el-input v-model="form.apiCode" placeholder="如：search" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="请求地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="如：https://dian.ysbang.cn/api/search/searchGoods" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="请求方法" prop="apiMethod">
              <el-select v-model="form.apiMethod" style="width:100%">
                <el-option label="POST" value="POST" />
                <el-option label="GET" value="GET" />
                <el-option label="PUT" value="PUT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认每页" prop="defaultPageSize">
              <el-input-number v-model="form.defaultPageSize" :min="1" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序权重" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="签名模式" prop="usePageSign">
              <el-select v-model="form.usePageSign" placeholder="请选择" style="width:100%">
                <el-option label="禁用" :value="0" />
                <el-option label="启用" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数据加密" prop="dataEncrypt">
              <el-select v-model="form.dataEncrypt" placeholder="请选择" style="width:100%" clearable>
                <el-option v-for="item in encryptTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分页字段名" prop="pageField">
              <el-input v-model="form.pageField" placeholder="如：pageIndex" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="页数字段名" prop="pageSizeField">
              <el-input v-model="form.pageSizeField" placeholder="如：pageSize" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="构建函数" prop="buildBody">
          <el-input v-model="form.buildBody" type="textarea" :rows="3" placeholder="(kw, page, size) => JSON.stringify({ searchKey: kw, pageIndex: page, pageSize: size })" />
        </el-form-item>
        <el-form-item label="标准化函数" prop="normalize">
          <el-input v-model="form.normalize" type="textarea" :rows="3" placeholder="(res) => { if (!res?.data?.list) return []; return res.data.list.map(item => ({ name: item.goods_name, price: parseFloat(item.sale_price) || 0 })); }" />
        </el-form-item>
        <el-form-item label="签名脚本" prop="signScript">
          <el-input v-model="form.signScript" type="textarea" :rows="5" placeholder="粘贴签名脚本代码（如 waf.js 内容）" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据入口路径" prop="entryPath">
              <el-tooltip slot="label" placement="top" :open-delay="200">
                <span>数据入口路径 <i class="el-icon-question"></i></span>
                <div slot="content" style="line-height: 1.8; max-width: 380px;">
                  <div>用于字段映射：指定商品数组在 JSON 中的位置</div>
                  <div style="margin-top: 4px;"><b>示例：</b></div>
                  <div><code>{"data": [...]}</code> → 填 <code>data</code></div>
                  <div><code>{"data": {"wholesales": [...]}}</code> → 填 <code>data.wholesales</code></div>
                </div>
              </el-tooltip>
              <el-input v-model="form.entryPath" placeholder="如：data.wholesales（用于字段映射）" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加密数据路径" prop="encryptDataPath">
              <el-tooltip slot="label" placement="top" :open-delay="200">
                <span>加密数据路径 <i class="el-icon-question"></i></span>
                <div slot="content" style="line-height: 1.8; max-width: 380px;">
                  <div>用于提取加密数据：指定加密字符串在 JSON 中的位置</div>
                  <div style="margin-top: 4px;"><b>示例（药师帮）：</b></div>
                  <div><code>{"data": {"o": "加密数据"}}</code> → 填 <code>data.o</code></div>
                  <div style="margin-top: 4px; color: orange;"><b>注意：</b>与「数据入口路径」不同！</div>
                </div>
              </el-tooltip>
              <el-input v-model="form.encryptDataPath" placeholder="如：data.o（用于提取加密数据）" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
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
import { listPlatformApi, getPlatformApi, delPlatformApi, addPlatformApi, updatePlatformApi } from "@/api/yy/platformApi"
import { listAllPlatform } from "@/api/yy/platform"

export default {
  name: "PlatformApi",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      platformApiList: [],
      platformOptions: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        platformId: null,
        apiName: null,
        apiType: null,
        isActive: null
      },
      form: {},
      encryptTypeOptions: [],
      rules: {
        platformId: [{ required: true, message: "请选择所属平台", trigger: "change" }],
        apiName: [{ required: true, message: "API名称不能为空", trigger: "blur" }],
        apiCode: [{ required: true, message: "API编码不能为空", trigger: "blur" }],
        apiUrl: [{ required: true, message: "请求地址不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
    this.loadPlatforms()
    this.loadEncryptTypes()
  },
  methods: {
    getList() {
      this.loading = true
      listPlatformApi(this.queryParams).then(response => {
        this.platformApiList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    loadPlatforms() {
      listAllPlatform().then(response => {
        this.platformOptions = response.data || []
      })
    },
    loadEncryptTypes() {
      this.getDicts('yy_platform_encrypt_type').then(response => {
        this.encryptTypeOptions = (response.data || []).map(item => ({
          label: item.dictLabel,
          value: parseInt(item.dictValue)
        }))
      })
    },
    getPlatformName(platformId) {
      const p = this.platformOptions.find(o => o.pId === platformId)
      return p ? p.platformName : ('ID:' + platformId)
    },
    apiTypeLabel(type) {
      return { search: '搜索', detail: '详情', stock: '库存', price: '价格' }[type] || type
    },
    apiTypeTag(type) {
      return { search: '', detail: 'success', stock: 'warning', price: 'danger' }[type] || ''
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        apiId: null, platformId: null, apiName: null, apiCode: null,
        apiUrl: null, apiMethod: 'POST', contentType: 'application/json',
        dataEncrypt: null, headers: null, buildBody: null, queryParams: null, normalize: null,
        responseType: 'json', pageField: null, pageSizeField: null,
        defaultPageSize: 20, apiType: 'search', entryPath: null, isActive: 1, sortOrder: 0,
        usePageSign: 0, signScript: null, encryptDataPath: null, remark: null
      }
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.apiId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加API配置"
    },
    handleUpdate(row) {
      this.reset()
      const apiId = row.apiId || this.ids
      getPlatformApi(apiId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改API配置"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.apiId != null) {
            updatePlatformApi(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPlatformApi(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleStatusChange(row) {
      const text = row.isActive === 1 ? "启用" : "禁用"
      this.$modal.confirm('确认要' + text + ' "' + row.apiName + '" 吗？').then(() => {
        return updatePlatformApi({ apiId: row.apiId, isActive: row.isActive })
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(() => {
        row.isActive = row.isActive === 1 ? 0 : 1
      })
    },
    handleDelete(row) {
      const apiIds = row.apiId || this.ids
      this.$modal.confirm('是否确认删除API ID为"' + apiIds + '"的数据？').then(() => {
        return delPlatformApi(apiIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>
