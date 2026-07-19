# Exposure 迁移报告：Minecraft 26.1.2 Fabric Phase 18

## 0. 阶段结论

Phase 18 已完成本阶段限定范围内的 Viewfinder、Overlay、controls、zoom/FOV、CameraCapture action、EntitiesInFrame、CameraItem/EnderMan 与两个旧 Access Widener 目标迁移。

- `common` Java 错误：`131 -> 62`，减少 69 条。
- 本阶段所有目标文件的 Java 编译错误均已清零。
- 剩余 62 条全部属于明确留给后续阶段的普通 GUI、Bugger、ResourceImage/NativeImage/export、注册/数据 API 尾项。
- `:fabric:compileJava` 仍被 `:common:compileJava` 阻断，Fabric javac 尚未真正启动。
- common 与 fabric Access Widener 验证均成功。
- 旧 `GameRenderer.getFov(Camera,float,boolean):double` AW 已在全部调用方迁移后删除。
- 旧 `EnderMan.isLookingAtMe(Player):boolean` AW 已替换为 26.1.2 真实私有方法 `EnderMan.isBeingStaredBy(Player):boolean`。
- 未修改 networking、packet schema、SavedData、ExtraData、Frame/ExposureData 格式、照片路径、像素编码、调色板、曝光/胶片算法。
- 未回退 Phase 17 的 ShaderManager ownership、post effect、RenderTarget 或异步 fenced readback。

## 1. 输入资料与基线

开始前读取并以以下资料为优先依据：

- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE17_SHADER_RENDERTARGET_SCREENSHOT.md`
- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE16_HELD_PHOTO_PLAYER_POSE.md`
- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE15_DYNAMIC_PHOTO_RENDERING.md`
- `phase17-common-compile-final.log`
- `phase17-clean-build-final.log`
- `phase17-common-accesswidener.log`
- `phase17-fabric-accesswidener.log`
- `phase17-merged-jar-gpu-architecture.log`
- `phase17-shader-resource-audit.log`

按需追溯了 Phase 10/12 的输入事件与抽取式 GUI 规则。没有全量重读与本阶段无关的历史报告。

基线命令：

```text
./gradlew :common:compileJava
```

完整日志：`phase18-common-baseline.log`。

基线 131 条错误按文件归档如下：

| 类别/文件 | 数量 |
|---|---:|
| ViewfinderOverlay | 33 |
| ViewfinderCameraControlsScreen | 22 |
| ViewfinderZoom | 3 |
| Viewfinder | 2 |
| CameraClient | 1 |
| FocalLengthButton | 1 |
| CameraItem/EnderMan | 1 |
| EntitiesInFrame | 1 |
| CaptureTemplate | 1 |
| GammaModifier | 2 |
| Flash | 2 |
| 普通 GUI、GuiUtil、Photograph/Lightroom/Test screens | 27 |
| Bugger 与 Bugger mixin | 13 |
| ResourceImage/NativeImage/File/URL/export | 13 |
| ColorPalettes、Exposure 注册、ChromaticSheet 等其他尾项 | 9 |

## 2. 修改文件与原因

### 2.1 Viewfinder、Camera 与输入

- `common/src/main/java/io/github/mortuusars/exposure/client/camera/CameraClient.java`
  - 使用 26.1.2 `GameRenderer.getMainCamera().setEntity(Entity)`。
  - 不调用会清空 entity/level 的 `Camera.reset()`。
  - 增加死亡、camera 失效、世界退出和断线的轻量状态清理。
- `common/src/main/java/io/github/mortuusars/exposure/client/camera/viewfinder/Viewfinder.java`
  - 输入迁到 `KeyEvent`、`MouseButtonEvent`。
  - 保持 Esc/背包键、controls 打开/返回、攻击拦截和中键行为。
- `common/src/main/java/io/github/mortuusars/exposure/client/camera/viewfinder/ViewfinderZoom.java`
  - 按键迁到 `KeyEvent`，保留滚轮方向、Shift 精细缩放、步长、惯性、clamp 和同步设置。
- `common/src/main/java/io/github/mortuusars/exposure/client/input/KeyboardHandler.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/input/MouseHandler.java`
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/KeyboardHandlerMixin.java`
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/MouseHandlerMixin.java`
  - 对齐 26.1.2 `keyPress(long,int,KeyEvent)`、`onButton(long,MouseButtonInfo,int)`、`onScroll` 与 `turnPlayer` 实际签名和 LVT。
  - 保持 zoom、灵敏度缩放与 selfie 旋转输入。
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/CameraMixin.java`
  - 旧 `Camera.setup` 注入迁到 `alignWithEntity(float)`。
  - 保持第一人称相机 Y 偏移、第三人称正面 selfie 旋转与最大相机距离。

