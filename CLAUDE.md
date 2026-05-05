# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Skill routing

When the user's request matches an available skill, invoke it via the Skill tool. When in doubt, invoke the skill.

Key routing rules:
- Product ideas/brainstorming → invoke /office-hours
- Strategy/scope → invoke /plan-ceo-review
- Architecture → invoke /plan-eng-review
- Design system/plan review → invoke /design-consultation or /plan-design-review
- Full review pipeline → invoke /autoplan
- Bugs/errors → invoke /investigate
- QA/testing site behavior → invoke /qa or /qa-only
- Code review/diff check → invoke /review
- Visual polish → invoke /design-review
- Ship/deploy/PR → invoke /ship or /land-and-deploy
- Save progress → invoke /context-save
- Resume context → invoke /context-restore

## 项目概述

这是一个医药采购比价平台，采用前后端分离架构。前端是 React 应用（helpbuy-clone 目录），后端是基于 RuoYi-Vue 框架的 Java/Spring Boot 服务。

## 常用命令

### 前端开发（helpbuy-clone 目录）
```bash
cd helpbuy-clone
npm run dev          # 启动开发服务器（端口 8585）
npm run build        # 构建生产版本
npm run lint         # ESLint 检查
npm run preview      # 预览生产构建
```

### 后端构建（根目录）
```bash
mvn clean package    # Maven 构建
java -jar ruoyi-admin/target/ruoyi-admin.jar  # 运行后端
```

## 代码架构

### 前端（helpbuy-clone/）
- **技术栈**: React 19 + TypeScript + Vite + Tailwind CSS
- **路由**: react-router-dom v7，路由定义在 `src/App.tsx`
- **状态管理**: Context API（UserContext, PlatformContext）
- **API 层**: `src/api/request.ts` 封装 fetch，`src/api/index.ts` 定义接口，`src/api/types.ts` 定义类型
- **认证**: JWT Token，存储在 localStorage，通过 `useAuth` hook 管理
- **UI 组件**: 自定义组件，使用 Tailwind CSS 样式

### 后端（RuoYi-Vue 模块）
- **ruoyi-admin**: 主应用模块，包含控制器和配置
- **ruoyi-framework**: 框架核心（安全、拦截器等）
- **ruoyi-system**: 系统管理模块（用户、角色、菜单等）
- **ruoyi-common**: 公共工具类和常量
- **ruoyi-quartz**: 定时任务模块
- **ruoyi-generator**: 代码生成器

### Chrome 扩展（Extensions/）
用于从医药采购平台采集数据的浏览器扩展：
- **background.js**: 后台服务，处理消息路由和数据采集
- **bridge.js**: 内容脚本，桥接网页和扩展通信
- **token-monitor.js**: 监控各平台 Token 状态
- **manifest.json**: 扩展配置，支持多个医药平台域名

## 核心业务模块

### API 接口（/yy/ 前缀）
- `/yy/login`, `/yy/register`, `/yy/captcha` - 认证模块
- `/yy/user/*` - 用户中心（个人资料、邀请码、推荐人）
- `/yy/platform/*` - 平台管理（绑定、解绑、Token 同步）
- `/yy/tier/*` - 会员套餐
- `/yy/subscription/*` - 订阅订单
- `/yy/product/*` - 商品搜索和推荐

### 数据类型
- **YyUser**: 用户对象，包含会员等级（0普通/1黄金/2铂金/3钻石）、余额、邀请码等
- **PlatformItem**: 平台对象，包含平台配置和 API 定义
- **PlatformApi**: 平台 API 配置，支持数据加密、签名脚本等
- **TierItem**: 会员套餐，包含价格、权益、限购等

## 开发注意事项

### 前端
- API 基础地址在 `.env` 文件配置（`VITE_API_BASE_URL`）
- 使用 `src/api/request.ts` 的封装方法，自动处理 Token 和错误
- 套餐列表接口有 5 分钟内存缓存机制
- 路由守卫通过 `RequireAuth` 和 `RequireUnauth` 组件实现

### 后端
- 基于 Spring Boot 4.x，需要 JDK 17+
- 使用 MyBatis + PageHelper 进行数据访问
- JWT 认证，Token 有效期在配置文件中设置
- 支付回调接口无需 Token（@Anonymous 注解）

### Chrome 扩展
- Manifest V3 架构
- 使用 offscreen 文档进行后台数据采集
- 支持多个医药平台的数据抓取和 Token 管理
- 扩展与网页通过 bridge.js 通信

## 环境配置

- **开发环境**: 前端 `http://localhost:8585`，后端默认 8080
- **生产环境**: `http://8.152.160.105:8585/prod-api`
- **数据库**: MySQL（配置在 application.yml）
- **缓存**: Redis（用于 Token 和会话管理）
