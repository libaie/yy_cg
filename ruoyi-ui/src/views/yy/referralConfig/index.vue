<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="会员等级" prop="tierId">
        <el-input v-model="queryParams.tierId" placeholder="请输入套餐ID，留空表示全等级" clearable
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="请选择" clearable>
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
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
          v-hasPermi="['yy:config:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple"
          @click="handleDelete" v-hasPermi="['yy:config:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="configList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="会员套餐ID" align="center" prop="tierId" width="120" />
      <el-table-column label="奖励类型" align="center" prop="rewardType" width="120">
        <template slot-scope="scope">
          <span v-if="scope.row.rewardType === 1">固定金额</span>
          <span v-else-if="scope.row.rewardType === 2">百分比(%)</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="直推奖励" align="center" prop="directReward" width="120">
        <template slot-scope="scope">
          {{ isPercent ? scope.row.directReward + '%' : '¥' + scope.row.directReward }}
        </template>
      </el-table-column>
      <el-table-column label="间推奖励" align="center" prop="indirectReward" width="120">
        <template slot-scope="scope">
          {{ isPercent ? scope.row.indirectReward + '%' : '¥' + scope.row.indirectReward }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="isActive" width="100">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isActive" :active-value="1" :inactive-value="0"
            @change="handleStatusChange(scope.row)" v-hasPermi="['yy:config:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['yy:config:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['yy:config:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="适用套餐" prop="tierId">
          <el-input-number v-model="form.tierId" placeholder="留空=全等级通用" :min="0" :step="1"
            style="width: 100%" />
          <span style="color: #999; font-size: 12px;">留空表示对所有等级生效，有特定等级时优先匹配</span>
        </el-form-item>
        <el-form-item label="奖励类型" prop="rewardType">
          <el-radio-group v-model="form.rewardType">
            <el-radio :label="1">固定金额</el-radio>
            <el-radio :label="2">百分比(%)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-divider content-position="left">奖励金额</el-divider>
        <el-form-item :label="form.rewardType === 2 ? '直推奖励(%)' : '直推奖励(元)'" prop="directReward">
          <el-input-number v-model="form.directReward" :min="0" :precision="2" :max="form.rewardType === 2 ? 100 : 99999"
            style="width: 200px" />
          <span style="color: #999; font-size: 12px; margin-left: 10px;">
            {{ form.rewardType === 2 ? '占支付金额的百分比' : '固定金额，单位元' }}
          </span>
        </el-form-item>
        <el-form-item :label="form.rewardType === 2 ? '间推奖励(%)' : '间推奖励(元)'" prop="indirectReward">
          <el-input-number v-model="form.indirectReward" :min="0" :precision="2" :max="form.rewardType === 2 ? 100 : 99999"
            style="width: 200px" />
          <span style="color: #999; font-size: 12px; margin-left: 10px;">
            {{ form.rewardType === 2 ? '占支付金额的百分比' : '固定金额，单位元' }}
          </span>
        </el-form-item>
        <el-form-item label="状态" prop="isActive">
          <el-radio-group v-model="form.isActive">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
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
import { listReferralConfig, getReferralConfig, addReferralConfig, updateReferralConfig, delReferralConfig } from "@/api/yy/referralConfig";

export default {
  name: "ReferralConfig",
  data() {
    return {
      loading: true,
      ids: [],
      configList: [],
      title: "",
      open: false,
      multiple: true,
      showSearch: true,
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        tierId: null,
        isActive: null
      },
      form: {},
      rules: {
        rewardType: [
          { required: true, message: "请选择奖励类型", trigger: "change" }
        ],
        directReward: [
          { required: true, message: "请输入直推奖励", trigger: "blur" }
        ],
        indirectReward: [
          { required: true, message: "请输入间推奖励", trigger: "blur" }
        ]
      }
    };
  },
  computed: {
    isPercent() {
      return this.form.rewardType === 2 || (this.form.rewardType === undefined && this.queryParams.rewardType === 2);
    }
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listReferralConfig(this.queryParams).then(response => {
        this.configList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        id: null,
        tierId: null,
        rewardType: 1,
        directReward: 0,
        indirectReward: 0,
        isActive: 1,
        remark: null
      };
      this.resetForm("form");
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "新增推荐奖励配置";
    },
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids[0];
      getReferralConfig(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改推荐奖励配置";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateReferralConfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addReferralConfig(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除配置？').then(() => {
        return delReferralConfig(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.multiple = !selection.length;
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleStatusChange(row) {
      let text = row.isActive === 1 ? "启用" : "禁用";
      this.$modal.confirm('确认要"' + text + '""该配置吗？').then(() => {
        return updateReferralConfig(row);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        row.isActive = row.isActive === 1 ? 0 : 1;
      });
    }
  }
};
</script>

<style scoped>
.el-divider__text {
  font-weight: bold;
  font-size: 14px;
}
</style>