### 2.2 Overlay 与 controls

- `common/src/main/java/io/github/mortuusars/exposure/client/camera/viewfinder/ViewfinderOverlay.java`
  - 迁到 `GuiGraphicsExtractor`、`Matrix3x2fStack`、`GUI_TEXTURED`、`text` 和 `nextStratum`。
  - 玩家 bob 数据迁到原版 Camera 同源的 `ClientAvatarState` 插值入口。
  - 保持 opening、遮罩、快门、guide、BSOD、胶卷状态图标、纹理、UV、颜色、坐标和动画。
- `common/src/main/java/io/github/mortuusars/exposure/client/camera/viewfinder/ViewfinderCameraControlsScreen.java`
  - 迁到 `extractRenderState`/`extractBackground` 和事件对象输入。
  - 保持 widgets、焦点、hover、tooltip、drag、滚轮、方向键、确认、Esc 与 packet 调用。

Overlay/controls 不再使用旧 GUI 即时 `drawString`/`blit`/`PoseStack`。`ItemInHandRendererMixin` 中保留的 `PoseStack` 是 Phase 16 的三维手臂/物品渲染路径，不是 Overlay GUI 回退。

### 2.3 FOV

- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/GameRendererMixin.java`
  - 删除不存在的 `GameRenderer.getFov` 注入。
  - 增加窗口 resize 时取消活动 capture 的安全边界。
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/CameraMixin.java`
  - 在 26.1.2 `Camera.calculateFov(float):float` 的真实返回边界应用 Exposure FOV。
- `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/camera/button/FocalLengthButton.java`
  - 改读公开的 `Camera.getFov():float`。
- `common/src/main/java/io/github/mortuusars/exposure/client/render/FovModifier.java`
  - 增加世界/客户端 reset 的立即清理。
- `common/src/main/resources/exposure.accesswidener`
  - 删除旧 `GameRenderer.getFov` 条目。

### 2.4 CameraCapture 与 action

- `common/src/main/java/io/github/mortuusars/exposure/client/capture/Capture.java`
  - 单次完成门控、超时/异常统一失败、客户端线程完成、活动 capture 集合和生命周期取消。
  - 世界退出、断线、资源重载、resize 可安全恢复 action。
- `common/src/main/java/io/github/mortuusars/exposure/client/capture/action/CompositeAction.java`
  - apply 保持声明顺序；成功 restore 保持旧正序，失败/异常/取消严格逆序。
  - 部分 apply 失败也恢复已开始的 action；restore 幂等。
  - 转发 `onSuccess`/`onFailure`。
- `common/src/main/java/io/github/mortuusars/exposure/client/capture/action/SetCameraEntityAction.java`
  - 恢复前校验旧 entity 未移除且仍属于当前 level，否则回退本地玩家。
- `common/src/main/java/io/github/mortuusars/exposure/client/capture/template/CaptureTemplate.java`
  - 失败聊天 API 迁到 `sendSystemMessage`，不改错误内容和后处理链。
- `common/src/main/java/io/github/mortuusars/exposure/client/render/GammaModifier.java`
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/ExposureLightTextureMixin.java`
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/GameRendererLightmapAccessor.java`（新增）
- `common/src/main/resources/exposure-common.mixins.json`
  - 旧 light texture 已删除；gamma 迁到 `LightmapRenderStateExtractor` 的 brightness。
  - action apply/restore 时立即重建当前 lightmap UBO，保证 background capture 同帧生效。
- `common/src/main/java/io/github/mortuusars/exposure/event/ClientEvents.java`
  - level unload、disconnect、resource reload 先取消 capture，再清理 camera/viewfinder。
