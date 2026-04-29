<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="推荐人ID" prop="referrerId">
        <el-input v-model="queryParams.referrerId" placeholder="请输入推荐人ID" clearable
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="被推荐人ID" prop="referredId">
        <el-input v-model="queryParams.referredId" placeholder="请输入被推荐人ID" clearable
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="推荐层级" prop="referralDepth">
        <el-select v-model="queryParams.referralDepth" placeholder="请选择" clearable>
          <el-option label="直推" value="1" />
          <el-option label="间推" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable>
          <el-option label="待发放" value="0" />
          <el-option label="已发放" value="1" />
          <el-option label="已取消" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="rewardList">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="推荐人" align="center" prop="referrerPhone" width="130">
        <template slot-scope="scope">
          {{ scope.row.referrerPhone || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="被推荐人" align="center" prop="referredPhone" width="130">
        <template slot-scope="scope">
          {{ scope.row.referredPhone || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="推荐层级" align="center" prop="referralDepth" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.referralDepth === 1 ? 'success' : 'warning'" size="small">
            {{ scope.row.referralDepth === 1 ? '直推' : '间推' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="触发类型" align="center" prop="triggerType" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.triggerType === 1">充值</span>
          <span v-else-if="scope.row.triggerType === 2">续费</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="订单金额" align="center" prop="triggerAmount" width="120">
        <template slot-scope="scope">
          ¥{{ scope.row.triggerAmount || '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="计算方式" align="center" prop="calculatedBy" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.calculatedBy === 1">固定</span>
          <span v-else-if="scope.row.calculatedBy === 2">百分比</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="奖励金额" align="center" prop="rewardAmount" width="120">
        <template slot-scope="scope">
          <span style="color: #E6A23C; font-weight: bold;">¥{{ scope.row.rewardAmount || '0.00' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="订单ID" align="center" prop="payOrderId" width="120" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />
  </div>
</template>

<script>
import { listReferralReward } from "@/api/yy/referralReward";

export default {
  name: "ReferralReward",
  data() {
    return {
      loading: true,
      rewardList: [],
      total: 0,
      showSearch: true,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        referrerId: null,
        referredId: null,
        referralDepth: null,
        status: null
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listReferralReward(this.queryParams).then(response => {
        this.rewardList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    statusTagType(status) {
      const map = { 0: 'info', 1: 'success', 2: 'danger' };
      return map[status] || 'info';
    },
    statusLabel(status) {
      const map = { 0: '待发放', 1: '已发放', 2: '已取消' };
      return map[status] || '未知';
    }
  }
};
</script>
