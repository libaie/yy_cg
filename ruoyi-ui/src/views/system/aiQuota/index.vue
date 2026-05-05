<template>
  <div class="quota-page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">AI 配额管理</h1>
        <p class="page-desc">为不同会员等级配置 AI 功能的调用额度与权限</p>
      </div>
      <div class="page-header-decoration"></div>
    </div>

    <!-- 配额表格卡片 -->
    <div class="quota-card">
      <el-table
        v-loading="loading"
        :data="quotaList"
        class="quota-table"
      >
        <el-table-column label="会员等级" prop="tierLevel" width="140">
          <template slot-scope="scope">
            <span class="tier-badge" :class="'tier-' + scope.row.tierLevel">
              {{ tierMap[scope.row.tierLevel] }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="每日对话限额" width="190">
          <template slot-scope="scope">
            <div class="quota-cell">
              <el-input-number
                v-model="scope.row.dailyChatLimit"
                :min="-1"
                :max="9999"
                size="small"
                @change="handleModify(scope.row)"
              />
              <span v-if="scope.row.dailyChatLimit === -1" class="unlimited-tag">无限</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="每日快捷功能" width="190">
          <template slot-scope="scope">
            <div class="quota-cell">
              <el-input-number
                v-model="scope.row.dailyToolLimit"
                :min="-1"
                :max="9999"
                size="small"
                @change="handleModify(scope.row)"
              />
              <span v-if="scope.row.dailyToolLimit === -1" class="unlimited-tag">无限</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单次最大Token" width="170">
          <template slot-scope="scope">
            <div class="quota-cell">
              <el-input-number
                v-model="scope.row.maxTokensPerReq"
                :min="100"
                :max="4000"
                :step="100"
                size="small"
                @change="handleModify(scope.row)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.enabled"
              :active-value="1"
              :inactive-value="0"
              @change="handleModify(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="" width="100" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              class="save-btn"
              @click="handleSave(scope.row)"
            >保存</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="card-footer">
        <span class="footer-hint">设定为 -1 表示不限制使用次数</span>
      </div>
    </div>
  </div>
</template>

<script>
import { listAiQuota, updateAiQuota } from "@/api/system/aiQuota"

export default {
  name: "AiQuota",
  data() {
    return {
      loading: false,
      quotaList: [],
      tierMap: {
        0: "普通用户",
        1: "黄金会员",
        2: "铂金会员",
        3: "钻石会员"
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAiQuota().then(response => {
        this.quotaList = response.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    handleModify(row) {
      // 值变更时的回调，暂不做额外处理
    },
    handleSave(row) {
      updateAiQuota(row).then(() => {
        this.$message.success("保存成功")
        this.getList()
      })
    }
  }
}
</script>

<style scoped>
/* ─────── Page Layout ─────── */
.quota-page {
  min-height: 100vh;
  padding: 32px 36px;
  background: #f8fafc;
}

/* ─────── Page Header ─────── */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 32px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #0f172a;
  line-height: 1.2;
}

.page-desc {
  margin: 6px 0 0;
  font-size: 14px;
  color: #94a3b8;
  font-weight: 400;
}

.page-header-decoration {
  width: 64px;
  height: 4px;
  border-radius: 2px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  margin-bottom: 8px;
}

/* ─────── Card ─────── */
.quota-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 8px 0 0 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02),
              0 8px 32px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.3s ease;
}

.quota-card:hover {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02),
              0 12px 40px rgba(0, 0, 0, 0.06);
}

/* ─────── Table ─────── */
.quota-table {
  font-size: 14px;
}

/* Tier badge */
.tier-badge {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  background: #f1f5f9;
}

.tier-badge.tier-1 {
  color: #b45309;
  background: #fef3c7;
}

.tier-badge.tier-2 {
  color: #6d28d9;
  background: #ede9fe;
}

.tier-badge.tier-3 {
  color: #065f46;
  background: #d1fae5;
}

/* Quota number input cell */
.quota-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Unlimited tag */
.unlimited-tag {
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  height: 22px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #6366f1;
  background: #eef2ff;
}

/* ─────── Save Button ─────── */
.save-btn {
  border: none !important;
  border-radius: 8px !important;
  padding: 6px 16px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  color: #ffffff !important;
  background: linear-gradient(135deg, #6366f1, #8b5cf6) !important;
  transition: all 0.25s ease !important;
}

.save-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

.save-btn:active {
  transform: translateY(0);
}

/* ─────── Card Footer ─────── */
.card-footer {
  padding: 16px 20px;
  border-top: none;
}

.footer-hint {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}
</style>