- `common/src/main/java/io/github/mortuusars/exposure/world/item/camera/Flash.java`
  - 对齐 `sendParticles` 新 `overrideLimiter/alwaysShow` 参数。
  - `FLASH` 迁到白色 `ColorParticleOption`，保留原视觉意图。

没有修改 `ScreenshotReadback`、`BackgroundScreenshotCaptureTask`、`DirectScreenshotCaptureTask`、CaptureShader/PostChain ownership 或任何图像/调色板算法。

### 2.5 EntitiesInFrame

- `common/src/main/java/io/github/mortuusars/exposure/world/camera/frame/EntitiesInFrame.java`
  - 用 server-safe visual culling box helper 替代已删除的 entity culling API。
  - 保持筛选和排序业务规则。

### 2.6 CameraItem 与 EnderMan

- `common/src/main/java/io/github/mortuusars/exposure/world/item/camera/CameraItem.java`
  - 调用真实原版 `EnderMan.isBeingStaredBy(Player)`。
- `common/src/main/resources/exposure.accesswidener`
  - 更新 EnderMan 方法名与真实 descriptor。

## 3. 旧 Viewfinder 数据流与迁移后职责

1. 服务端/同步状态把 active Exposure camera 写入 `CameraOperator`；客户端 `LocalPlayerMixin.setActiveExposureCamera` 调用 `CameraClient.setupViewfinder`。
2. `CameraClient` 只拥有当前客户端 `Viewfinder`。active camera 的权威状态仍在 `CameraOperator`，没有新增 packet、NBT 或 component。
3. `Viewfinder` 构造时创建 `ViewfinderZoom`、Overlay、ViewfinderShader 和 Selfie；controls screen 仅按需创建并由 `Minecraft.screen` 显示。
4. `CameraInHand` 从指定手及玩家库存动态解析相机；`CameraOnStand` 保存 stand 与 operator 的明确生命周期引用。没有新增长期保存 Level/Player 的字段。
5. Camera Stand 的客户端 entity tick 负责把主 Camera entity 切到 stand；退出时恢复玩家。setup 时强制第一人称；stand 不允许切到 selfie。
6. 手持相机第一人称为正常 viewfinder；`THIRD_PERSON_FRONT` 表达 selfie。`CameraMixin.alignWithEntity` 在 mirrored 前视角加入 Exposure selfie 旋转。
7. `GuiMixin` 在 HUD 抽取时调用 Overlay；配置要求隐藏 HUD 时取消后续 HUD。`ItemInHandRendererMixin` 在看取景器时取消手臂/物品提交。
8. ViewfinderShader 每 tick 根据 `isLookingThrough`、filter attachment 和资源可用性更新；关闭 Viewfinder 时清除 Identifier。
9. 鼠标滚轮进入 zoom；按键进入 key binding；sprint+鼠标在 selfie 中旋转相机；controls 活动时不重复进入、不退出 viewfinder。
10. Esc/背包键关闭 controls 时回到 viewfinder；未开 controls 时 deactivate。切换/移除相机、相机失活和死亡由 active camera tick 退出。
11. level unload、disconnect、player/level 已空时只做客户端 reset，不发送无效 packet；防止断线 NPE。
12. capture 的临时 Camera entity/FOV/GUI/Gamma/post effect/filter/flash 状态不进入 Viewfinder 持久状态。

`CameraHolder` 表示物理持有/放置相机的实体和拍摄上下文；`CameraOperator` 持有当前 active camera 客户端/同步状态；`CameraClient` 只负责本地 viewfinder 与主渲染 Camera 桥接。

## 4. Minecraft 26.1.2 Camera 与 FOV 实际架构

直接检查 merged jar：

`D:/Codex/Exposure/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-1b20f767e8/26.1.2/minecraft-merged-1b20f767e8-26.1.2.jar`

确认结果：

