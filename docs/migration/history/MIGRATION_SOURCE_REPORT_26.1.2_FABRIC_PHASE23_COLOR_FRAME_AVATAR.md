# Exposure 1.21.1 → Minecraft 26.1.2 Fabric 迁移报告：Phase 23

## 彩色照片校色、相框放置崩溃、自拍杆姿势与复杂皮肤渲染

日期：2026-07-18（Asia/Shanghai）  
工作区：`D:\Codex\Exposure`  
固定业务上游：Exposure 1.21.1 commit `9383e0c9237099947a1e6399fcace5772f496c7d`  
迁移目标：Minecraft 26.1.2、Fabric、Java 25  
上一阶段交付：`phase22-exposure-fabric-26.1.2-1.9.18.jar`  
上一阶段 SHA-256：`6607A393B95AD097CFAFC2B7517FAB5514053907CA6F6689A30059F61C7B366F`

## 1. 最终状态

Phase 23 工程实现与本地受控验证已完成。Group 1 与 Group 2 两个阻塞项均找到可解释、可测试的迁移根因并修复；Group 4 找到 26.1.2 模型层级变化造成的真实 headwear 双变换并修复。Group 3 在最小 Exposure 环境中没有复现“自拍持杆手臂状态丢失”：render-state 提取、模型消费和实际旋转值均正确，主副手/左右利手矩阵通过；因此没有加入无上游依据的模型硬改，新增测试锁定固定上游姿势语义，等待用户实际整合包复测兼容性。

| 项目 | 结果 |
|---|---|
| Group 1 游戏内彩色照片严重偏色 | 已修复两处根因：NativeImage 的 R/B 二次交换；26.1.2 Std140 UBO 对齐导致滤镜 Saturation 读成 0 |
| 位面投影器/外部图片对照 | 未改变存储格式；受控色卡路径与 codec/display 往返保持正确 |
| Group 2 两种 Exposure 相框放置崩溃 | 已修复 HangingEntity 继承同步槽未定义；集成服与 dedicated 连接验证通过 |
| Group 3 自拍杆手臂 | 最小环境的状态链和实际模型值正确，未复现状态丢失；左右手矩阵通过并新增回归测试 |
| Group 4 复杂皮肤 headwear | 已修复 26.1.2 `hat` 成为 `head` 子节点后仍复制父 pose 的双变换 |
| 最终测试 | 15 suites，51 tests，0 failures，0 errors，0 skipped |
| 双 Access Widener | common 与 Fabric 均通过 |
| 完整 build | `BUILD SUCCESSFUL` |
| integrated client | 保存退出正常，无修复后 crash |
| dedicated server | 启动、保存、停止正常 |
| 游戏 JVM 残留 | 0 |
| 新 JAR | `D:\Codex\Exposure\phase23-exposure-fabric-26.1.2-1.9.18.jar` |
| 新 JAR SHA-256 | `3751FB1813F5AAA603423EFD993DAE7EDE9BC8D96044836531CA5F4AC395ACD2` |

## 2. 输入、历史边界与基线

开始前完整读取：

- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE22_CAPTURE_UI_RECIPES.md`
- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE21B_FIRST_PLAY_FIXES.md`
- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE21_CLIENT_BOOT.md`

随后只按问题定向读取 Phase 17 的 RenderTarget、PostChain、ScreenshotReadback 报告，以及 Phase 18 的 Viewfinder、CameraCapture、Avatar/玩家模型记录；没有全量重扫历史阶段。业务语义始终以固定上游 commit 为准，没有从当前移植结果反向推测原设计。

### 2.1 用户四项正式人工反馈

1. 基础拍摄、冲洗、打印及 Phase 22 快门卡顿已正常，但游戏内彩色照片严重偏色；外部导入未见同样现象。
2. Exposure 相框放置到墙面时客户端立即崩溃。
3. 自拍 Viewfinder 中持自拍杆的手臂看似下垂，不随自拍相机方向运动。
4. `skin\test skin.png` 的刘海、角等头部外层在自拍 Viewfinder 中错位。

### 2.2 工具链与非 clean 基线

- Java：`C:\Program Files\Java\jdk-25.0.3`，版本 25.0.3
- Gradle Wrapper：`D:\Codex\Exposure\gradlew.bat`，Gradle 9.4.0
- ASCII Gradle 缓存：`GRADLE_USER_HOME=D:\Codex\Exposure.gradle-home`
- 未修改用户全局 Java、Gradle、启动器或系统环境
- 全阶段未执行 `clean`
- 工作区不是 Git 仓库；本报告不使用 Git diff、commit、branch 或回滚提交表述

修改前执行：

```text
:common:compileJava
:fabric:compileJava
:common:compileTestJava
:common:test
:common:validateAccessWidener
:fabric:validateAccessWidener
```

使用 `--rerun-tasks`，结果为 `BUILD SUCCESSFUL in 1m 34s`；11 suites、43 tests、0 failures/errors/skips，双 Access Widener 通过，执行前游戏 JVM 为 0。证据：

- `phase23-baseline.log`
- `phase23-baseline-summary.txt`
- `phase23-environment.log`
- `phase23-prechange-source-resource-manifest.csv`（999 entries）
- `phase23-prechange-source-resource-snapshot.zip`

所有新证据均使用 `phase23-` 前缀，没有覆盖 Phase 22 或更早证据。Phase 22 JAR 在最终复核时仍保持原 SHA-256。

## 3. Group 1：游戏内彩色照片严重偏色

## 3.1 固定受控场景

建立了固定色卡世界与一张精确 sRGB 色卡。场景包含红、绿、蓝、青、品红、黄、白、50% 灰、黑，以及草、天空、村民/肤色、木材和石材等自然材质。固定条件：

- 正午、晴天
- FOV 70
- 854×480 窗口/viewport
- 固定相机位置、朝向与构图
- 默认彩色胶卷、默认曝光、默认镜头、无滤镜作为主诊断组
- direct 与 background 分别测试
- 手持相机与 Camera Stand 分别测试
- 黑白胶卷作为控制组
- Shader 组使用 Iris 1.11.2 + Sodium 0.9.1 + BSL 8.4.01.2

受控文件：

- `phase23-g1-known-srgb-chart.png`
- `phase23-g1-known-srgb-chart.bmp`
- `phase23-g1-color-scene-world-snapshot.zip`
- `phase23-g1-color-scene-reference-no-shader-pre-fix.png`
- `phase23-g1-color-scene-reference-no-shader-post-fix.png`
- `phase23-g1-color-scene-reference-iris-bsl-post-fix.png`

临时 Iris/Sodium/BSL、阶段诊断 PNG 写出与计时钩子只用于开发运行，最终全部从运行 mods、生产源码和最终 JAR 中移除。

## 3.2 固定上游处理顺序

固定 1.21.1 上游的核心顺序为：截图/输入图像 → Image 适配器返回 ARGB → crop 与 Exposure effects → 胶片风格/色彩与 palette → ExposureData 编码 → 解码 → 照片纹理显示。

上游 `WrappedNativeImage#getPixelARGB` 对旧版 `NativeImage#getPixelRGBA` 做一次 `ABGRtoARGB`。Minecraft 26.1.2 的 API 已变为 `NativeImage#getPixel()`，它在返回值边界已经把内部 ABGR 存储转换成 ARGB。迁移代码继续额外执行 `ABGRtoARGB`，等于第二次交换 R/B。

定向上游参考：

