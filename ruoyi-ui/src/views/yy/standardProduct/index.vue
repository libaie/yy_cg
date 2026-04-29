<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item v-for="field in searchFields" :key="field.key" :label="field.label" :prop="field.key">
        <!-- 下拉选择 -->
        <el-select v-if="field.type === 'select'" v-model="queryParams[field.key]" :placeholder="field.placeholder || '全部'" clearable>
          <el-option v-for="opt in field.options" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <!-- 输入框 -->
        <el-input v-else v-model="queryParams[field.key]" :placeholder="field.placeholder || '请输入'" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="productList" border @row-click="handleRowClick">
      <el-table-column 
        v-for="col in tableColumns" 
        :key="col.prop"
        :label="col.label" 
        :prop="col.prop" 
        :width="col.width" 
        :min-width="col.minWidth"
        :fixed="col.fixed"
        :align="col.align"
        :show-overflow-tooltip="!col.slot"
      >
        <template slot-scope="scope" v-if="col.slot">
          <!-- 价格类 -->
          <span v-if="col.type === 'price'" :style="col.style">
            {{ scope.row[col.prop] ? '¥' + scope.row[col.prop] : '-' }}
          </span>
          <!-- 状态类 -->
          <template v-else-if="col.type === 'status'">
            <el-tag v-for="opt in col.options" :key="opt.value" v-if="scope.row[col.prop] === opt.value" :type="opt.tagType" size="small">{{ opt.label }}</el-tag>
            <span v-if="!col.options.find(o => o.value === scope.row[col.prop])">-</span>
          </template>
          <!-- 布尔类 -->
          <template v-else-if="col.type === 'boolean'">
            <el-tag v-if="scope.row[col.prop] === 1" type="success" size="small">{{ col.trueLabel || '是' }}</el-tag>
            <span v-else>{{ col.falseLabel || '否' }}</span>
          </template>
          <!-- JSON数组类 -->
          <template v-else-if="col.type === 'json'">
            {{ formatJson(scope.row[col.prop]) }}
          </template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click.stop="handleDetail(scope.row)">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click.stop="handleEdit(scope.row)">编辑</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog :visible.sync="detailVisible" title="商品详情" width="900px" append-to-body>
      <el-descriptions :column="2" border v-if="currentProduct">
        <el-descriptions-item v-for="field in detailFields" :key="field.key" :label="field.label">
          <!-- 价格类 -->
          <span v-if="field.type === 'price'" style="color: #f56c6c; font-weight: bold;">
            {{ currentProduct[field.key] ? '¥' + currentProduct[field.key] : '-' }}
          </span>
          <!-- 状态类 -->
          <template v-else-if="field.type === 'status'">
            <el-tag v-for="opt in field.options" :key="opt.value" v-if="currentProduct[field.key] === opt.value" :type="opt.tagType" size="small">{{ opt.label }}</el-tag>
            <span v-else-if="currentProduct[field.key] == null">-</span>
          </template>
          <!-- 布尔类 -->
          <template v-else-if="field.type === 'boolean'">
            <el-tag v-if="currentProduct[field.key] === 1" type="success" size="small">{{ field.trueLabel || '是' }}</el-tag>
            <span v-else>{{ field.falseLabel || '否' }}</span>
          </template>
          <!-- JSON类 -->
          <template v-else-if="field.type === 'json'">
            <pre v-if="currentProduct[field.key]" style="margin:0;white-space:pre-wrap;">{{ formatJsonPretty(currentProduct[field.key]) }}</pre>
            <span v-else>-</span>
          </template>
          <!-- 普通文本 -->
          <template v-else>
            {{ currentProduct[field.key] || '-' }}
          </template>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { listStandardProduct, getStandardProduct, updateStandardProduct } from '@/api/yy/standardProduct'
import { listPlatform } from '@/api/yy/platform'
import { TABLE_COLUMNS, DETAIL_FIELDS, SEARCH_FIELDS } from './config'

export default {
  name: "StandardProduct",
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      productList: [],
      platformList: [],
      detailVisible: false,
      currentProduct: null,
      // 配置驱动
      tableColumns: TABLE_COLUMNS,
      detailFields: DETAIL_FIELDS,
      searchFields: SEARCH_FIELDS,
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        sourcePlatform: null,
        commonName: null,
        productName: null,
        approvalNumber: null,
        barcode: null,
        productStatus: null
      }
    };
  },
  created() {
    this.getList();
    this.getPlatformList();
  },
  methods: {
    getList() {
      this.loading = true;
      listStandardProduct(this.queryParams).then(response => {
        this.productList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    getPlatformList() {
      listPlatform().then(response => {
        this.platformList = response.rows;
        // 动态更新搜索字段的平台选项
        const platformField = this.searchFields.find(f => f.key === 'sourcePlatform');
        if (platformField) {
          platformField.options = response.rows.map(p => ({
            label: p.platformName,
            value: p.platformCode
          }));
        }
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
    handleRowClick(row) {
      this.handleDetail(row);
    },
    handleDetail(row) {
      this.currentProduct = row;
      this.detailVisible = true;
    },
    handleEdit(row) {
      this.$message.info('编辑功能待实现');
    },
    formatJson(val) {
      if (!val) return '-';
      try {
        const arr = JSON.parse(val);
        if (Array.isArray(arr)) return arr.length + '项';
        return '有数据';
      } catch {
        return '-';
      }
    },
    formatJsonPretty(val) {
      if (!val) return '-';
      try {
        return JSON.stringify(JSON.parse(val), null, 2);
      } catch {
        return val;
      }
    }
  }
};
</script>
