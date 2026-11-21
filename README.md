# 合大课表 (HfuuSchedule)

参照开源项目 **ShiGuangSchedule（拾光课程表）** 的现代化架构，并融合 **明日方舟《众生行迹》(The Masses' Travels)** 平面图录视觉美学，专为**合肥大学**学生打造的 Android 课程表、学习通待办与日程提醒应用。

## 核心功能

1. **课表与合肥大学教务处一键导入**
   - 内置合肥大学常规作息时间（1-10 节及晚间扩展节次），支持左右滑动切周、5 天/7 天模式切换、课表备忘录与手动增删改课程。
   - 内置 WebView + JS 注入解析引擎（`app/src/main/assets/hfuu_jwxt_parser.js`），登录合肥大学教务系统后点击「一键解析并导入当前页课表」，自动识别课表并同步至本地。
2. **接入超星学习通（作业 / 考试 / 签到）**
   - 支持 WebView 登录授权并将 Cookie 通过 Android Keystore (`AES256-GCM`) 加密保存在本机。
   - 自动汇总待交作业、考试安排与进行中的签到，支持按类型筛选、按紧急度/课程排序以及一键唤起学习通 App。
3. **「日程」管理与系统响铃提醒**
   - 独立「日程」页面，支持自定义标题、位置、全天/精确时段、重复规则（永不/每天/每周/每月/每年）、彩色分类标签、实时倒计时显示与备注。
   - 自动联动展示当日课程，支持设置提前提醒并通过 Android `AlarmManager` + 高优先级通知渠道触发系统提示音、横幅弹窗与震动。
   - 支持在「我的」中开启 **启动时直接进入日程**。
4. **双套 UI 自由切换（拾光极简 / 明日方舟·众生行迹）**
   - **拾光极简**：柔和圆角与低饱和度色板，轻盈清爽。
   - **众生行迹 (`THE MASSES' TRAVELS`)**：参考 [Funkierry/the-masses-travels](https://github.com/Funkierry/the-masses-travels) 平面图录风格，采用温润纸面底色、钴蓝/炭墨/绯红配色、竖屏海报蓝图背景板、手绘过冲圆环、套印双线角标与顶栏小苹果能天使挂件。
5. **自定义作息、滚轮学期设置与本地加密隐私保护**
   - 上课时间支持逐节调整、增删节次与一键恢复合大默认作息。
   - 学期简称与完整学期名均支持丝滑滚轮滑动选择，默认 18 周并支持 ±7 天微调开学周次。
   - 所有账号凭证与本地数据均保存在设备本地，不经过任何第三方服务器。

## 技术栈

- **语言**：Kotlin 2.0 + Coroutines / Flow
- **UI**：Jetpack Compose + Material 3 + Navigation Compose
- **本地存储**：Room Database + DataStore Preferences + AndroidX Security Crypto (`EncryptedSharedPreferences`)
- **网络与解析**：OkHttp + Jsoup + kotlinx.serialization + WebView JS Bridge

## 构建与运行

1. 使用 **Android Studio Ladybug (2024.2) 或更高版本** 打开项目根目录。
2. 等待 Gradle Sync 完成（已配置阿里云镜像与腾讯云 Gradle 分发源以加速国内下载）。
3. 连接 Android 8.0+ 真机或模拟器，点击 **Run (`app`)** 即可编译安装。
