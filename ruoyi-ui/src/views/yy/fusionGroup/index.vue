<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item v-for="field in searchFields" :key="field.key" :label="field.label" :prop="field.key">
        <el-input v-model="queryParams[field.key]" :placeholder="field.placeholder || '请输入'" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 融合分组表格 -->
    <el-table v-loading="loading" :data="groupList" border @selection-change="handleSelectionChange">
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column 
        v-for="col in tableColumns" 
        :key="col.prop"
        :label="col.label" 
        :prop="col.prop" 
        :width="col.width" 
        :min-width="col.minWidth"
        :align="col.align"
        :show-overflow-tooltip="!col.slot"
      >
        <template slot-scope="scope" v-if="col.slot">
          <!-- 平台数 -->
          <el-tag v-if="col.type === 'count'" type="success">{{ scope.row[col.prop] }}</el-tag>
          <!-- 价格 -->
          <span v-else-if="col.type === 'price'" style="color: #f56c6c; font-weight: bold;">
            {{ scope.row[col.prop] ? '¥' + scope.row[col.prop] : '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope">
          <el-button type="text" icon="el-icon-view" @click="handleView(scope.row)">查看平台</el-button>
          <el-button type="text" style="color: #F56C6C;" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 平台对比弹窗 -->
    <el-dialog :visible.sync="dialogVisible" title="各平台数据对比" width="1000px" append-to-body>
      <el-table :data="platformDataList" border>
        <el-table-column 
          v-for="col in platformColumns" 
          :key="col.prop"
          :label="col.label" 
          :prop="col.prop" 
          :width="col.width" 
          :min-width="col.minWidth"
          :align="col.align"
          :show-overflow-tooltip="!col.slot"
        >
          <template slot-scope="scope" v-if="col.slot">
            <span v-if="col.type === 'price'" style="color: #f56c6c; font-weight: bold;">
              {{ scope.row[col.prop] ? '¥' + scope.row[col.prop] : '-' }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listFusionGroup, delFusionGroup } from '@/api/yy/fusionGroup'
import { getStandardProductByFusionGroup } from '@/api/yy/standardProduct'
import { FUSION_TABLE_COLUMNS, FUSION_SEARCH_FIELDS, PLATFORM_COMPARE_COLUMNS } from './config'

export default {
  name: "FusionGroup",
  data() {
    return {
      loading: false,
      showSearch: true,
      ids: [],
      total: 0,
      groupList: [],
      platformDataList: [],
      dialogVisible: false,
      // 配置驱动
      tableColumns: FUSION_TABLE_COLUMNS,
      searchFields: FUSION_SEARCH_FIELDS,
      platformColumns: PLATFORM_COMPARE_COLUMNS,
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        commonName: null,
        manufacturer: null
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listFusionGroup(this.queryParams).then(res => {
        this.groupList = res.rows || [];
        this.total = res.total || 0;
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.searchFields.forEach(f => {
        this.queryParams[f.key] = null;
      });
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
    },
    handleView(row) {
      getStandardProductByFusionGroup(row.id).then(res => {
        this.platformDataList = res.data || [];
        this.dialogVisible = true;
      });
    },
    handleDelete(row) {
      this.$confirm('确定删除该融合分组及其关联的标准商品数据吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return delFusionGroup([row.id]);
      }).then(() => {
        this.$message.success('删除成功');
        this.getList();
      }).catch(() => {});
    }
  }
};
</script>
