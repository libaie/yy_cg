<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="平台名称" prop="platformName">
        <el-input v-model="queryParams.platformName" placeholder="请输入平台名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="平台编码" prop="platformCode">
        <el-input v-model="queryParams.platformCode" placeholder="请输入平台编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="请选择状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['yy:platform:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['yy:platform:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['yy:platform:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['yy:platform:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="platformList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="平台名称" align="center" prop="platformName" width="120" />
      <el-table-column label="平台编码" align="center" prop="platformCode" width="100" />
      <el-table-column label="Logo" align="center" prop="platformLogoUrl" width="80">
        <template slot-scope="scope">
          <el-image v-if="scope.row.platformLogoUrl" :src="scope.row.platformLogoUrl" style="width:32px;height:32px" fit="contain" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="登录地址" align="center" prop="platformLoginUrl" :show-overflow-tooltip="true" />
      <el-table-column label="首页地址" align="center" prop="platformHomeUrl" :show-overflow-tooltip="true" />
      <el-table-column label="Token域名" align="center" prop="tokenDomain" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="Token键名" align="center" prop="tokenKey" width="120" />
      <el-table-column label="存储类型" align="center" prop="tokenStorageType" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.tokenStorageType === 'cookie'" size="small">Cookie</el-tag>
          <el-tag v-else-if="scope.row.tokenStorageType === 'localStorage'" size="small" type="success">LocalStorage</el-tag>
          <el-tag v-else-if="scope.row.tokenStorageType === 'sessionStorage'" size="small" type="warning">SessionStorage</el-tag>
          <span v-else>{{ scope.row.tokenStorageType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isActive" :active-value="1" :inactive-value="0"
            @change="handleStatusChange(scope.row)" v-hasPermi="['yy:platform:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-key" @click="handleVaultConfig(scope.row)" v-hasPermi="['yy:platform:edit']">密钥配置</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['yy:platform:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['yy:platform:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改平台信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="760px" append-to-body class="platform-dialog" top="8vh">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px" label-position="right">

        <!-- 基本信息 -->
        <div class="dialog-section">
          <h4 class="section-title"><i class="el-icon-info" /> 基本信息</h4>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="平台名称" prop="platformName">
                <el-input v-model="form.platformName" placeholder="如：药师帮" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="平台编码" prop="platformCode">
                <el-input v-model="form.platformCode" placeholder="如：ysbang" clearable />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="Logo" prop="platformLogoUrl">
            <el-input v-model="form.platformLogoUrl" placeholder="请输入Logo图片URL" clearable>
              <i slot="prefix" class="el-icon-picture-outline el-input__icon" />
            </el-input>
          </el-form-item>
        </div>

        <!-- 地址配置 -->
        <div class="dialog-section">
          <h4 class="section-title"><i class="el-icon-link" /> 地址配置</h4>
          <el-form-item label="登录地址" prop="platformLoginUrl">
            <el-input v-model="form.platformLoginUrl" placeholder="如：https://dian2.ysbang.cn/#/login" clearable>
              <i slot="prefix" class="el-icon-unlock el-input__icon" />
            </el-input>
          </el-form-item>
          <el-form-item label="首页地址" prop="platformHomeUrl">
            <el-input v-model="form.platformHomeUrl" placeholder="如：https://dian2.ysbang.cn/" clearable>
              <i slot="prefix" class="el-icon-office-building el-input__icon" />
            </el-input>
          </el-form-item>
        </div>

        <!-- Token 配置 -->
        <div class="dialog-section">
          <h4 class="section-title"><i class="el-icon-key" /> Token 配置</h4>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="Token域名" prop="tokenDomain">
                <el-input v-model="form.tokenDomain" placeholder="如：ysbang.cn" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Token键名" prop="tokenKey">
                <el-input v-model="form.tokenKey" placeholder="如：Token" clearable />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="存储类型" prop="tokenStorageType">
                <el-select v-model="form.tokenStorageType" placeholder="请选择存储类型" style="width:100%">
                  <el-option label="Cookie" value="cookie">
                    <span style="float:left">🍪</span>&nbsp;&nbsp;Cookie
                  </el-option>
                  <el-option label="LocalStorage" value="localStorage">
                    <span style="float:left">💾</span>&nbsp;&nbsp;LocalStorage
                  </el-option>
                  <el-option label="SessionStorage" value="sessionStorage">
                    <span style="float:left">🗄️</span>&nbsp;&nbsp;SessionStorage
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="排序权重" prop="sortOrder">
                <el-input-number v-model="form.sortOrder" :min="0" :max="999" controls-position="right" placeholder="0" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 状态控制 -->
        <div class="dialog-section">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="状态" prop="isActive">
                <el-radio-group v-model="form.isActive">
                  <el-radio :label="1">启用</el-radio>
                  <el-radio :label="0">禁用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 备注 -->
        <div class="dialog-section">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注信息" maxlength="500" show-word-limit clearable />
          </el-form-item>
        </div>

      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="密钥配置" :visible.sync="vaultOpen" width="680px" append-to-body class="platform-dialog" top="8vh">
      <el-form ref="vaultFormRef" :model="vaultForm" :rules="vaultRules" label-width="120px" label-position="right">
        <el-form-item label="平台名称">
          <el-input v-model="selectedPlatformName" disabled />
        </el-form-item>
        <el-form-item label="对称密钥(Key)" prop="symmetricKey">
          <el-input v-model="vaultForm.symmetricKey" placeholder="不修改请保留********" clearable />
          <div class="el-form-item__content" style="margin-top: 4px; font-size: 12px; color: #909399;">AES/DES/SM4 共同使用;若已有密钥则默认显示为 ********，提交时若不修改将保持原值。</div>
        </el-form-item>
        <el-form-item label="对称向量(IV)" prop="symmetricIv">
          <el-input v-model="vaultForm.symmetricIv" placeholder="不修改请保留********" clearable />
          <div class="el-form-item__content" style="margin-top: 4px; font-size: 12px; color: #909399;">若已有向量则默认显示为 ********，提交时若不修改将保持原值。</div>
        </el-form-item>
        <el-form-item label="RSA公钥" prop="rsaPublicKey">
          <el-input type="textarea" v-model="vaultForm.rsaPublicKey" :rows="3" placeholder="请输入RSA公钥" clearable />
        </el-form-item>
        <el-form-item label="RSA私钥" prop="rsaPrivateKey">
          <el-input type="textarea" v-model="vaultForm.rsaPrivateKey" :rows="3" placeholder="不修改请保留********" clearable />
          <div class="el-form-item__content" style="margin-top: 4px; font-size: 12px; color: #909399;">若已有私钥则默认显示为 ********，提交时若不修改将保持原值。</div>
        </el-form-item>
        <el-form-item label="App Key" prop="appKey">
          <el-input v-model="vaultForm.appKey" placeholder="不修改请保留********" clearable />
          <div class="el-form-item__content" style="margin-top: 4px; font-size: 12px; color: #909399;">若已有App Key则默认显示为 ********，提交时若不修改将保持原值。</div>
        </el-form-item>
        <el-form-item label="App Secret" prop="appSecret">
          <el-input v-model="vaultForm.appSecret" placeholder="不修改请保留********" clearable />
          <div class="el-form-item__content" style="margin-top: 4px; font-size: 12px; color: #909399;">若已有App Secret则默认显示为 ********，提交时若不修改将保持原值。</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input type="textarea" v-model="vaultForm.remark" :rows="3" placeholder="可选说明" clearable />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancelVault">取 消</el-button>
        <el-button type="primary" @click="submitVaultForm">保 存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPlatform, getPlatform, delPlatform, addPlatform, updatePlatform } from "@/api/yy/platform"
import { getVaultByPlatform, addVault, updateVault } from "@/api/yy/vault"

export default {
  name: "Platform",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      platformList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        platformName: null,
        platformCode: null,
        isActive: null
      },
      form: {},
      vaultOpen: false,
      vaultForm: {},
      vaultRules: {
        rsaPrivateKey: [{ required: false, message: "如果需要更新私钥请填写新值", trigger: "blur" }]
      },
      selectedPlatformName: '',
      rules: {
        platformName: [{ required: true, message: "平台名称不能为空", trigger: "blur" }],
        platformCode: [{ required: true, message: "平台编码不能为空", trigger: "blur" }],
        platformLoginUrl: [{ required: true, message: "登录地址不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listPlatform(this.queryParams).then(response => {
        this.platformList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    resetForm(refName) {
      const ref = this.$refs[refName]
      if (ref && typeof ref.resetFields === 'function') {
        ref.resetFields()
      }
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        pId: null,
        platformName: null,
        platformCode: null,
        platformLogoUrl: null,
        platformLoginUrl: null,
        platformHomeUrl: null,
        tokenDomain: null,
        tokenKey: null,
        tokenStorageType: 'cookie',
        isActive: 1,
        sortOrder: 0
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
      this.ids = selection.map(item => item.pId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加平台"
    },
    handleUpdate(row) {
      this.reset()
      const pId = row.pId || this.ids
      getPlatform(pId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改平台"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.pId != null) {
            updatePlatform(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPlatform(this.form).then(() => {
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
      this.$modal.confirm('确认要' + text + ' "' + row.platformName + '" 吗？').then(() => {
        return updatePlatform({ pId: row.pId, isActive: row.isActive })
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(() => {
        row.isActive = row.isActive === 1 ? 0 : 1
      })
    },
    handleDelete(row) {
      const pIds = row.pId || this.ids
      this.$modal.confirm('是否确认删除平台ID为"' + pIds + '"的数据？').then(() => {
        return delPlatform(pIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('yy/platform/export', { ...this.queryParams }, `platform_${new Date().getTime()}.xlsx`)
    },
    resetVaultForm() {
      this.vaultForm = {
        vaultId: null,
        platformId: null,
        symmetricKey: null,
        symmetricIv: null,
        rsaPublicKey: null,
        rsaPrivateKey: null,
        appKey: null,
        appSecret: null,
        remark: null
      }
      this.resetForm('vaultFormRef')
    },
    handleVaultConfig(row) {
      this.resetVaultForm()
      this.selectedPlatformName = row.platformName || ''
      this.vaultForm.platformId = row.pId
      this.vaultOpen = true
      getVaultByPlatform(row.pId).then(response => {
        const data = response.data || {}
        this.vaultForm = {
          vaultId: data.vaultId || null,
          platformId: data.platformId || row.pId,
          symmetricKey: data.symmetricKey || null,
          symmetricIv: data.symmetricIv || null,
          rsaPublicKey: data.rsaPublicKey || null,
          rsaPrivateKey: data.rsaPrivateKey || null,
          appKey: data.appKey || null,
          appSecret: data.appSecret || null,
          remark: data.remark || null
        }
      })
    },
    cancelVault() {
      this.vaultOpen = false
      this.selectedPlatformName = ''
      this.resetVaultForm()
    },
    submitVaultForm() {
      this.$refs['vaultFormRef'].validate(valid => {
        if (valid) {
          const api = this.vaultForm.vaultId ? updateVault : addVault
          api(this.vaultForm).then(() => {
            this.$modal.msgSuccess('保存成功')
            this.vaultOpen = false
            this.selectedPlatformName = ''
            this.resetVaultForm()
          })
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
/* ===== 弹窗内部区块 ===== */
.dialog-section {
  margin-bottom: 8px;

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #334155;
    margin: 0 0 12px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding-bottom: 8px;
    border-bottom: 1px dashed #E2E8F0;

    i {
      color: #3B82F6;
      font-size: 16px;
    }
  }
}

/* 弹窗覆盖默认样式 */
::v-deep .platform-dialog {
  .el-dialog__header {
    background: linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%);
    padding: 16px 24px 14px;
    border-bottom: 1px solid #E5E7EB;
  }
  .el-dialog__title {
    font-size: 17px;
    font-weight: 700;
    color: #1E293B;
  }
  .el-dialog__body {
    padding: 20px 28px !important;
    background: #FFFFFF;
  }
  .el-dialog__footer {
    padding: 14px 24px 18px !important;
    border-top: 1px solid #F1F5F9;
  }
  .el-form-item__label {
    font-size: 13px;
    color: #475569;
    font-weight: 500;
  }
  .el-input__inner,
  .el-textarea__inner {
    background: #F8FAFC;
    transition: all 0.25s ease;
  }
  .el-input__inner:focus,
  .el-textarea__inner:focus {
    background: #FFFFFF;
  }
  .el-input-number.is-controls-right .el-input__inner {
    text-indent: 8px;
  }
}
</style>
