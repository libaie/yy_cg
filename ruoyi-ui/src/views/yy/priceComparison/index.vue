<template>
  <div class="app-container price-comparison">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item v-for="field in searchFields" :key="field.key" :label="field.label" :prop="field.key">
        <el-select 
          v-if="field.type === 'select'" 
          v-model="queryParams[field.key]" 
          :placeholder="field.placeholder || '全部'" 
          :multiple="field.multiple"
          clearable
        >
          <el-option v-for="opt in field.options" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-input 
          v-else 
          v-model="queryParams[field.key]" 
          :placeholder="field.placeholder || '请输入'" 
          clearable 
          @keyup.enter.native="handleQuery" 
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-cards" v-if="stats">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalProducts || 0 }}</div>
            <div class="stat-label">比价商品数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ stats.platformCount || 0 }}</div>
            <div class="stat-label">覆盖平台</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">¥{{ stats.avgSavedAmount || 0 }}</div>
            <div class="stat-label">平均节省</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ stats.activityPercent || 0 }}%</div>
            <div class="stat-label">活动商品占比</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 比价结果 -->
    <el-card class="comparison-card" v-if="comparisonData.length > 0">
      <div slot="header" class="card-header">
        <span>{{ currentProduct.productName }} - 比价结果</span>
        <el-button type="text" icon="el-icon-refresh" @click="refreshComparison">刷新</el-button>
      </div>
      
      <el-table :data="comparisonData" border stripe>
        <el-table-column 
          v-for="col in tableColumns" 
          :key="col.prop"
          :prop="col.prop" 
          :label="col.label" 
          :width="col.width"
          :align="col.align"
          :fixed="col.fixed"
        >
          <template slot-scope="scope">
            <!-- 价格类 -->
            <span v-if="col.type === 'price'" :style="col.style || ''">
              {{ scope.row[col.prop] != null ? '¥' + scope.row[col.prop] : '-' }}
            </span>
            <!-- 布尔类 -->
            <template v-else-if="col.type === 'boolean'">
              <el-tag v-if="scope.row[col.prop] === true" type="success" size="small">是</el-tag>
              <span v-else>否</span>
            </template>
            <!-- 标签类 -->
            <template v-else-if="col.type === 'tag'">
              <el-tag v-if="scope.row[col.prop]" :type="getActivityTagType(scope.row[col.prop])" size="small">
                {{ scope.row[col.prop] }}
              </el-tag>
              <span v-else>-</span>
            </template>
            <!-- 普通文本 -->
            <template v-else>
              {{ scope.row[col.prop] || '-' }}
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-else description="请输入SKU ID或商品名称进行比价" />
  </div>
</template>

<script>
import { comparePrices, getComparisonStats } from '@/api/yy/priceComparison'
import { SEARCH_FIELDS, COMPARISON_TABLE_COLUMNS, CUSTOMER_TYPE_OPTIONS } from './config'

export default {
  name: 'PriceComparison',
  data() {
    return {
      loading: false,
      showSearch: true,
      searchFields: SEARCH_FIELDS,
      tableColumns: COMPARISON_TABLE_COLUMNS,
      customerTypeOptions: CUSTOMER_TYPE_OPTIONS,
      queryParams: {
        skuId: null,
        productName: null,
        commonName: null,
        customerType: 'single',
        platformCodes: [],
        hasActivity: null
      },
      currentProduct: {},
      comparisonData: [],
      stats: null
    }
  },
  created() {
    this.getStats()
  },
  methods: {
    // 查询比价
    handleQuery() {
      if (!this.queryParams.skuId && !this.queryParams.productName) {
        this.$message.warning('请输入SKU ID或商品名称')
        return
      }
      
      this.loading = true
      comparePrices(this.queryParams.skuId, this.queryParams.customerType)
        .then(response => {
          this.comparisonData = response.data || []
          if (this.comparisonData.length > 0) {
            this.currentProduct = {
              skuId: this.queryParams.skuId,
              productName: this.comparisonData[0].productName
            }
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    
    // 重置查询
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.queryParams = {
        skuId: null,
        productName: null,
        commonName: null,
        customerType: 'single',
        platformCodes: [],
        hasActivity: null
      }
      this.comparisonData = []
      this.currentProduct = {}
    },
    
    // 刷新比价
    refreshComparison() {
      this.handleQuery()
    },
    
    // 获取统计信息
    getStats() {
      getComparisonStats(this.queryParams.customerType)
        .then(response => {
          this.stats = response.data || {}
        })
    },
    
    // 获取活动标签类型
    getActivityTagType(activityType) {
      const typeMap = {
        '拼团': 'danger',
        '凑单': 'warning',
        '会员价': 'success',
        '限时折扣': 'primary',
        '专享价': 'info'
      }
      return typeMap[activityType] || ''
    }
  }
}
</script>

<style scoped>
.price-comparison {
  padding: 20px;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 10px 0;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.comparison-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 16px;
  font-weight: bold;
}
</style>
