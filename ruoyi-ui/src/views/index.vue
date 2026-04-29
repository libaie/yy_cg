<template>
  <div class="home-container">
    <!-- 顶部品牌区 -->
    <div class="home-hero">
      <div class="hero-content">
        <h1 class="hero-title">医药采购管理系统</h1>
        <p class="hero-desc">
          多平台采购对接 · 统一管理 · 智能同步 — 支持京东、淘宝、顺丰等主流采购平台API接入与数据同步，实现药品采购全流程数字化管理。
        </p>
      </div>
    </div>

    <!-- 平台功能快捷入口 -->
    <div class="home-section">
      <div class="section-header">
        <h2>平台管理</h2>
        <p>接入配置 · 授权同步 · 套餐管理</p>
      </div>
      <div class="card-grid">
        <div class="action-card" @click="goTo('/yy/platform')">
          <div class="card-icon blue">
            <i class="el-icon-office-building"></i>
          </div>
          <h3>平台管理</h3>
          <p>管理采购平台信息，配置平台参数与接口状态</p>
        </div>
        <div class="action-card" @click="goTo('/yy/platformApi')">
          <div class="card-icon cyan">
            <i class="el-icon-connection"></i>
          </div>
          <h3>平台API</h3>
          <p>配置各采购平台API密钥，管理接口权限</p>
        </div>
        <div class="action-card" @click="goTo('/yy/user')">
          <div class="card-icon green">
            <i class="el-icon-user-solid"></i>
          </div>
          <h3>用户管理</h3>
          <p>管理系统用户，配置角色与操作权限</p>
        </div>
        <div class="action-card" @click="goTo('/yy/subscription')">
          <div class="card-icon orange">
            <i class="el-icon-wallet"></i>
          </div>
          <h3>套餐管理</h3>
          <p>设置平台套餐等级，管理有效期与权限</p>
        </div>
        <div class="action-card" @click="goTo('/yy/tier')">
          <div class="card-icon purple">
            <i class="el-icon-medal"></i>
          </div>
          <h3>等级管理</h3>
          <p>配置平台等级体系，定义升级规则</p>
        </div>
        <div class="action-card" @click="goTo('/yy/userPlatform')">
          <div class="card-icon pink">
            <i class="el-icon-s-order"></i>
          </div>
          <h3>用户平台关联</h3>
          <p>管理用户与采购平台的授权绑定关系</p>
        </div>
      </div>
    </div>

    <!-- 系统功能说明 -->
    <div class="home-section">
      <div class="section-header">
        <h2>核心功能</h2>
      </div>
      <div class="feature-grid">
        <div class="feature-item">
          <div class="feature-icon"><i class="el-icon-s-platform"></i></div>
          <h4>多平台接入</h4>
          <p>统一对接京东、淘宝、顺丰等多个主流采购平台，一次配置多处使用</p>
        </div>
        <div class="feature-item">
          <div class="feature-icon"><i class="el-icon-refresh-right"></i></div>
          <h4>自动同步</h4>
          <p>Token 自动刷新与数据同步，无需手动维护平台授权状态</p>
        </div>
        <div class="feature-item">
          <div class="feature-icon"><i class="el-icon-s-data"></i></div>
          <h4>等级体系</h4>
          <p>灵活的平台等级与套餐管理，支持多维度权限控制与有效期管理</p>
        </div>
        <div class="feature-item">
          <div class="feature-icon"><i class="el-icon-lock"></i></div>
          <h4>安全管控</h4>
          <p>API 密钥加密存储，操作日志全记录，权限细粒度控制</p>
        </div>
        <div class="feature-item">
          <div class="feature-icon"><i class="el-icon-setting"></i></div>
          <h4>灵活配置</h4>
          <p>支持平台状态开关、接口超时、重试策略等细粒度自定义配置</p>
        </div>
        <div class="feature-item">
          <div class="feature-icon"><i class="el-icon-s-tools"></i></div>
          <h4>系统运维</h4>
          <p>内置定时任务、服务监控、缓存管理，运维保障一站到位</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "Index",
  methods: {
    goTo(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped lang="scss">
.home-container {
  padding: 0;
  background: #F8FAFC;
  min-height: calc(100vh - 50px);
}

/* ===== Hero ===== */
.home-hero {
  background: linear-gradient(135deg, #1E40AF 0%, #3B82F6 40%, #0EA5E9 100%);
  padding: 48px 32px;
  text-align: center;
  color: #FFFFFF;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -20%;
    width: 600px;
    height: 600px;
    background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 70%);
    border-radius: 50%;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: -40%;
    left: -10%;
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(255,255,255,0.06) 0%, transparent 70%);
    border-radius: 50%;
  }

  .hero-content {
    position: relative;
    z-index: 1;
  }

  .hero-title {
    font-size: 32px;
    font-weight: 700;
    margin: 0 0 16px;
    letter-spacing: 0.02em;
  }

  .hero-desc {
    font-size: 15px;
    line-height: 1.8;
    max-width: 800px;
    margin: 0 auto;
    color: rgba(255, 255, 255, 0.85);
  }
}

/* ===== Section ===== */
.home-section {
  padding: 32px 24px;
  max-width: 1200px;
  margin: 0 auto;

  .section-header {
    margin-bottom: 24px;
    padding-bottom: 12px;
    border-bottom: 2px solid #E5E7EB;

    h2 {
      font-size: 20px;
      font-weight: 700;
      color: #1E293B;
      margin: 0 0 6px;
    }
    p {
      font-size: 13px;
      color: #94A3B8;
      margin: 0;
    }
  }
}

/* ===== Action Cards (快捷入口) ===== */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.action-card {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 28px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #F1F5F9;
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
    border-color: #BFDBFE;

    .card-icon {
      transform: scale(1.1);
    }
  }

  .card-icon {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;
    font-size: 22px;
    color: #FFFFFF;
    transition: transform 0.3s ease;

    &.blue { background: linear-gradient(135deg, #3B82F6, #2563EB); }
    &.cyan { background: linear-gradient(135deg, #0EA5E9, #0284C7); }
    &.green { background: linear-gradient(135deg, #10B981, #059669); }
    &.orange { background: linear-gradient(135deg, #F59E0B, #D97706); }
    &.purple { background: linear-gradient(135deg, #8B5CF6, #7C3AED); }
    &.pink { background: linear-gradient(135deg, #EC4899, #DB2777); }
  }

  h3 {
    font-size: 15px;
    font-weight: 600;
    color: #1E293B;
    margin: 0 0 8px;
  }
  p {
    font-size: 12px;
    color: #94A3B8;
    line-height: 1.6;
    margin: 0;
  }
}

/* ===== Features ===== */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.feature-item {
  background: #FFFFFF;
  border-radius: 10px;
  padding: 20px 16px;
  text-align: center;
  border: 1px solid #F1F5F9;
  transition: all 0.25s ease;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    border-color: #E2E8F0;
  }

  .feature-icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: #EFF6FF;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 12px;
    font-size: 18px;
    color: #3B82F6;
  }

  h4 {
    font-size: 14px;
    font-weight: 600;
    color: #334155;
    margin: 0 0 6px;
  }
  p {
    font-size: 12px;
    color: #94A3B8;
    line-height: 1.6;
    margin: 0;
  }
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .home-hero {
    padding: 32px 16px;
    .hero-title { font-size: 24px; }
    .hero-desc { font-size: 13px; }
  }
  .home-section { padding: 24px 12px; }
  .card-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .feature-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .action-card { padding: 20px 12px; }
}
</style>
