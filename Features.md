# Jenkins Mobile 功能设计文档

## 项目概述

Jenkins Mobile 是一个跨平台的 Jenkins 移动客户端，支持 iOS 和 Android 平台。使用现代化的 UI 框架（SwiftUI 和 Jetpack Compose）构建，提供原生的用户体验。

## 技术栈

| 平台 | UI 框架 | 语言 | 架构模式 |
|-----|---------|------|---------|
| iOS | SwiftUI | Swift | MVVM |
| Android | Jetpack Compose | Kotlin | MVVM |

## 核心功能

### 1. 服务器配置与登录

#### 功能描述
- 支持配置 Jenkins 服务器地址
- 支持用户名 + API Token 认证
- 自动保存登录状态，下次打开自动登录
- 支持退出登录

#### 技术实现
- **iOS**: 使用 `UserDefaults` 持久化存储
- **Android**: 使用 `DataStore` 持久化存储
- **认证方式**: HTTP Basic Auth (Base64 编码)

#### 文件位置
- iOS: `ios/ios/Views/LoginView.swift`, `ios/ios/Services/StorageService.swift`
- Android: `android/.../ui/login/LoginScreen.kt`, `android/.../data/repository/JenkinsRepository.kt`

---

### 2. Dashboard（任务列表）

#### 功能描述
- 显示 Jenkins 服务器的所有 Views（视图）
- 支持切换不同 View 查看对应的任务列表
- 显示每个任务的状态（成功/失败/不稳定/构建中等）
- 显示任务健康度图标
- 支持下拉刷新
- 支持快速触发构建（自动检测是否需要参数）

#### 任务状态
| 状态 | 颜色 | 说明 |
|-----|------|-----|
| SUCCESS | 绿色 | 构建成功 |
| FAILURE | 红色 | 构建失败 |
| UNSTABLE | 黄色 | 构建不稳定 |
| DISABLED | 灰色 | 任务已禁用 |
| ABORTED | 灰色 | 构建已中止 |
| NOT_BUILT | 灰色 | 从未构建 |
| BUILDING | 蓝色+动画 | 正在构建 |

#### 健康度图标
根据任务构建稳定性显示不同天气图标：
- ☀️ 晴天 (81-100%)
- 🌤️ 多云 (61-80%)
- ⛅ 阴天 (41-60%)
- 🌧️ 雨天 (21-40%)
- ⛈️ 雷雨 (0-20%)

#### 文件位置
- iOS: `ios/ios/Views/DashboardView.swift`, `ios/ios/ViewModels/DashboardViewModel.swift`
- Android: `android/.../ui/dashboard/DashboardScreen.kt`, `android/.../ui/dashboard/DashboardViewModel.kt`

---

### 3. 任务详情

#### 功能描述
- 显示任务基本信息（名称、描述）
- 显示快速统计（最新构建、上次成功、上次失败）
- 显示构建历史列表
- 支持触发构建（自动检测是否需要参数）
- 支持下拉刷新

