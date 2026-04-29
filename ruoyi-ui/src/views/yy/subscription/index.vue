<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="会员开始时间" prop="startTime">
        <el-date-picker clearable
          v-model="queryParams.startTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择会员开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="会员到期时间" prop="endTime">
        <el-date-picker clearable
          v-model="queryParams.endTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择会员到期时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="支付状态" prop="payStatus">
        <el-select v-model="queryParams.payStatus" placeholder="请选择支付状态" clearable>
          <el-option
            v-for="dict in dict.type.yy_member_pay_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="系统订单号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          placeholder="请输入系统订单号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户手机号" prop="phone" v-hasPermi="['yy:subscription:list']">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入用户手机号"
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
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['yy:subscription:edit']"
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
          v-hasPermi="['yy:subscription:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['yy:subscription:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="subscriptionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="订阅记录ID" align="center" prop="subId" width="90" />
      <el-table-column label="用户手机号" align="center" prop="phone" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.user && scope.row.user.phone ? scope.row.user.phone : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="会员等级" align="center" prop="tierName" width="100">
        <template slot-scope="scope">
          <span>{{ scope.row.tier && scope.row.tier.tierName ? scope.row.tier.tierName : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="会员类型" align="center" prop="cardTitle" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.tier && scope.row.tier.cardTitle ? scope.row.tier.cardTitle : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="会员开始时间" align="center" prop="startTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="会员到期时间" align="center" prop="endTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" align="center" prop="payStatus" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.yy_member_pay_status" :value="scope.row.payStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="系统订单号" align="center" prop="orderNo" width="200" />
      <el-table-column label="支付流水号" align="center" prop="transactionId" width="180">
        <template slot-scope="scope">
          <span>{{ scope.row.transactionId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付超时时间" align="center" prop="payExpireTime" width="160">
        <template slot-scope="scope">
          <span v-if="scope.row.payStatus === 3" style="color: #E6A23C;">{{ parseTime(scope.row.payExpireTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          <span v-else>{{ scope.row.payExpireTime ? parseTime(scope.row.payExpireTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付时间" align="center" prop="payTime" width="160">
        <template slot-scope="scope">
          <span>{{ scope.row.payTime ? parseTime(scope.row.payTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['yy:subscription:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['yy:subscription:remove']"
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

    <!-- 修改会员订阅订单对话框（仅允许修改业务可调字段） -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <!-- 只读展示字段 -->
        <el-form-item label="订阅记录ID">
          <el-input :value="form.subId" disabled />
        </el-form-item>
        <el-form-item label="用户手机号">
          <el-input :value="(form.user && form.user.phone) || '-'" disabled />
        </el-form-item>
        <el-form-item label="套餐名称">
          <el-input :value="form.tierName" disabled />
        </el-form-item>
        <el-form-item label="系统订单号">
          <el-input :value="form.orderNo" disabled />
        </el-form-item>
        <el-form-item label="支付流水号">
          <el-input :value="form.transactionId || '-'" disabled />
        </el-form-item>
        <el-form-item label="支付时间">
          <el-input :value="form.payTime ? parseTime(form.payTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-'" disabled />
        </el-form-item>
        <el-divider content-position="left">可修改字段</el-divider>
        <!-- 可编辑字段 -->
        <el-form-item label="会员开始时间" prop="startTime">
          <el-date-picker clearable
            v-model="form.startTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择会员开始时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="会员到期时间" prop="endTime">
          <el-date-picker clearable
            v-model="form.endTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择会员到期时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="支付状态" prop="payStatus">
          <el-select v-model="form.payStatus" placeholder="请选择支付状态">
            <el-option
              v-for="dict in dict.type.yy_member_pay_status"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="支付超时时间" prop="payExpireTime">
          <el-date-picker clearable
            v-model="form.payExpireTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="请选择支付超时时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="支付时间" prop="payTime">
          <el-date-picker clearable
            v-model="form.payTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="请选择支付时间">
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
import { listSubscription, getSubscription, delSubscription, updateSubscription } from "@/api/yy/subscription"

export default {
  name: "Subscription",
  dicts: ['yy_member_pay_status'],
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
      // 会员订阅订单表格数据
      subscriptionList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        startTime: null,
        endTime: null,
        payStatus: null,
        orderNo: null,
        phone: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        startTime: [
          { required: true, message: "会员开始时间不能为空", trigger: "blur" }
        ],
        endTime: [
          { required: true, message: "会员到期时间不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询会员订阅订单列表 */
    getList() {
      this.loading = true
      listSubscription(this.queryParams).then(response => {
        this.subscriptionList = response.rows
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
        subId: null,
        userId: null,
        tierId: null,
        tierName: null,
        startTime: null,
        endTime: null,
        payStatus: null,
        orderNo: null,
        transactionId: null,
        payExpireTime: null,
        payTime: null,
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
      this.ids = selection.map(item => item.subId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const subId = row.subId || this.ids
      getSubscription(subId).then(response => {
        this.form = response.data
        // 填充套餐名称用于展示
        if (response.data.tier) {
          this.form.tierName = response.data.tier.tierName + ' - ' + response.data.tier.cardTitle
        }
        this.open = true
        this.title = "修改会员订阅订单"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateSubscription(this.form).then(response => {
            this.$modal.msgSuccess("修改成功")
            this.open = false
            this.getList()
          })
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const subIds = row.subId || this.ids
      this.$modal.confirm('是否确认删除会员订阅订单编号为"' + subIds + '"的数据项？').then(function() {
        return delSubscription(subIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('yy/subscription/export', {
        ...this.queryParams
      }, `subscription_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