- `GameRenderer` 已无 `getFov`。
- FOV owner 是 `net.minecraft.client.Camera`。
- 私有计算入口：`calculateFov(F)F`。
- 公开最终值入口：`getFov()F`。
- Camera 每帧 `update(DeltaTracker)`：取得 camera entity partial tick，`alignWithEntity(float)`，`calculateFov(float)`，再生成 HUD FOV、frustum 和 projection。
- `tickFov()` 使用 `AbstractClientPlayer.getFieldOfViewModifier(firstPerson, options.fovEffectScale)`，继续包含疾跑、飞行、弓/使用物品等原版 modifier，并按原版方式平滑 `oldFovModifier/fovModifier`。
- `calculateFov` 从 options FOV 与 modifier 得到基础值，再应用死亡/液体等原版分支。panoramic 有独立早返回。
- 主 Camera：`GameRenderer.getMainCamera()`。
- 当前 camera entity：`Minecraft.getCameraEntity()` 最终读取主 Camera entity；公开 `Minecraft.setCameraEntity` 还会改变 entity post effect，因此 Exposure 的临时 capture 直接调用 `Camera.setEntity`。
- `Camera.reset()` 会清空 level/entity，不可在 `setEntity` 后机械调用。
- `CameraType.FIRST_PERSON`、`THIRD_PERSON_BACK`、`THIRD_PERSON_FRONT` 仍存在；`isFirstPerson/isMirrored` 表达第一人称和 selfie 镜像。
- Camera 原版插值使用 `DeltaTracker` 的真实 partial tick；本阶段没有固定为 0 或 1。
- Camera 的 perspective FOV 是纵向值；横向视野由 projection 按窗口 aspect ratio 推导。Exposure 的 36 mm 换算和正方形 viewfinder 业务公式未改。
- RealCamera 兼容类仍存在并参与源码，但本阶段无直接编译证据要求修改，按范围限制跳过。

### 4.1 FOV 注入设计

1. 原版基础值完整计算后，Exposure 才在 `Camera.calculateFov` return 应用 viewfinder/capture 值。
2. 正常 return 使用 `@ModifyReturnValue(ordinal = 1)`，因此未激活 Exposure 时仅旧恢复动画可能参与，panoramic 早返回保持原版。
3. active viewfinder 或 capture override 使用全 return 注入，也覆盖特殊早返回；与旧双钩子语义一致。
4. `ViewfinderZoom` 保存 `FocalRange`、current FOV、target FOV 和 300 ms `EASE_OUT_EXPO` Animation。
5. Exposure zoom 动画原本就是 wall-clock Animation，不消费 partial tick；本阶段没有伪造 partial tick。原版基础 FOV 仍使用 Camera 的 DeltaTracker partial tick。
6. 18–55 等 lens range、10–300 合法焦距边界、`ZOOM_STEP=8`、精细系数 `0.25`、惯性、clamp 和滚轮方向保持。
7. 焦距换算仍为 36 mm：`f = 36 / (2*tan(FOV/2))`；反向为 `2*atan(36/(2*f))`。
8. vanilla `fovEffectScale=0` 时，原版动态 modifier 按原版关闭；Exposure viewfinder zoom 仍在最终边界生效，不会重新开启疾跑/弓等效果。
9. 离开 viewfinder 按旧规则用 300 ms ease 恢复原版值；world/disconnect 异常 reset 立即清理，避免跨世界污染。
10. capture override 优先于 viewfinder；restore 只取消 override，仍在 viewfinder 时回到 viewfinder zoom，否则回到原版/恢复动画。
11. 内部焦距/zoom 继续使用 double；只在 26.1.2 Camera 的 float 返回边界转换，避免额外提前截断。
12. 因公开 `Camera.getFov()` 足以供 controls 显示，且 Mixin 可在私有方法返回注入，不再需要 GameRenderer FOV AW。

## 5. Overlay 与 controls 迁移

### 5.1 Overlay

- render 入口接收 `GuiGraphicsExtractor` 与 `DeltaTracker`。
- opening 和 scale 即使不绘制也继续更新，供其他逻辑使用。
- 二维中心缩放、bob、攻击动画和旋转延迟使用 `Matrix3x2fStack`。
- bob 读取 `AbstractClientPlayer.avatarState().getBackwardsInterpolatedWalkDistance(partialTick)` 与 `getInterpolatedBob(partialTick)`；这与 26.1.2 Camera render-state extraction 同源。
- 纹理使用 `RenderPipelines.GUI_TEXTURED`；文字使用 extractor `text`；各层用 `nextStratum` 分隔。
- opening 两侧遮罩、快门、viewfinder frame、composition guide、BSOD、无胶卷与剩余张数分支均保留。
- 未用静态截图替代真实世界视图，未修改 Phase 17 ViewfinderShader。

