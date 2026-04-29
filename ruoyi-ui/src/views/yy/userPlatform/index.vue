<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户手机" prop="phone">
        <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="所属平台" prop="platformId">
        <el-select v-model="queryParams.platformId" placeholder="请选择平台" clearable>
          <el-option v-for="p in platformOptions" :key="p.pId" :label="p.platformName" :value="p.pId" />
        </el-select>
      </el-form-item>
      <el-form-item label="绑定状态" prop="bindStatus">
        <el-select v-model="queryParams.bindStatus" placeholder="请选择" clearable>
          <el-option label="已绑定" :value="1" />
          <el-option label="未绑定" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="登录状态" prop="loginStatus">
        <el-select v-model="queryParams.loginStatus" placeholder="请选择" clearable>
          <el-option label="在线" :value="1" />
          <el-option label="离线" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['yy:userPlatform:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['yy:userPlatform:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="userPlatformList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户" align="center" width="140">
        <template slot-scope="scope">
          {{ scope.row.userPhone || scope.row.platformNickname || scope.row.platformUsername || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="所属平台" align="center" width="120">
        <template slot-scope="scope">{{ getPlatformName(scope.row.platformId) }}</template>
      </el-table-column>
      <el-table-column label="平台账号" align="center" prop="platformUsername" width="140" />
      <el-table-column label="绑定状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.bindStatus === 1" type="success" size="small">已绑定</el-tag>
          <el-tag v-else type="info" size="small">未绑定</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="登录状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.loginStatus === 1" size="small">在线</el-tag>
          <el-tag v-else type="info" size="small">离线</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Token" align="center" prop="token" :show-overflow-tooltip="true" width="120" />
      <el-table-column label="Token过期" align="center" width="160">
        <template slot-scope="scope">{{ parseTime(scope.row.tokenExpireTime, '{y}-{m}-{d} {h}:{i}') || '-' }}</template>
      </el-table-column>
      <el-table-column label="绑定时间" align="center" width="160">
        <template slot-scope="scope">{{ parseTime(scope.row.bindTime, '{y}-{m}-{d} {h}:{i}') || '-' }}</template>
      </el-table-column>
      <el-table-column label="最后登录" align="center" width="160">
        <template slot-scope="scope">{{ parseTime(scope.row.lastLoginTime, '{y}-{m}-{d} {h}:{i}') || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['yy:userPlatform:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['yy:userPlatform:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户ID" prop="userId">
              <el-input v-model="form.userId" placeholder="请输入用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属平台" prop="platformId">
              <el-select v-model="form.platformId" placeholder="请选择平台" style="width:100%">
                <el-option v-for="p in platformOptions" :key="p.pId" :label="p.platformName" :value="p.pId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="绑定状态" prop="bindStatus">
              <el-select v-model="form.bindStatus" style="width:100%">
                <el-option label="已绑定" :value="1" />
                <el-option label="未绑定" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="登录状态" prop="loginStatus">
              <el-select v-model="form.loginStatus" style="width:100%">
                <el-option label="在线" :value="1" />
                <el-option label="离线" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="平台账号" prop="platformUsername">
          <el-input v-model="form.platformUsername" placeholder="如：176****6602" />
        </el-form-item>
        <el-form-item label="平台昵称" prop="platformNickname">
          <el-input v-model="form.platformNickname" placeholder="请输入平台昵称" />
        </el-form-item>
        <el-form-item label="Token" prop="token">
          <el-input v-model="form.token" type="textarea" :rows="2" placeholder="平台Token" />
        </el-form-item>
        <el-form-item label="Token过期" prop="tokenExpireTime">
          <el-date-picker v-model="form.tokenExpireTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择过期时间" style="width:100%" />
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
import { listUserPlatform, getUserPlatform, delUserPlatform, addUserPlatform, updateUserPlatform } from "@/api/yy/userPlatform"
import { listAllPlatform } from "@/api/yy/platform"

export default {
  name: "UserPlatform",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      userPlatformList: [],
      platformOptions: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        platformId: null,
        bindStatus: null,
        loginStatus: null
      },
      form: {},
      rules: {
        userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
        platformId: [{ required: true, message: "请选择平台", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
    this.loadPlatforms()
  },
  methods: {
    getList() {
      this.loading = true
      listUserPlatform(this.queryParams).then(response => {
        this.userPlatformList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    loadPlatforms() {
      listAllPlatform().then(response => {
        this.platformOptions = response.data || []
      })
    },
    getPlatformName(platformId) {
      const p = this.platformOptions.find(o => o.pId === platformId)
      return p ? p.platformName : ('ID:' + platformId)
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: null, userId: null, platformId: null,
        bindStatus: 0, loginStatus: 0,
        token: null, tokenExpireTime: null,
        platformUsername: null, platformNickname: null,
        bindTime: null, lastLoginTime: null, lastSyncTime: null
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
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加用户平台绑定"
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getUserPlatform(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改用户平台绑定"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateUserPlatform(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addUserPlatform(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除ID为"' + ids + '"的数据？').then(() => {
        return delUserPlatform(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>
