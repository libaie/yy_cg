<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="等级名称" prop="tierName">
        <el-select v-model="queryParams.tierName" placeholder="请选择等级名称" clearable>
          <el-option
            v-for="dict in dict.type.yy_member_tier_name"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="卡片标题" prop="cardTitle">
        <el-input
          v-model="queryParams.cardTitle"
          placeholder="请输入卡片标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="卡种" prop="cardType">
        <el-select v-model="queryParams.cardType" placeholder="请选择卡种" clearable>
          <el-option
            v-for="dict in dict.type.yy_member_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <!-- <el-form-item label="有效期(天数)" prop="durationDays">
        <el-input
          v-model="queryParams.durationDays"
          placeholder="请输入有效期(天数)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item> -->
      <el-form-item label="售卖价格" prop="price">
        <el-input
          v-model="queryParams.price"
          placeholder="请输入售卖价格"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <!-- <el-form-item label="卡片标签" prop="cardTag">
        <el-input
          v-model="queryParams.cardTag"
          placeholder="请输入卡片标签"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item> -->
      <el-form-item label="限购次数" prop="limitCount">
        <el-select v-model="queryParams.limitCount" placeholder="请选择限购次数" clearable>
          <el-option
            v-for="dict in dict.type.yy_member_limit_count"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否上架" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="请选择是否上架" clearable>
          <el-option
            v-for="dict in dict.type.yy_member_is_active"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['yy:tier:add']"
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
          v-hasPermi="['yy:tier:edit']"
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
          v-hasPermi="['yy:tier:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['yy:tier:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="tierList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="tierId" />
      <el-table-column label="等级名称" align="center" prop="tierName">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.yy_member_tier_name" :value="scope.row.tierName"/>
        </template>
      </el-table-column>
      <el-table-column label="卡片标题" align="center" prop="cardTitle" />
      <el-table-column label="卡种" align="center" prop="cardType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.yy_member_type" :value="scope.row.cardType"/>
        </template>
      </el-table-column>
      <el-table-column label="有效期(天数)" align="center" prop="durationDays" />
      <el-table-column label="售卖价格" align="center" prop="price" />
      <el-table-column label="权益描述" align="center" prop="privileges" />
      <el-table-column label="卡片标签" align="center" prop="cardTag" />
      <el-table-column label="限购次数" align="center" prop="limitCount">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.yy_member_limit_count" :value="scope.row.limitCount"/>
        </template>
      </el-table-column>
      <el-table-column label="是否上架" align="center" prop="isActive">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.yy_member_is_active" :value="scope.row.isActive"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['yy:tier:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['yy:tier:remove']"
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

    <!-- 添加或修改会员套餐配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="等级名称" prop="tierName">
          <el-select v-model="form.tierName" placeholder="请选择等级名称">
            <el-option
              v-for="dict in dict.type.yy_member_tier_name"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="卡片标题" prop="cardTitle">
          <el-input v-model="form.cardTitle" placeholder="请输入卡片标题" />
        </el-form-item>
        <el-form-item label="卡种" prop="cardType">
          <el-select v-model="form.cardType" placeholder="请选择卡种">
            <el-option
              v-for="dict in dict.type.yy_member_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="有效期(天数)" prop="durationDays">
          <el-input v-model="form.durationDays" placeholder="请输入有效期(天数)" />
        </el-form-item>
        <el-form-item label="售卖价格" prop="price">
          <el-input v-model="form.price" placeholder="请输入售卖价格" />
        </el-form-item>
        <el-form-item label="权益描述" prop="privileges">
          <el-input v-model="form.privileges" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="卡片标签" prop="cardTag">
          <el-input v-model="form.cardTag" placeholder="请输入卡片标签" />
        </el-form-item>
        <el-form-item label="限购次数" prop="limitCount">
          <el-select v-model="form.limitCount" placeholder="请选择限购次数">
            <el-option
              v-for="dict in dict.type.yy_member_limit_count"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="是否上架" prop="isActive">
          <el-select v-model="form.isActive" placeholder="请选择是否上架">
            <el-option
              v-for="dict in dict.type.yy_member_is_active"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
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
import { listTier, getTier, delTier, addTier, updateTier } from "@/api/yy/tier"

export default {
  name: "Tier",
  dicts: ['yy_member_is_active', 'yy_member_type', 'yy_member_limit_count', 'yy_member_tier_name'],
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
      // 会员套餐配置表格数据
      tierList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        tierName: null,
        cardTitle: null,
        cardType: null,
        durationDays: null,
        price: null,
        privileges: null,
        cardTag: null,
        limitCount: null,
        isActive: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        tierName: [
          { required: true, message: "等级名称不能为空", trigger: "change" }
        ],
        cardTitle: [
          { required: true, message: "卡片标题不能为空", trigger: "blur" }
        ],
        cardType: [
          { required: true, message: "卡种不能为空", trigger: "change" }
        ],
        durationDays: [
          { required: true, message: "有效期(天数)不能为空", trigger: "blur" }
        ],
        price: [
          { required: true, message: "售卖价格不能为空", trigger: "blur" }
        ],
        // cardTag: [
        //   { required: true, message: "卡片标签不能为空", trigger: "blur" }
        // ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询会员套餐配置列表 */
    getList() {
      this.loading = true
      listTier(this.queryParams).then(response => {
        this.tierList = response.rows
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
        tierId: null,
        tierName: null,
        cardTitle: null,
        cardType: null,
        durationDays: null,
        price: null,
        privileges: null,
        cardTag: null,
        limitCount: null,
        isActive: null,
        createTime: null,
        memberLevel: null
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
      this.ids = selection.map(item => item.tierId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加会员套餐配置"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const tierId = row.tierId || this.ids
      getTier(tierId).then(response => {
        this.form = response.data
        // 数据库 tierName 存的是字典中文标签，需要转回字典值供 el-select 绑定
        if (this.form.tierName) {
          let dictItem = this.dict.type.yy_member_tier_name.find(item => item.label === this.form.tierName)
          if (dictItem) {
            this.form.tierName = dictItem.value
          }
        }
        this.open = true
        this.title = "修改会员套餐配置"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 统一从 tierName (字典值) 解析出 memberLevel 和中文名称
          let val = this.form.tierName
          if (val != null) {
            this.form.memberLevel = parseInt(val)
            let dictItem = this.dict.type.yy_member_tier_name.find(item => item.value === val)
            if (dictItem) {
              this.form.tierName = dictItem.label
            }
          }
          if (this.form.tierId != null) {
            updateTier(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addTier(this.form).then(response => {
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
      const tierIds = row.tierId || this.ids
      this.$modal.confirm('是否确认删除会员套餐配置编号为"' + tierIds + '"的数据项？').then(function() {
        return delTier(tierIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('yy/tier/export', {
        ...this.queryParams
      }, `tier_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