### 5.2 Controls

- `render`/`renderBackground` 迁到 `extractRenderState`/`extractBackground`。
- 继续复用原 widgets、buttons、sliders、focus/hover/active/disabled/tooltip 状态。
- 鼠标接口使用 `MouseButtonEvent`；键盘接口使用 `KeyEvent`；持续按键状态用 `InputConstants.isKeyDown(Window,int)`。
- packet ID、字段顺序、Codec、服务端校验和 CameraSettings 同步方式未改。
- Screen 存在期间 viewfinder 保持 active；`onClose` 返回 viewfinder，不重复 setup。

## 6. CameraCapture action、异步边界与恢复

### 6.1 CameraCaptureTemplate 顺序

声明和 apply 顺序保持：

1. `SetCameraEntityAction`
2. `ForceRegularOrSelfieCameraTypeAction`
3. 可选 `SetFovAction`
4. `HideGuiAction`
5. 可选 `DisablePostEffectAction`
6. `SetFilterAction`（抑制 viewfinder shader，设置 CaptureShader Identifier）
7. 可选 `ModifyGammaAction`
8. 可选 `FlashAction`
9. 按 Phase 17 条件选择 direct 或 background screenshot
10. screenshot future 完成后才进入 CPU effects
11. crop/resize/exposure/contrast/levels/HSB/noise/single-channel
12. palettize
13. `ExposureData` 构造
14. upload

projection override、camera ID、Frame/CaptureParameters、上传协议、图像公式与 palette 均未改。

### 6.2 生命周期保证

- `initialize` 与 delay tick 顺序保持。
- apply 仍按上述顺序；成功 restore 保持旧正序；失败、异常、取消与部分 apply 失败严格逆序。
- action 部分 apply 抛错时恢复所有已经开始的 action。
- screenshot future 的成功、业务失败、异常和 12 秒超时统一进入一次性 finisher。
- `AtomicBoolean` 保证同一 capture 不重复 onSuccess/onFailure、restore 或 complete。
- completion callback 切回 Minecraft client thread，避免超时 scheduler 线程修改渲染状态。
- `Capture.cancelAll()` 覆盖 level unload、disconnect、resource reload 和 GameRenderer resize。
- cancel 会先完成底层 future 为失败；Phase 17 fenced callback 若迟到，现有 `future.complete(...) == false` 分支会关闭 image/target，不泄漏也不假成功。
- direct capture 仍至少等待一帧；background capture 仍独立 depth target 并异步 readback；没有 `join/get/sleep` 或 GPU 同步等待。
- Gamma 现在修改 `LightmapRenderState.brightness`，并在 action apply/restore 时立即提交当前 Lightmap UBO；下一次 extractor update 继续从原版 options gamma 计算后叠加 offset。
- capture failure 若旧 camera entity 已失效，恢复到当前本地玩家；world 已空则不写无效 Camera。

## 7. EntitiesInFrame 语义

原筛选顺序保持：

1. holder block position 周围 128 block AABB 查询实体。
2. 按 camera position 到实体 position 的距离升序稳定排序。
3. 仅 `LivingEntity`。
4. 必须 alive。
5. entity eye position 必须在以 camera POV 构造的正方形 frustum 内；FOV 保留 5% edge margin。
6. visual size 修正后的距离不得超过由 FOV 换算的 focal length。
7. camera position 到 entity eye position 用 `ClipContext.Block.COLLIDER`、`Fluid.NONE`，必须 MISS。
8. 保持排序加入结果；Frame/EntityInFrame 字段和 ExtraData hook 不变。

本阶段没有新增 invisibility、spectator 或 entity type 过滤，因为旧业务没有这些条件；也没有返回空列表或跳过 LOS。

