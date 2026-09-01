# 点译 TapTranslate

Android Chrome 点词翻译原型。

## v0.1 使用方式

1. 安装 APK。
2. 打开「点译」，点击「开启无障碍服务」。
3. 在系统无障碍设置中开启「点译 · Chrome 点词翻译」。
4. 回到 Chrome，右侧会出现「译」悬浮球。
5. 点「译」→ 点网页里的英文单词 → 弹出「原词 / 中文」两行卡片。

## 原理

- 不注入 Chrome、不需要 Chrome 扩展。
- 使用 Android AccessibilityNodeInfo 的逐字符位置接口定位点击坐标对应的文字。
- Chromium Android 原生实现了 `EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY`，因此无需 OCR 和截图。
- 翻译暂时通过 Google Dictionary/Translate 的网页接口完成，不需要 API Key，但该接口不是正式公开 API，未来可能变化。

## 隐私边界

- Accessibility Service 配置只接收 `com.android.chrome` 的事件。
- 只有用户主动进入一次性点词模式时才读取当前活动网页的无障碍文字。
- 不截屏。
- 不读取或保存 Chrome 历史、密码、Cookie、账号。
- 待翻译的单词会发送给 Google 翻译服务。

## 兼容性

- minSdk 26（Android 8.0+）；重点针对 Android 15 / Chrome。
- 某些网页元素如果 Chrome 不暴露逐字符坐标，v0.1 会提示未识别到单词。
