# Jenkins Mobile

一个跨平台的 Jenkins 移动客户端，支持 iOS 和 Android 平台。使用原生技术栈开发，iOS 端采用 SwiftUI，Android 端采用 Jetpack Compose。

## 功能特性

- 🔐 **安全登录** - 支持 Jenkins Basic Auth 认证（用户名 + API Token）
- 📋 **任务管理** - 查看所有 Jenkins 任务，按视图分类浏览
- 🚀 **触发构建** - 一键触发任务构建
- 📊 **构建历史** - 查看任务的构建历史和详细信息
- 🔄 **实时刷新** - 下拉刷新获取最新状态
- 🎨 **状态可视化** - 直观的状态图标和健康度指示器

## 截图预览

| 登录页 | 任务列表 | 任务详情 |
|:---:|:---:|:---:|
| Login | Dashboard | Job Detail |

## 技术栈

### iOS
- **UI 框架**: SwiftUI
- **最低版本**: iOS 17.0+
- **网络请求**: URLSession
- **数据持久化**: UserDefaults

### Android
- **UI 框架**: Jetpack Compose
- **最低版本**: Android 7.0 (API 24)+
- **网络请求**: Retrofit + OkHttp
- **数据持久化**: DataStore Preferences
- **导航**: Navigation Compose

## 项目结构

```
JenkinsMobile/
├── ios/                          # iOS 项目
│   └── ios/
│       ├── Models/               # 数据模型
│       ├── Services/             # API 和存储服务
│       ├── ViewModels/           # 视图模型
│       └── Views/                # SwiftUI 视图
│           └── Components/       # 可复用组件
│
├── android/                      # Android 项目
│   └── app/src/main/java/com/by/android/
│       ├── data/
│       │   ├── api/              # Retrofit API 接口
│       │   ├── model/            # 数据模型
│       │   └── repository/       # 数据仓库
│       └── ui/
│           ├── components/       # 可复用 Composable
│           ├── login/            # 登录模块
│           ├── dashboard/        # 仪表盘模块
│           ├── jobdetail/        # 任务详情模块
│           ├── settings/         # 设置模块
│           ├── navigation/       # 导航配置
│           └── theme/            # 主题配置
│
├── Features.md                   # 功能需求文档
└── README.md                     # 项目说明
```

## 快速开始

### 环境要求

**iOS:**
- macOS 13.0+
- Xcode 15.0+
- iOS 17.0+ 设备或模拟器

**Android:**
- Android Studio Hedgehog (2023.1.1)+
- JDK 11+
- Android SDK 24+

### 构建运行

#### iOS

```bash
# 进入 iOS 项目目录
cd ios

# 使用 Xcode 打开项目
open ios.xcodeproj

# 或使用命令行构建
xcodebuild -scheme ios -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 15' build
```

#### Android

```bash
# 进入 Android 项目目录
cd android

# 构建 Debug APK
./gradlew assembleDebug

# APK 输出路径
# android/app/build/outputs/apk/debug/app-debug.apk
```

## 使用说明

### 获取 Jenkins API Token

1. 登录 Jenkins Web 界面
2. 点击右上角用户名 → **设置**
3. 找到 **API Token** 部分
4. 点击 **添加新 Token** → 生成并复制

### 连接服务器

1. 打开 Jenkins Mobile 应用
2. 输入 Jenkins 服务器地址（如 `https://jenkins.example.com`）
3. 输入用户名和 API Token
4. 点击**登录**

## API 接口

应用使用以下 Jenkins REST API：

| 接口 | 说明 |
|------|------|
| `GET /api/json` | 获取服务器信息和视图列表 |
| `GET /view/{name}/api/json` | 获取指定视图的任务列表 |
| `GET /job/{name}/api/json` | 获取任务详情和构建历史 |
| `POST /job/{name}/build` | 触发构建 |
| `GET /job/{name}/{build}/consoleText` | 获取构建日志 |

## 开发计划

- [ ] 参数化构建支持
- [ ] 构建日志查看
- [ ] 推送通知
- [ ] 多服务器管理
- [ ] 构建队列管理
- [ ] 深色模式优化

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 致谢

- [Jenkins](https://www.jenkins.io/) - 持续集成服务器
- [SwiftUI](https://developer.apple.com/xcode/swiftui/) - Apple 声明式 UI 框架
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Android 现代 UI 工具包