26.1.2 把 `getBoundingBoxForCulling` 从 Entity 移到客户端 renderer 的 protected 方法，而 EntitiesInFrame 在专用服务端计算，不能调用 renderer。新的 helper：

- 默认使用 entity bounding box，与 `EntityRenderer` 默认值一致。
- dragon head 头饰全轴 inflate 0.5，与 `LivingEntityRenderer` 一致。
- Happy Ghast 向下扩展半个实体高度。
- Illusioner 仅 X/Z inflate 3。
- Sniffer 全轴 inflate 0.6。
- Ender Dragon 合并全部 `EnderDragonPart` AABB，保留 multipart/大型实体尺寸语义。

merged jar 审计确认以上是 26.1.2 所有 vanilla LivingEntity renderer 的 culling-box override。第三方自定义 renderer 的 protected visual box 在 dedicated server 没有可查询注册表；本阶段不处理被禁止的 mod integration，风险见第 12 节。

## 8. CameraItem 与 EnderMan

旧逻辑不是替换 EnderMan AI，也不是“拿相机就激怒”：只有照片已通过 EntitiesInFrame 记录该 EnderMan 后，`entityCaptured` 才检查特殊 advancement。

条件保持：

1. camera holder 本身必须是 `ServerPlayer`；Camera Stand 不伪装成玩家观察源。
2. 被拍实体必须是 EnderMan。
3. EnderMan 当前 target 必须正是该玩家。
4. 再调用原版完整凝视判定。

26.1.2 私有 `isBeingStaredBy(Player):boolean` 字节码：

- 先执行 `LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM`，保留南瓜/伪装保护。
- 再调用继承的 `isLookingAtMe(LivingEntity, 0.025, true, false, double[]{enderMan.getEyeY()})`。
- 因而保留原版角度数学、距离/eye height 与 LOS 语义。

没有复制不完整算法，没有固定 true/false，没有修改 EnderMan AI 或增加 packet。

## 9. Access Widener 结果

### 9.1 GameRenderer FOV

旧条目：

```text
accessible method net/minecraft/client/renderer/GameRenderer getFov (Lnet/minecraft/client/Camera;FZ)D
```

旧调用方是 `FocalLengthButton`，旧用途是取得当前原版+Exposure 最终 FOV。26.1.2 无此方法；调用方已改读公开 `Camera.getFov()F`，FOV 注入已移到 Camera Mixin，因此旧 AW 在调用方清零后删除。

### 9.2 EnderMan

旧条目：

```text
accessible method net/minecraft/world/entity/monster/EnderMan isLookingAtMe (Lnet/minecraft/world/entity/player/Player;)Z
```

26.1.2 真实条目：

```text
accessible method net/minecraft/world/entity/monster/EnderMan isBeingStaredBy (Lnet/minecraft/world/entity/player/Player;)Z
```

保留该最小私有访问是为了复用完整原版凝视算法；公开五参数父类 helper 不能独立包含 EnderMan 的 disguise predicate 和 eye-Y 参数构造。

阶段相关旧目标均清零；当前保留一个有效的 EnderMan 新目标。整个 AW 文件现有 17 条访问目标，common/fabric 验证均通过。

## 10. 分组验证

| 组 | 日志 | common 错误 |
|---|---|---:|
| 基线 | `phase18-common-baseline.log` | 131 |
| 1 Viewfinder/Camera/input | `phase18-group1-viewfinder-camera-input.log` | 125 |
| 2 Overlay/controls 首次 | `phase18-group2-overlay-controls.log` | 74 |
| 2 Overlay/controls 最终 | `phase18-group2-overlay-controls-final.log` | 70 |
| 3 Zoom/FOV | `phase18-group3-viewfinder-zoom-fov.log` | 69 |
| 4 Capture/action 首次 | `phase18-group4-camera-capture-actions.log` | 66 |
| 4 Capture/action 最终 | `phase18-group4-camera-capture-actions-final.log` | 64 |
| 5 EntitiesInFrame | `phase18-group5-entities-in-frame.log` | 63 |
| 6 CameraItem/EnderMan | `phase18-group6-camera-item-enderman.log` | 62 |

第六组 AW：