#### 构建历史信息
- 构建编号 (#123)
- 构建状态（成功/失败/构建中等）
- 构建时间（相对时间，如"2小时前"）
- 构建耗时（如"5分32秒"）

#### 文件位置
- iOS: `ios/ios/Views/JobDetailView.swift`, `ios/ios/ViewModels/JobDetailViewModel.swift`
- Android: `android/.../ui/jobdetail/JobDetailScreen.kt`, `android/.../ui/jobdetail/JobDetailViewModel.kt`

---

### 4. 触发构建

#### 功能描述
- 支持无参数构建（直接触发）
- 支持参数化构建（显示参数填写界面）
- 自动获取并缓存 CSRF Token (Jenkins Crumb)
- 支持 Session Cookie 保持（确保 Crumb 有效）

#### 支持的参数类型
| 参数类型 | 控件 | 说明 |
|---------|------|-----|
| String | 文本输入框 | 单行文本 |
| Text | 多行文本框 | 多行文本 |
| Boolean | 开关 | 是/否选择 |
| Choice | 下拉选择 | 从预定义选项中选择 |
| Password | 密码输入框 | 隐藏输入内容 |

#### 技术细节
- 使用 `buildWithParameters` 端点
- CSRF Token 通过 `/crumbIssuer/api/json` 获取
- Crumb Header 名称从响应的 `crumbRequestField` 动态获取
- Android 使用 `CookieJar` 保持 Session

#### 文件位置
- iOS: `ios/ios/Views/BuildParametersView.swift`
- Android: `android/.../ui/components/BuildParametersDialog.kt`

---

### 5. 构建日志查看

#### 功能描述
- 查看构建的完整控制台输出
- 等宽字体显示，保持日志格式
- 深色背景，便于阅读
- 支持文本选择和复制
- 自动滚动到底部
- 支持刷新获取最新日志

#### UI 设计
- 背景色: `#1E1E1E` (深灰色)
- 文字色: `#D4D4D4` (浅灰色)
- 字体: 等宽字体 (Monospace)
- 字号: 12sp/pt

#### 文件位置
- iOS: `ios/ios/Views/BuildLogView.swift`
- Android: `android/.../ui/buildlog/BuildLogScreen.kt`, `android/.../ui/buildlog/BuildLogViewModel.kt`

---

### 6. 设置页面

#### 功能描述
- 显示当前登录的服务器信息
- 显示当前登录的用户名
- 支持退出登录

#### 文件位置
- iOS: `ios/ios/Views/SettingsView.swift`
- Android: `android/.../ui/settings/SettingsScreen.kt`

---

## API 接口

### Jenkins REST API 使用

| 接口 | 方法 | 说明 |
|-----|------|-----|
| `/api/json` | GET | 获取服务器信息和视图列表 |
| `/view/{name}/api/json` | GET | 获取指定视图的任务列表 |
| `/job/{name}/api/json` | GET | 获取任务详情和构建历史 |
| `/job/{name}/buildWithParameters` | POST | 触发构建（带参数） |
| `/job/{name}/{build}/consoleText` | GET | 获取构建日志 |
| `/crumbIssuer/api/json` | GET | 获取 CSRF Token |

### 认证方式
```
Authorization: Basic base64(username:apiToken)
```

### CSRF 保护
```
Jenkins-Crumb: {crumb_value}
```

---

## 数据模型

### Server（服务器配置）
```swift
struct Server {
    var url: String        // 服务器地址
    var username: String   // 用户名
    var apiToken: String   // API Token
}
```

### Job（任务）
```swift
struct Job {
    let name: String              // 任务名称
    let url: String               // 任务 URL
    let color: String?            // 状态颜色
    let lastBuild: BuildReference?
    let lastSuccessfulBuild: BuildReference?
    let lastFailedBuild: BuildReference?
    let buildable: Bool?
    let healthReport: [HealthReport]?
}
```

### Build（构建）
```swift
struct Build {
    let number: Int         // 构建编号
    let url: String         // 构建 URL
    let result: String?     // 构建结果
    let timestamp: Int64?   // 开始时间戳
    let duration: Int64?    // 构建耗时(ms)
    let building: Bool?     // 是否正在构建
}
```

### ParameterDefinition（参数定义）
```swift
struct ParameterDefinition {
    let name: String              // 参数名
    let type: String?             // 参数类型
    let description: String?      // 参数描述
    let defaultParameterValue: ParameterValue?
    let choices: [String]?        // 选项（Choice类型）
}
```

---

## 项目结构

### iOS 项目结构
```
ios/
├── ios/
│   ├── Models/          # 数据模型
│   │   ├── Server.swift
│   │   ├── Job.swift
│   │   ├── Build.swift
│   │   └── JenkinsView.swift
│   ├── Services/        # 服务层
│   │   ├── JenkinsAPI.swift
│   │   └── StorageService.swift
│   ├── ViewModels/      # 视图模型
│   │   ├── LoginViewModel.swift
│   │   ├── DashboardViewModel.swift
│   │   └── JobDetailViewModel.swift
│   ├── Views/           # UI 视图
│   │   ├── LoginView.swift
│   │   ├── DashboardView.swift
│   │   ├── JobDetailView.swift
│   │   ├── BuildLogView.swift
│   │   ├── BuildParametersView.swift
│   │   └── SettingsView.swift
│   └── Components/      # 可复用组件
│       ├── StatusIcon.swift
│       ├── WeatherIcon.swift
│       └── JobRowView.swift
```

### Android 项目结构
```
android/app/src/main/java/com/by/android/
├── data/
│   ├── api/             # API 接口定义
│   │   └── JenkinsApi.kt
│   ├── model/           # 数据模型
│   │   ├── Server.kt
│   │   ├── Job.kt
│   │   ├── Build.kt
│   │   └── JenkinsView.kt
│   └── repository/      # 数据仓库
│       └── JenkinsRepository.kt
├── ui/
│   ├── login/           # 登录
│   ├── dashboard/       # 任务列表
│   ├── jobdetail/       # 任务详情
│   ├── buildlog/        # 构建日志
│   ├── settings/        # 设置
│   ├── components/      # 可复用组件
│   ├── navigation/      # 导航
│   └── theme/           # 主题
└── MainActivity.kt
```

---

## UI/UX 设计原则

1. **iOS 设计语言**: 使用 iOS 原生设计风格，包括导航栏、列表样式、Sheet 等
2. **一致性**: iOS 和 Android 界面保持 90% 以上相似度
3. **响应式**: 支持不同屏幕尺寸
4. **状态反馈**: 加载中显示进度指示器，错误显示友好提示
5. **下拉刷新**: 所有列表支持下拉刷新
6. **深色模式**: 支持系统深色模式（自动跟随系统）

---

## 安全考虑

1. **凭证存储**: 使用平台安全存储机制（iOS Keychain / Android EncryptedSharedPreferences）
2. **CSRF 保护**: 正确处理 Jenkins CSRF Token
3. **Session 管理**: 保持 Session Cookie 以确保 Crumb 有效
4. **敏感信息**: 密码参数使用密码输入框，不显示明文

---

## 未来扩展

- [ ] 构建队列管理
- [ ] 构建参数历史记录
- [ ] 构建通知推送
- [ ] 多服务器管理
- [ ] 构建收藏功能
- [ ] 搜索任务功能
- [ ] 构建统计图表
- [ ] Pipeline 可视化
