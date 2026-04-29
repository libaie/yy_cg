<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员等级" prop="memberLevel">
        <el-select v-model="queryParams.memberLevel" placeholder="请选择会员等级" clearable>
          <el-option
            v-for="dict in dict.type.yy_member_tier_name"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="会员到期时间" prop="memberExpireTime" label-width="110px">
        <el-date-picker clearable
          v-model="queryParams.memberExpireTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择到期时间">
        </el-date-picker>
      </el-form-item>
      <!-- <el-form-item label="首次成为会员的时间" prop="firstMemberTime">
        <el-date-picker clearable
          v-model="queryParams.firstMemberTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择首次成为会员的时间">
        </el-date-picker>
      </el-form-item> -->
      <el-form-item label="邀请码" prop="inviteCode">
        <el-input
          v-model="queryParams.inviteCode"
          placeholder="请输入邀请码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['yy:user:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['yy:user:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['yy:user:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['yy:user:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="userId" />
      <el-table-column label="手机号" align="center" prop="phone" />
      <el-table-column label="昵称" align="center" prop="nickName" />
      <el-table-column label="头像链接" align="center" prop="avatar" />
      <el-table-column label="会员等级" align="center" prop="memberLevel">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.yy_member_tier_name" :value="scope.row.memberLevel"/>
        </template>
      </el-table-column>
      <!-- <el-table-column label="关联yy_member_tier表的ID" align="center" prop="memberLevelId" /> -->
      <el-table-column label="到期时间" align="center" prop="memberExpireTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.memberExpireTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="首次成为会员的时间" align="center" prop="firstMemberTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.firstMemberTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="账户余额" align="center" prop="balance" />
      <el-table-column label="专属Chatbot链接" align="center" prop="chatbotUrl" />
      <el-table-column label="邀请码" align="center" prop="inviteCode" />
      <el-table-column label="用户注册时间" align="center" prop="regTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.regTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['yy:user:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['yy:user:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改用户对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入密码" show-password type="password" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="头像链接" prop="avatar">
          <el-input v-model="form.avatar" placeholder="请输入头像链接" />
        </el-form-item>
        <el-form-item label="会员等级" prop="memberLevel">
          <el-select v-model="form.memberLevel" placeholder="请选择会员等级">
            <el-option
              v-for="dict in dict.type.yy_member_tier_name"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <!-- <el-form-item label="关联yy_member_tier表的ID" prop="memberLevelId">
          <el-input v-model="form.memberLevelId" placeholder="请输入关联yy_member_tier表的ID" />
        </el-form-item> -->
        <el-form-item label="到期时间" prop="memberExpireTime">
          <el-date-picker clearable
            v-model="form.memberExpireTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择到期时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="首次成为会员的时间" prop="firstMemberTime">
          <el-date-picker clearable
            v-model="form.firstMemberTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择首次成为会员的时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="账户余额" prop="balance">
          <el-input v-model="form.balance" placeholder="请输入账户余额" />
        </el-form-item>
        <el-form-item label="专属Chatbot链接" prop="chatbotUrl">
          <el-input v-model="form.chatbotUrl" placeholder="请输入专属Chatbot链接" />
        </el-form-item>
        <el-form-item label="邀请码" prop="inviteCode">
          <el-input v-model="form.inviteCode" placeholder="请输入邀请码" />
        </el-form-item>
        <el-form-item label="用户注册时间" prop="regTime">
          <el-date-picker clearable
            v-model="form.regTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择用户注册时间">
          </el-date-picker>
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
import { listUser, getUser, delUser, addUser, updateUser } from "@/api/yy/user"

export default {
  name: "User",
  dicts: ['yy_member_tier_name'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 用户表格数据
      userList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        phone: null,
        memberLevel: null,
        memberExpireTime: null,
        firstMemberTime: null,
        inviteCode: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        phone: [
          { required: true, message: "手机号不能为空", trigger: "blur" }
        ],
        password: [
          { required: true, message: "密码不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询用户列表 */
    getList() {
      this.loading = true
      listUser(this.queryParams).then(response => {
        this.userList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        userId: null,
        phone: null,
        password: null,
        nickName: null,
        avatar: null,
        memberLevel: null,
        memberLevelId: null,
        memberExpireTime: null,
        firstMemberTime: null,
        balance: null,
        chatbotUrl: null,
        inviteCode: null,
        regTime: null,
        createTime: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.userId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加用户"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const userId = row.userId || this.ids
      getUser(userId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改用户"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.userId != null) {
            updateUser(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addUser(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const userIds = row.userId || this.ids
      this.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(function() {
        return delUser(userIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('yy/user/export', {
        ...this.queryParams
      }, `user_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