- `phase18-group6-common-accesswidener.log`：成功。
- `phase18-group6-fabric-accesswidener.log`：成功。

最终命令：

| 命令 | 日志 | 结果 |
|---|---|---|
| `./gradlew :common:compileJava` | `phase18-common-compile-final.log` | 失败，62 条后续错误 |
| `./gradlew :fabric:compileJava` | `phase18-fabric-compile-final.log` | 被 common 阻断，Fabric javac 未启动 |
| `./gradlew clean build --continue` | `phase18-clean-build-final.log` | 失败于 common 62 条；AW task 成功 |
| `./gradlew :common:validateAccessWidener` | `phase18-common-accesswidener.log` | 成功 |
| `./gradlew :fabric:validateAccessWidener` | `phase18-fabric-accesswidener.log` | 成功 |

## 11. 剩余 62 条错误

| 文件/类别 | 数量 |
|---|---:|
| PhotographSlotWidget | 7 |
| TestImageScreen | 7 |
| BuggerScreenOverlayMixin | 7 |
| Bugger | 6 |
| GuiUtil 旧即时辅助方法 | 5 |
| ResourceImage | 5 |
| ColorPalettes registry API | 4 |
| ClientGUI | 3 |
| PhotographScreen | 3 |
| ExportExposuresTask | 3 |
| Exposure 注册/API | 3 |
| LightroomScreen | 2 |
| ChromaticSheetItem | 2 |
| NativeImageFileSaver | 1 |
| FileCaptureTask | 1 |
| UrlCaptureTask | 1 |
| ImageExporter | 1 |
| WrappedNativeImage | 1 |

这些错误全部属于用户明确禁止在 Phase 18 顺带处理的普通 GUI、Bugger、ResourceImage/NativeImage/export、注册或数据 API 尾项。因此按范围要求停止，没有为了让 common 清零而扩张。

## 12. 静态检查与风险

### 已确认

- target Viewfinder Overlay/controls 无旧即时 GUI drawString/blit/PoseStack/旧输入签名。
- 无固定 FOV、无禁用 viewfinder、无空 Overlay。
- Viewfinder 未激活时 Camera 使用原版 FOV；普通疾跑、飞行、弓、死亡、液体和状态效果路径未覆盖。
- capture action 均有恢复路径；成功 restore 保持旧正序，失败/异常 restore 逆序且幂等。
- capture 只有一个 completion gate，不重复成功/失败/恢复。
- 没有渲染线程 `join/get/sleep`，Phase 17 readback 仍异步 fenced callback。
- EntitiesInFrame 未返回空列表，未删除实体类型，LOS 和稳定排序保留。
- EnderMan 未固定 true/false，原版 disguise/角度/LOS 保留。
- ShaderManager ownership、19 个 post effect、RenderTarget/readback 未修改。
- networking、ExtraData、SavedData、照片格式和算法未修改。
- `exposure-common.mixins.json` 的 `required: true` 与 `defaultRequire: 1` 保持。

### 尚存风险

- **运行时 Mixin apply**：`Camera.calculateFov` ordinal、`alignWithEntity`、输入 LVT 与 Lightmap extractor 均按 merged jar 字节码核对，但 common 未清零，无法启动客户端做 runtime apply 验证。
- **视觉回归**：Overlay 坐标/UV/颜色和层级保持，但未能进入游戏做比例、bob、alpha 与 controls tooltip 的视觉 QA。
- **FOV 跳变**：正常退出保留旧 300 ms restore；异常世界 reset 立即恢复。代码语义明确，但需运行测试窗口 resize、死亡与跨维度时的主 Camera 切换。
- **zoom 精度**：内部 double、最终 float 与原版签名一致；只有 Camera 边界一次转换。风险低。
- **capture 状态**：一次性 gate、成功旧正序 restore 与异常逆序 restore 已静态保证；需运行测试 direct/background 的 timeout、reload、resize 与迟到 readback。
- **Gamma**：迁到新 UBO render-state 并即时提交，需视觉确认不同维度、夜视与 darkness 的叠加结果。
- **第三方 visual culling box**：vanilla LivingEntity override 已完整覆盖；服务端无法查询客户端第三方 renderer override。本阶段按禁止集成范围仅记录风险。
- **EnderMan**：AW 验证成功且复用完整原版方法；需游戏内确认 advancement 在普通相机/selfie/stand 边界与旧版本一致。

