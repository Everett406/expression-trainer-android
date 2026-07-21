# 表达训练系统 - 原生 Android 版

基于 Kotlin + Jetpack Compose + Material 3 的中文口语表达训练 Android 应用。

## 功能

- **实时语音识别**：阶跃(stepfun)双向流式 ASR，边说边出字幕
- **词库实时分析**：检测填充词、犹豫词、笼统词，给出替代建议
- **AI 实时反馈**：每说约 30 字触发一次 AI 教练提示
- **完整分析报告**：录制结束后生成 Markdown 格式的深度分析报告
- **自定义训练规则**：可设置个人训练目标、规则、参考风格、口癖补充
- **Material You**：支持动态取色与浅色/深色主题

## 技术栈

- Kotlin 2.0.20
- Jetpack Compose + Material 3
- 阶跃 `stepaudio-2.5-asr-stream` 双向流式 ASR
- OkHttp WebSocket + AudioRecord 音频采集
- DeepSeek / OpenAI 兼容 API 作为 AI 后端
- DataStore Preferences 本地存储

## 开始使用

### 环境要求

- Android Studio Ladybug 或更新版本
- JDK 21
- Android SDK 35

### 安装

```bash
./gradlew assembleRelease
```

### GitHub Actions 自动构建

推送 `v*` 格式的 tag 即可触发自动构建：

```bash
git tag v1.0.6
git push origin v1.0.6
```

构建完成后会在 GitHub Releases 页面生成 APK 下载。

## 配置

首次打开 APP 需要在设置中配置：
1. 阶跃 ASR API Key（在 platform.stepfun.com 获取）
2. AI 后端选择（DeepSeek 或自定义 OpenAI 兼容接口）
3. AI API Key

## 版本历史

- v1.0.6 - 修复 APK 签名：同时启用 V1 + V2 签名，提升安装兼容性；CI 增加 apksigner 验证
- v1.0.5 - 修复 release 签名配置注入
- v1.0.4 - 替换 Settings 中不兼容的 ExposedDropdownMenu 为 RadioButton
- v1.0.3 - 修复 Kotlin 编译错误
- v1.0.2 - 添加 JitPack 仓库以解析 compose-markdown
- v1.0.1 - 修复 Kotlin 2.0 Compose Compiler 插件配置
- v1.0.0 - 初始版本，从 Capacitor 版迁移到原生 Android

## License

MIT
