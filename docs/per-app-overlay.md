# App 选择版构建报告

来源：https://github.com/jev-chat/jev-chat-jarvis
基线提交：cb113d1acbfd14206fa03d185ee766e1fbe73d8e
本地分支：feature/per-app-overlay
版本：1.3-app-select（versionCode 5），包名 com.jev.probe，Android 11+，ARM64。

## 原因与修改
原版 ChatCaptureService 明确在非适配 App 中保留 showIdle 悬浮球，供手动 OCR 使用；Prefs.whitelist 只过滤会话标题。
新增 OverlayAppsActivity：设置顶部进入，可搜索可启动应用名称/包名、多选、仅微信、全部取消，保存立即生效。未保存返回不更改设置。
Prefs.overlayApps 默认仅 com.tencent.mm，空集合代表全部关闭，旧配置键不变。
新增 OverlaySession 对前台变化和异步任务作代次验证；隐藏时取消防抖和清空旧状态；延迟截图发起前与 OCR/网络返回时重查前台。每 500ms 补查前台，锁屏及灭屏隐藏。
OverlayController 在创建/恢复悬浮窗时也检查允许范围。
Manifest 仅查询 launcher intent，不申请 QUERY_ALL_PACKAGES。
构建脚本移除 Windows 默认签名路径，以环境变量指定 release 签名。
APK 内附 LICENSE/NOTICE，设置页标注非官方二次开发来源。

## 自动验证
命令：gradle :app:assembleDebug :app:testDebugUnitTest

```text
> Task :app:assembleDebug
w: Detected multiple Kotlin daemon sessions at kotlin/sessions

BUILD SUCCESSFUL in 24s
42 actionable tasks: 17 executed, 25 up-to-date
```

6 项单元测试，0 失败、0 错误（OverlaySessionTest）：
离开微信、返回微信不复活旧回调、切换已选 App、关闭范围/总开关、普通事件稳定、未知前台/新会话。
APK v2 签名校验通过；zipalign -c -P 16 4 通过。
与此前 1.3 Debug APK 的签名证书 SHA-256 相同，versionCode 从 4 升到 5，可覆盖该 Debug 版本；无法覆盖作者不同签名的 release 版本。

## 实机验收缺口
没有连接手机/模拟器，尚未验证 ColorOS 上实际 App 切换、键盘、锁屏、分屏及应用分身。应用列表仅当前 Android 用户可见的可启动 App，不跨工作资料或分身用户。
已发送的网络请求不会撤回，但过期结果不会再显示；排队任务和延迟截图在开始前会再次检查。

## 操作
设置 → 悬浮窗使用范围 → 选择启用悬浮窗的 App → 勾选 → 保存并生效。
建议手机验证：仅微信 → 微信显示 → 桌面/浏览器隐藏 → 返回微信显示；取消所有 → 都隐藏；添加 QQ → 微信/QQ 显示；分析时切出 → 结果不弹回其他 App。