## 13. 失败尝试与修正记录

- 机械迁移旧 `GameRenderer.getFov` 不可行：26.1.2 方法已删除。改为基于 Camera 字节码选择注入点。
- Overlay 第一次编译剩 4 条 `walkDist/bob` 字段错误；核对 Camera extraction 后改用 `ClientAvatarState`，第二次清零。
- Flash 第一次只补新增布尔参数仍失败，因为 `FLASH` 已变为 `ParticleType<ColorParticleOption>`；改用白色 ColorParticleOption 后清零。
- 旧 `gameRenderer.lightTexture().tick()` 已删除；改为 Lightmap render-state + 最小 Mixin accessor，不扩大 AW。
- 最终矩阵第一次组合执行因外层工具超时过短被中止；随后逐项运行，并在 resize 补丁后完整复跑，最终日志有效。

## 14. 明确跳过和保护项

按范围明确未处理：

- Bugger 与调试覆盖层。
- ResourceImage、NativeImageFileSaver、File/URL projecting 尾项、ImageExporter 与 export task。
- 与 Viewfinder 无关的 Photograph/Lightroom/Test/album 普通 GUI。
- JEI、Jade、Create、RealCamera、Easy Anvils 集成。
- networking、packet schema、SavedData、ExtraData、旧照片路径、照片格式。
- ExposureData 像素编码、调色板、胶片、曝光和后处理算法。
- Phase 17 GLSL、post-effect 资源、ShaderManager ownership、RenderTarget 与 ScreenshotReadback。
- Phase 16 手持照片姿态、Phase 15 动态照片、Phase 13 item model。

## 15. 下一阶段建议

建议下一阶段新建对话，并以本报告、Phase 17 报告和 `phase18-common-compile-final.log` 为起点。新阶段应只处理当前 62 条尾项，建议顺序：

1. 普通 GUI + GuiUtil 抽取式绘制（PhotographSlotWidget、TestImageScreen、PhotographScreen、LightroomScreen、ClientGUI）。
2. ResourceImage/NativeImage/WrappedNativeImage 与 File/URL load API。
3. export task/ImageExporter。
4. Bugger 独立迁移。
5. ColorPalettes、Exposure 注册与 ChromaticSheet 少量 API。
6. common 清零后立即让 Fabric javac 真正启动，再单独处理 Fabric integration。

模型建议：**GPT-5.6 Terra**。[OpenAI 官方模型目录](https://developers.openai.com/api/docs/models)把 Terra 定位为智能与成本平衡，Luna 定位为成本敏感、高吞吐工作。下一阶段虽是尾项，但仍包含抽取式 GUI、NativeImage 资源生命周期和 registry 边界，判断密度高于机械批处理，因此 Terra 更合适。若后半段只剩大量同构签名替换，再考虑 Luna。

建议新建对话：**是**。原因是 Phase 18 已形成完整交接报告，下一阶段范围不同，且当前线程上下文很长；新线程可减少把本阶段 capture/FOV 设计误带入普通 GUI/export 清尾的风险。

## 16. 交接摘要

已完成：Viewfinder 生命周期、Overlay、controls、zoom、Camera FOV、输入、CameraCapture action 与异常恢复、Gamma/flash、EntitiesInFrame、CameraItem/EnderMan、两个旧 AW 目标处理。

关键决策：FOV owner 改为 Camera；Exposure 在原版最终 FOV return 应用；capture 成功保持旧正序恢复、异常逆序恢复并单次完成；服务端建立 vanilla-equivalent visual box helper；EnderMan 复用私有完整原版判定。

验证结果：`131 -> 62`；target 错误清零；common/fabric AW 成功；Fabric javac 尚未启动。

遗留问题：62 条明确后续尾项，以及因 common 未清零而无法执行的 runtime Mixin/视觉/capture 集成测试。

下一步：新建 Phase 19 对话，以 Terra 处理普通 GUI、ResourceImage/export/Bugger/注册 API，common 清零后首次进入 Fabric javac。