- [WrappedNativeImage.java at 9383e0c](https://github.com/Mortuusars/Exposure/blob/9383e0c9237099947a1e6399fcace5772f496c7d/common/src/main/java/io/github/mortuusars/exposure/client/image/WrappedNativeImage.java)
- [CameraPoses.java at 9383e0c](https://github.com/Mortuusars/Exposure/blob/9383e0c9237099947a1e6399fcace5772f496c7d/common/src/main/java/io/github/mortuusars/exposure/client/animation/CameraPoses.java)
- [PhotographFrameEntity.java at 9383e0c](https://github.com/Mortuusars/Exposure/blob/9383e0c9237099947a1e6399fcace5772f496c7d/common/src/main/java/io/github/mortuusars/exposure/world/entity/PhotographFrameEntity.java)

本阶段只修正格式和 UBO 布局边界，没有删除或中性化固定上游的胶片饱和度、对比度、palette、滤镜或艺术处理。

## 3.3 逐阶段像素与偏色首次出现位置

无 Shader 的 direct/background 得到相同结论。下表为色块中心 ROI 算术均值；alpha 为 255，没有预乘异常。

| 样本 | ScreenshotReadback NativeImage RGBA | 修复前生产 ARGB 解释 | 修复后生产 ARGB 解释 |
|---|---:|---:|---:|
| 红 | (114.491, 27.994, 28.568, 255) | (28.568, 27.994, 114.491, 255) | (114.491, 27.994, 28.568, 255) |
| 绿 | (59.959, 74.355, 31.367, 255) | (31.367, 74.355, 59.959, 255) | (59.959, 74.355, 31.367, 255) |
| 蓝 | (37.071, 38.769, 115.917, 255) | (115.917, 38.769, 37.071, 255) | (37.071, 38.769, 115.917, 255) |
| 白 | (165.982, 171.071, 172.183, 255) | (172.183, 171.071, 165.982, 255) | (165.982, 171.071, 172.183, 255) |
| 灰 | (40.385, 30.320, 76.426, 255) | (76.426, 30.320, 40.385, 255) | (40.385, 30.320, 76.426, 255) |
| 黑 | (8.320, 10.189, 14.497, 255) | (14.497, 10.189, 8.320, 255) | (8.320, 10.189, 14.497, 255) |

精确 sRGB 色卡的中心值为：红 `(255,0,0)`、绿 `(0,255,0)`、蓝 `(0,0,255)`、白 `(255,255,255)`、灰 `(128,128,128)`、黑 `(0,0,0)`。修复前 PNG 经 NativeImage 解码后这些值仍完全正确，但生产 Image 适配器把红变蓝、蓝变红；修复后 readback 与生产 ARGB 逐像素完全相同，different pixels = 0，max channel error = 0。

结论：严重偏色第一次出现于 `NativeImage` → Exposure `Image#getPixelARGB` 的格式解释边界，不在 RenderTarget、不在 GPU readback、不在垂直翻转、不在 crop/effects、不在 palette、不在 ExposureData codec，也不在最终照片纹理。

完整数据：

- `phase23-g1-pre-fix-pixel-summary.md`
- `phase23-g1-pre-fix-pixel-rois.csv`
- `phase23-g1-post-fix-pixel-summary.md`
- `phase23-g1-post-fix-pixel-rois.csv`

## 3.4 位面投影器/外部输入对照

同一 sRGB 色卡保存为内容一致的 PNG 与 BMP。诊断分别覆盖 NativeImage PNG 解码边界和位面投影器使用的 BufferedImage 输入分支：

- 源文件中心色块精确；
- PNG NativeImage 原始解码值精确；
- 修复前只有再次执行 ABGR→ARGB 的 NativeImage 适配器分支交换 R/B；
- BufferedImage 分支到 effects 前保持精确，这解释了用户观察到的位面投影器导入并未发生同样偏色；
- 修复后 PNG NativeImage 分支也与源图逐像素一致；
- palette 之后的输出保留 Exposure 原有艺术处理，ExposureData codec roundtrip 与最终 display 保留收到的同一分支，没有额外交换或 gamma 转换。

`ResourceImage#getPixelARGB` 同步改为使用 26.1.2 已返回 ARGB 的 `NativeImage#getPixel()`，避免将来资源 PNG 走到相同二次交换。没有合并或改写两条导入路径，没有改变 ExposureData 格式。

## 3.5 第二个颜色问题：26.1.2 PostChain UBO 对齐

通道修复后的矩阵测试又发现所有明显彩色滤镜变成灰度。根因不是胶片参数，而是 26.1.2 PostChain 使用 Std140 UBO：`vec3` 槽按 16 字节对齐，但迁移后的 GLSL block 将 `vec3 Mul`、`vec3 Add` 和标量连续声明，CPU 上传布局中的 padding 被 shader 当成 `Saturation`，实际读值为 0。

修复为：

```glsl
layout(std140) uniform ColorConfig {
    vec3 Mul;
    float _PaddingAfterMul;
    vec3 Add;
    float _PaddingAfterAdd;
    float Saturation;
    float Contrast;
};
```

这只恢复旧 PostChain 参数的实际槽位，没有调整任何滤镜值。正确旧资源 ID 为 `exposure:shaders/post/red_filter.json`；一次先用错误 ID `exposure:red_pane` 的失败运行已保留在证据中，随后按真实资源修正。

## 3.6 Shader 与模式矩阵

| 环境/模式 | 结果 |
|---|---|
| 无 Shader，direct | readback 与生产 ARGB 相同；彩色成片通道正确 |
| 无 Shader，background | readback 与生产 ARGB 相同；彩色成片通道正确 |
| Iris + BSL，direct | readback 与生产 ARGB 逐像素相同，max error 0 |
| Iris + BSL，background | readback 与生产 ARGB 逐像素相同，max error 0 |
| 手持相机 | 通过 |
| Camera Stand | 通过 |
| 普通彩色胶卷 | 通过 |
| 高感彩色胶卷 | 通过 |
| 无滤镜 | 通过 |
| red filter | 颜色效果恢复，不再因 Saturation=0 变灰 |
| crisp/明显滤镜 | 通过 |
| 黑白胶卷控制 | 无新染色或额外亮度错误 |

BSL 自身 tone mapping 会改变色卡场景的绝对数值，例如 direct 蓝色 ROI 为 `(58.645,69.408,147.053)`，但同一帧在 readback 与生产 ARGB 边界完全一致；没有加入 BSL 特判、白平衡或固定 RGB 矩阵。完整 Shader 数据在：

- `phase23-g1-iris-post-fix-pixel-summary.md`
- `phase23-g1-iris-post-fix-pixel-rois.csv`
- `phase23-g1-iris-post-direct-final.png`
- `phase23-g1-iris-bsl-post-background-final.png`

## 3.7 性能

临时计时从 capture task armed 到异步 Screenshot callback，在 Image 包装与诊断写盘前停止，因此没有把 PNG I/O 算入业务耗时。

| 模式 | 稳定第 2–6 次耗时 | 稳定范围 |
|---|---|---|
| background | 12.582, 12.563, 11.552, 12.656, 11.959 ms | 11.552–12.656 ms |
| direct | 14.632, 13.158, 12.095, 13.666, 11.732 ms | 11.732–14.632 ms |

Phase 22 相同 viewport 稳定参考为 background 12.58–17.37 ms、direct 20.93 ms。Phase 23 没有恢复同步等待、嵌套 `renderLevel`、屏幕裁剪、`glReadPixels` 或 Iris 私有 framebuffer；生产安全扫描确认这些调用没有进入生产源码。颜色修复反而移除了一个逐像素无意义的通道交换。

证据：`phase23-g1-capture-performance-summary.md` 及 timing logs。

## 3.8 Group 1 自动回归

- `WrappedNativeImageTests`：用真实 NativeImage 锁定 R/G/B/C/M/Y/白/50% 灰/黑和 alpha 的 ARGB 通道。
- `Phase23ShaderResourceTests`：锁定 Std140 padding 与字段顺序，并锁定生产滤镜资源仍提供上游语义的饱和度/对比度值。

## 4. Group 2：Exposure 相框墙面放置崩溃

## 4.1 准确 item ID 与首次根异常

确认两个物品：

- `exposure:photograph_frame`
- `exposure:glass_photograph_frame`

在 integrated server 首次放置普通 Photograph Frame 即稳定崩溃。最早且能够解释客户端终止的异常为：

```text
java.lang.IllegalStateException:
PhotographFrameEntity has not defined synched data value 8
```

证据：

- `phase23-g2-integrated-first-crash-crash-2026-07-18_16.13.33-client.txt`
- `phase23-g2-integrated-first-crash-latest.log`
- `phase23-g2-integrated-first-crash-debug.log`

该异常发生在服务端实体设置 attachment direction 并同步的阶段，早于自定义 Photograph renderer、ItemFrame render-state extraction 和模型提交。后续 renderer shutdown、连接断开等只是连锁症状。

## 4.2 根因与修复

Minecraft 26.1.2 的 `HangingEntity` 自身拥有同步的 `DATA_DIRECTION`。迁移后的 `PhotographFrameEntity#defineSynchedData` 只定义 Exposure 自有字段，未调用父实现；父类随后访问 direction accessor 时找不到 slot 8。

修复：

1. 在自有字段前调用 `super.defineSynchedData(builder)`；
2. 在 `onSyncedDataUpdated` 开头调用 `super.onSyncedDataUpdated(key)`，让父类消费 direction 更新并保持 bounding box/attachment 生命周期。

Phase 21 迁移的 `ItemFrameRendererMixin` 和 `ItemFrameRenderStateMixin` target、descriptor 与客户端加载不是本次崩溃原因；没有降低 Mixin require、禁用 renderer、吞异常或改用原版 ItemFrame。

## 4.3 运行验证

integrated server 与连接 dedicated server 的客户端分别验证：

- Photograph Frame 与 Glass Photograph Frame
- 空框墙面放置
- 四个水平朝向
- 允许的地面/顶面
- 插入照片
- 旋转照片
- 取下照片
- 破坏并返回物品
- 重新放置
- 保存退出并重新进入世界
- 整个客户端退出后再次启动并加载

结果：不再崩溃；未观察到重复实体、吞物品、照片复制、错误朝向、紫黑纹理或重载消失。dedicated 环境没有客户端类误加载。最终 online-mode 已恢复为 `true`。

代表证据：

- `phase23-g2-post-fix-integrated-latest.log`
- `phase23-g2-post-fix-integrated-debug.log`
- `phase23-g2-fourth-horizontal-glass-frame.png`
- `phase23-g2-full-client-restart-runClient.log`
- `phase23-g2-dedicated-controlled.log`

## 4.4 Group 2 自动回归

`PhotographFrameEntitySynchedDataTests` 共 2 项：确认继承的 direction accessor 被定义，并确认 direction 更新进入父类消费链。Group 2 gate 当时为 14 suites、48 tests、0 failures/errors/skips。

## 5. Group 3：自拍杆手臂姿势

## 5.1 固定上游调用链

定向检查固定上游 selfie camera、当前 Avatar/Humanoid render-state、HumanoidModel、PlayerItemInHandLayer 与相关 Mixin。26.1.2 当前链路为：

```text
HumanoidMobRendererMixin render-state extraction
    → CameraPoseRenderState（SELFIE、持杆手、cameraEntity、相机 yaw/pitch）
    → HumanoidModelMixin setupAnim
    → CameraPoses.applySelfie
    → 对应 rightArm/leftArm xRot、yRot、zRot
```

`applySelfie` 的业务公式仍与固定上游一致：持杆手由手臂枚举选择；`xRot` 来自 head pitch 并减 π/2，`yRot` 跟随 head yaw，Camera Stand/相机实体时增加左右侧偏移，`zRot` 随抬头/低头方向变化。它不在每帧从全局 `Minecraft.camera` 硬读模型字段。

## 5.2 诊断结论

受控场景为默认皮肤、无盔甲、自拍杆，分别测试水平转动、抬头、低头、慢速/快速移动、站立、潜行、行走，以及正常第三人称、自拍 Viewfinder 和捕获场景。

临时开发日志在 extraction、`PlayerModel` apply 和 setup 返回点记录真实模型值，结论为：

- SELFIE 状态被正确写入 render state；
- right/main、left/main、left/offhand、right/offhand 均选中正确手臂；
- 中性持杆臂 `xRot` 约为 `-1.571 rad`；移动 pitch 后实际变化到例如 `-2.4818583`、`-1.790`；
- yaw/roll 随视角变化；
- setup 返回后没有被 PlayerModel 或 item layer 重置；
- 非持杆手保持普通姿势；退出自拍后普通 pose 恢复。

屏幕上朝向镜头的自拍杆与手臂存在明显透视缩短，长条物体容易被看成“向下”，但模型数值和受控拖动截图不支持“render state 丢失、手臂一直为 vanilla 下垂”这个根因。为避免破坏攻击、游泳、普通持物、Camera Stand 或其他玩家，未加入无证据的额外角度、全局 camera 读取或强制 pose。

运行矩阵截图：

- `phase23-g3-selfie-neutral-pose.png`
- `phase23-g3-left-mainhand-selfie.png`
- `phase23-g3-left-setting-offhand-selfie.png`
- `phase23-g3-right-setting-offhand-selfie.png`

临时 `[Phase23Pose...]` 日志代码已全部移除；生产源码与最终 JAR 扫描均未发现该标记。用户实际整合包若仍表现为手臂下垂，应以 Phase 23 JAR 复测并保留该环境的 mod list 与首帧截图；下一步唯一合理范围是第三方 player model/layer 在 `setupAnim` 之后的覆盖，而不是重写 Exposure 上游姿势。

## 5.3 Group 3 自动回归

`CameraPosesTests` 覆盖 right/left 自拍公式、camera entity 偏移、另一只手不被修改，防止未来 render-state 迁移再次造成持杆手选择或旋转丢失。

## 6. Group 4：复杂皮肤头部外层错位

## 6.1 `test skin.png` 只读检查与安全加载

原文件：`D:\Codex\Exposure\skin\test skin.png`

- 尺寸：64×64
- 格式：RGBA
- 大小：2,273 bytes
- SHA-256：`93DB2A757F153C990CC9CFF58BD6CF3848F6DFF76D3259C14230C56DA37F6C4C`
- alpha：2,121 transparent、0 translucent、1,975 opaque pixels
- head base 区域 `(0,0)-(32,16)`：384 opaque pixels
- head outer 区域 `(32,0)-(64,16)`：114 opaque pixels

PNG 不带 classic/slim 模型元数据；其手臂列填充更符合 classic/wide 受控路径。红色角/刘海图案属于 vanilla outer-layer 像素，本身不是 Ears 或 3D Skin Layers 几何。

安全测试方法：只在 `fabric\run\resourcepacks\phase23-test-skin` 创建临时开发资源包，覆盖开发 profile 的 vanilla wide/slim 默认皮肤，未修改用户账户、用户名、UUID 或正式皮肤。测试后通过 UI 禁用，恢复：

```text
resourcePacks:["vanilla"]
incompatibleResourcePacks:[]
mainHand:"right"
```

随后删除临时资源包。最终源码/JAR 不含测试皮肤、本机路径、用户名或 UUID；原始 `test skin.png` 的最终哈希与只读检查时相同。

## 6.2 真正迁移根因

通过 26.1.2 `HumanoidModel` 字节码/模型树定向确认：

- 旧业务代码把 `model.head` pose 显式复制到 `model.hat`；
- 26.1.2 中 `hat` 已是 `head.getChild("hat")`；
- 子节点天然继承 parent transform；
- `CameraPoses.applyHolding`、`applyDisassembled` 和 `applyStand` 继续 `hat.loadPose(head.storePose())`，导致父旋转一次、子 local pose 又复制一次，即 headwear 双变换；
- 默认简单皮肤不明显，刘海、角和高对比外层更容易暴露漂移。

修复为移除三处父 pose → child hat 的复制，让 hat 保持零 local pose 并通过模型层级自然继承 head。没有关闭 outer layer、没有针对测试皮肤坐标做特判，也没有实现或依赖第三方皮肤模组。

## 6.3 运行验证与第三方边界

普通第三人称和自拍 Viewfinder 中分别检查正面、侧面、抬头、低头、转头；最小 vanilla/Fabric 环境中 base 与 outer layer 保持贴合，修复后不会重复应用 head pose。代表截图：

- `phase23-g4-test-skin-third-person.png`
- `phase23-g4-test-skin-viewfinder-selfie.png`

本地最小环境没有安装 3D Skin Layers、Ears 或其他把 outer pixels 转成几何体的模组，因此没有声称验证某个未安装模组的私有 layer。若用户所说的“恶魔角”在正式整合包中是第三方几何，Phase 23 的标准 parent/child 修复为其提供正确 vanilla head transform；剩余兼容问题需用用户实际模组识别具体 layer 来源，不能安全修成 Exposure 对单个模组的硬依赖。

## 6.4 Group 4 自动回归

`CameraPosesTests` 还锁定：

- 26.1.2 `hat == head.getChild("hat")`；
- holding、disassembled、stand 后 hat local x/y/z rotation 均保持零；
- head 的业务 pose 仍正常改变。

## 7. 精确修改文件

生产源码/资源共 5 个：

1. `common/src/main/java/io/github/mortuusars/exposure/client/image/WrappedNativeImage.java`
   - 移除 26.1.2 下重复的 ABGR→ARGB。
2. `common/src/main/java/io/github/mortuusars/exposure/client/image/ResourceImage.java`
   - 同步恢复 NativeImage 资源输入的 ARGB 解释。
3. `common/src/main/resources/assets/minecraft/shaders/program/exposure_color_processor.fsh`
   - 为两个 Std140 `vec3` 补齐显式 padding。
4. `common/src/main/java/io/github/mortuusars/exposure/world/entity/PhotographFrameEntity.java`
   - 恢复 HangingEntity 同步数据定义与更新生命周期。
5. `common/src/main/java/io/github/mortuusars/exposure/client/animation/CameraPoses.java`
   - 移除 26.1.2 child hat 的重复 parent pose。

新增自动测试共 4 个文件、8 项测试：

6. `common/src/test/java/io/github/mortuusars/exposure/client/image/WrappedNativeImageTests.java`
7. `common/src/test/java/io/github/mortuusars/exposure/client/util/Phase23ShaderResourceTests.java`
8. `common/src/test/java/io/github/mortuusars/exposure/world/entity/PhotographFrameEntitySynchedDataTests.java`
9. `common/src/test/java/io/github/mortuusars/exposure/client/animation/CameraPosesTests.java`

每个文件的大小与 SHA-256 见 `phase23-final-modified-files.txt`。开发用 `phase23-*.py`、受控 server 脚本、PNG 与日志均是工作区证据，不是生产资源，也未打入 JAR。

## 8. 最终构建、客户端与服务器

## 8.1 完整非 clean 验证

最终执行：

```text
:common:compileJava
:fabric:compileJava
:common:compileTestJava
:common:test
:common:validateAccessWidener
:fabric:validateAccessWidener
:common:build
:fabric:build
```

结果：`BUILD SUCCESSFUL in 7s`，15 suites、51 tests、0 failures、0 errors、0 skipped；common/Fabric compileJava、common compileTestJava、双 Access Widener 与完整 build 全部通过。没有禁用、排除或跳过测试。

证据：

- `phase23-final-full-validation.log`
- `phase23-final-test-summary.txt`

## 8.2 Integrated client

最终 current-source runClient 在所有临时诊断代码和测试皮肤资源包移除后执行。进入 integrated world，检查自拍左右/主副手，保存退出世界，再完整退出客户端。日志记录各维度保存与 `[Render thread/INFO] Stopping!`。仅有开发 profile 预期的 Mojang/Realms 401，不是 Exposure 崩溃，也不影响本地世界验证。

证据：

- `phase23-final-integrated-latest.log`
- `phase23-final-integrated-debug.log`
- `phase23-final-integrated-runclient.out.log`
- `phase23-final-integrated-runclient.err.log`（0 bytes）

修复后的运行没有新 crash report；保留的最新 crash 是 Group 2 修复前的首个根异常。

## 8.3 Dedicated server

最终 Java 25、ASCII Gradle home、`online-mode=true` 的短时 dedicated server：

- `Done (0.364s)!`
- 正常接受 stop
- `Stopping server`
- `Saving players`
- `Saving worlds`
- overworld/the_end/the_nether 均保存
- Gradle `BUILD SUCCESSFUL in 36s`

证据：

- `phase23-final-dedicated-server.log`
- `phase23-final-dedicated-latest.log`
- `phase23-final-dedicated-debug.log`

最终 `KnotClient`、`KnotServer`、`dev-launch-injector` 游戏 JVM 残留为 0，见 `phase23-final-process-audit.txt`。

## 9. 新 JAR

```text
Path:          D:\Codex\Exposure\phase23-exposure-fabric-26.1.2-1.9.18.jar
Size:          2,055,354 bytes
Modified:      2026-07-18 17:22:09 +08:00
SHA-256:       3751FB1813F5AAA603423EFD993DAE7EDE9BC8D96044836531CA5F4AC395ACD2
```

交付件与 `fabric/build/libs/exposure-fabric-26.1.2-1.9.18.jar` SHA-256 完全相同。没有覆盖 Phase 22 JAR，也没有安装到用户正式实例。

`phase23-final-jar-content-scan.txt` 确认 JAR 不含：

- `D:\Codex\Exposure` 绝对路径
- `test skin.png`
- `Phase23Pose` 临时诊断
- `phase23-test-skin` 临时资源包
- 色卡 PNG
- 受控 server 脚本

## 10. 失败尝试与处理

1. 首次 prechange manifest 使用了 Windows PowerShell/.NET Framework 不支持的 `Path.GetRelativePath`，未加载 ZipFile assembly；检测后只重建无效 Phase 23 证据。第二次发现漏掉四个 Wrapper 文件，再生成最终 999-entry manifest。没有修改业务源码。
2. 首次滤镜矩阵使用错误 ID `exposure:red_pane`，得到“Could not find post chain”；随后按真实资源 `exposure:shaders/post/red_filter.json` 重试。该失败帮助区分资源 ID 与 UBO 对齐问题。
3. Iris 像素分析直接调用系统 `python` 得到 exit 9009；改用 Codex bundled Python 后成功，未修改数据。
4. 首版临时测试皮肤 resource pack 在 pack format >64 时遗漏 `min_format/max_format`，客户端拒绝；补齐 84.0 元数据后加载。最终资源包已禁用并删除。
5. dedicated 客户端首次在线开发认证受无效 dev session 影响；受控本地连接验证使用临时离线设置，测试结束立即恢复 `online-mode=true`，最终 dedicated 验证再次在 true 下干净启动/停止。
6. Group 3 曾加入临时逐阶段 pose 日志确认是否存在后置重置；证据证明没有重置后，全部诊断代码移除，没有将猜测性姿势改动留在生产。

## 11. 明确未修改边界

- 未修改任何业务 packet ID、方向、字段顺序或 StreamCodec。
- 未激活 `query_exposure_data`。
- 未改变 Phase 20 menu open-data envelope。
- 未改变 SavedData、ExtraData、Frame、ExposureData、CameraId、照片文件格式或服务器照片仓库协议。
- 未改变胶片艺术参数、palette 内容、滤镜业务值或通用图片存储格式。
- 未回退 Phase 22 的完整正常帧重定向、独占 RenderTarget、GUI 前异步 readback、direct/background 时序和资源所有权。
- 未加入同步 `glReadPixels`、额外 `renderLevel`、桌面截图/屏幕裁剪或 Iris 私有 framebuffer 读取。
- 未禁用 renderer、降低 Mixin require、禁止相框墙面放置、关闭 outer layer、隐藏玩家或吞异常。
- 未把 `test skin.png`、用户名、UUID、本机路径或自动测试命令写入生产资源或 JAR。
- 未修改用户正式账户皮肤，未安装新 JAR 到用户正式实例。

生产源码安全扫描见 `phase23-final-production-safety-scan.txt`。

## 12. 下一轮人工复测清单

请用户把 Phase 23 JAR 放入正式测试实例后，在实际 Shader Pack 和正式客户端模组组合中复测：

1. 同一自然场景拍普通彩色、高感彩色、黑白照片；无 Shader/Iris+实际 Shader 各做 direct/background。
2. 手持相机与 Camera Stand 各拍一次；无滤镜、red/crisp 或一个明显滤镜各拍一次。
3. 用位面投影器重新导入已知 sRGB PNG，确认导入、ExposureData 和最终显示仍无通道交换。
4. `exposure:photograph_frame` 与 `exposure:glass_photograph_frame`：四水平朝向、空框、装片、旋转、重载、取片、破坏。
5. 自拍杆：右利手/左利手、主手/副手，缓慢与快速上下左右移动；对比 Viewfinder 与最终成片。
6. `test skin.png`：普通第三人称与自拍 Viewfinder 的正面、侧面、抬头、低头、转头；若角是 3D Skin Layers/Ears 等几何，请同时记录实际模组名与版本。

若 Group 3 在用户整合包仍出现真正的手臂下垂，请提供：完整 mod list、对应 latest/debug log、普通第三人称与自拍同一时刻截图、主副手/左右利手设置。下一轮应只追踪在 Exposure `HumanoidModelMixin` 之后覆盖 arm pose 的具体第三方 layer/model，而不是改写已经验证正确的上游公式。

## 13. 状态交接

### 已完成

- 保存 Phase 23 修改前 manifest、快照、环境与基线。
- 定量定位并修复游戏 capture 的 R/B 二次交换。
- 定位并修复 26.1.2 PostChain Std140 滤镜 UBO 对齐。
- 证明位面投影/通用存储、codec 与 display 不是严重偏色的首次来源。
- 保存相框首次根异常，修复 HangingEntity 同步数据继承。
- 完成两种相框、四朝向、装片/旋转/重载/破坏、integrated/dedicated 验证。
- 验证自拍 render-state → model pose 链及左右手矩阵，新增姿势语义测试。
- 只读分析并安全加载测试皮肤，修复 headwear child 双变换。
- 移除全部临时诊断、皮肤资源包和第三方 Shader 测试文件。
- 51/51 测试、双 Access Widener、完整 build、客户端与 dedicated server 通过。
- 生成并校验 Phase 23 JAR。

### 关键决策

- 只修正确格式/布局/模型层级边界，不用主观调色矩阵覆盖根因。
- 保留固定上游胶片艺术风格和 Phase 22 capture 架构。
- 对 Group 3 以实际 render-state 和模型旋转证据为准，不加入不能被最小复现支持的角度补丁。
- 对复杂皮肤只恢复 vanilla parent/child 语义，不引入第三方皮肤模组硬依赖。

### 遗留问题

- 工程与最小环境中没有遗留阻塞性 crash 或严重彩色通道错误。
- Group 3 的用户整合包原现象没有在最小环境复现，仍需要用户以新 JAR 做正式兼容复测。
- 未安装的 3D Skin Layers/Ears 类几何层无法在本地宣称已逐模组验证；若仍错位，需要按实际 mod/version 定向检查。

### 下一步

默认下一步由用户使用 `phase23-exposure-fabric-26.1.2-1.9.18.jar`，在实际 Shader Pack 与正式客户端模组环境中复测彩色照片、两种相框、自拍杆姿势和 `test skin.png`。若有剩余问题，以本报告保存的像素阶段、首异常和 render-state 边界作为唯一继续范围。
