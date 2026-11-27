# 智触博物 · 达芬奇

PICO Spatial SDK 6.2.8 的全沉浸达芬奇展厅 Demo。应用直接进入展厅。入口面板显示生平双轨时间线，转身可选择七个阶段展牌。

- **生平时间线**覆盖七阶段十三节点，六幅核心画作挂接在事件旁。
- **真迹细读**支持手势缩放、平移和旋转，以及按钮操作。点击右侧观察热点可跳到对应局部。高倍视图按需读取原图区域。
- **物证比对**支持并置、透明叠加、透明度、轮廓引导和手动位移、缩放、旋转。红粉笔头像的归属与叠加假说保留不确定性。
- **作品瀑布流**收录 20 件作品和版本。六幅核心作品配有预置的观察说明。

展品图像和观察说明在本地运行。当前 Demo 不含 AI 讲解或语音服务。

## 构建与运行

按照 [PICO CLI 安装与初始化指南](https://developer-cn.picoxr.com/document/pico-cli/quickstart-common/) 准备 Node.js 和 PICO CLI。项目需要 JDK 21、Android SDK Platform 35 与 Build Tools 37.0.0。使用 Android Studio 或本机 `local.properties` 设置设备的 Android SDK 路径。

```sh
pico-cli --version
pico-cli doctor
./gradlew :app:assembleDebug
pico-cli emulator list --format json
pico-cli emulator start --avd Pico_Emulator_6_2 --format json
pico-cli app install app/build/outputs/apk/debug/app-debug.apk --replace --device emulator-5554
pico-cli app launch com.interactimuse.spatial --activity .platform.LaunchActivity --device emulator-5554
```

AVD 名称与设备编号以上一步的实际输出为准。六幅作品与比对素材的文件链接和分辨率见 [图源清单](LEONARDO-SOURCES.md)。
