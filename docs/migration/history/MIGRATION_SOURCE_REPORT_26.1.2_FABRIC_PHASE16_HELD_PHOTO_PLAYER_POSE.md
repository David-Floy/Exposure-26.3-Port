# Phase 16：手持照片、第一人称物品渲染与玩家姿态

日期：2026-07-14  
目标：Minecraft 26.1.2 Fabric / Gradle 9.4.0 / Loom 1.16.3 / Java 25

## 结论

本阶段完成了第一人称单张/叠放照片的 collector 提交、第三人称玩家手持动态照片 state/submit 边界，以及基于 `HumanoidRenderState` 的相机姿态提取。`common` javac 错误从 **195** 降到 **186**（-9）；阶段新增或修改的源码没有可见 javac 诊断。`:fabric:compileJava` 仍在 common 失败处停止，fabric javac 未开始。

没有修改 Shader、GameRenderer、RenderTarget、截图、取景器内部绘制、CameraCapture、EnderMan 观察语义、networking、ExtraData、SavedData、照片格式/编码、相机算法、Phase 13 item-model tree 或 Phase 15 动态纹理缓存。Access Widener 文件未修改，验证仍只报告既有 3 个旧目标。

## 修改文件与原因

| 文件 | 原因 |
|---|---|
| `common/.../client/render/photograph/HeldPhotographRenderRequest.java` | 新增轻量手持组合请求；复用 Phase 15 顶层 `PhotographRenderRequest`，叠放照片只额外保存最多两层纸张的 Identifier、旋转、偏移、light 与 RGBA。 |
| `common/.../client/render/photograph/PhotographRenderer.java` | 新增 `prepareHeld`/`submitHeld`，恢复旧 stacked photographs 的三层、顺序、偏移与亮度语义；没有恢复 immediate buffer overload。 |
| `common/.../mixin/client/ItemInHandRendererMixin.java` | 将第一人称目标与 shadow 参数迁移到 26.1.2 `SubmitNodeCollector`，以局部 held request 替换两个失效 immediate 调用。 |
| `common/.../client/animation/CameraPoseRenderState.java` | 新增实体无关的姿态快照：枚举、相机物品 Identifier、作用手、action 值、自拍视角布尔及支架头部角度。 |
| `common/.../client/animation/ExposureHumanoidRenderState.java` | `HumanoidRenderState` 的明确类型扩展接口。 |
| `common/.../mixin/client/HumanoidRenderStateMixin.java` | 在原版 humanoid state 中保存单一轻量姿态快照。 |
| `common/.../mixin/client/HumanoidMobRendererMixin.java` | 在实际静态 `extractHumanoidRenderState` 尾部从实体/现有同步状态提取 Exposure 姿态，不保留实体引用。 |
| `common/.../mixin/client/HumanoidModelMixin.java` | 从旧 `setupAnim(LivingEntity,FFFFF)` 迁移到 `setupAnim(HumanoidRenderState)`；在原版姿态计算后应用相机姿态并保留 arm bob multiplier。 |
| `common/.../client/animation/CameraPoses.java` | 姿态函数改为只消费 state 中的 arm、角度、action 与布尔；`ModelPart.copyFrom` 改为 26.1.2 `storePose/loadPose`。 |
| `common/.../client/animation/CameraModelPoses.java` | registry key 改为轻量 `Identifier`，避免 render state 保存 `CameraItem`。 |
| `common/.../client/render/photograph/AvatarPhotographRenderState.java` | 第三人称玩家左右手请求接口。 |
| `common/.../mixin/client/AvatarRenderStateMixin.java` | 在 `AvatarRenderState` 中分别保存左右手 held request。 |
| `common/.../mixin/client/AvatarRendererMixin.java` | 在玩家 state 提取尾部生成请求；仅请求有效时清空对应原版 `ItemStackRenderState`。 |
| `common/.../mixin/client/PlayerItemInHandLayerMixin.java` | 在玩家第三人称手持物品提交边界复现原版手臂/身体矩阵和 `item/generated` transform，提交动态照片并只取消照片物品模型。 |
| `common/src/main/resources/exposure-common.mixins.json` | 注册 5 个新增 required client mixin；全局 `required: true` 与 `defaultRequire: 1` 保持不变。 |

另生成了本阶段完整编译、构建、AW 日志及 `phase16-merged-jar-architecture.log`。工作区不是 Git 仓库，因此修改清单按本阶段实际写入记录，而非 `git diff` 生成。

## 基线错误分类

`phase16-common-baseline.log` 保存了完整 195-error 输出。按 javac 诊断和源码调用图分类：

| 类别 | 基线 | 阶段结束 |
|---|---:|---:|
| `ItemInHandRendererMixin` | 2：两个已删除 immediate overload 调用 | 0 |
| `ItemInHandRenderer` / hand state 架构 | 目标签名过时但没有额外独立 javac 诊断 | 0 |
| `HumanoidModelMixin` | 2：`AgeableListModel` 和旧泛型/方法边界 | 0 |
| `HumanoidModel` / `HumanoidRenderState` / `AvatarRenderState` | 没有独立旧 state 诊断；缺少业务 state 边界 | 新 state 源码 0 诊断 |
| 玩家手臂、头、身体与使用姿态 | `CameraPoses` 5：旧 `copyFrom`、`cameraEntity` 字段与 timer API | 0 |
| PhotographRenderer immediate overload 调用 | 2，均为第一人称 mixin | 0 |
| 玩家/手持路径的旧 `MultiBufferSource` / immediate 调用 | 同上 2 个调用及相关签名 | 0 |
| Exposure 特殊姿态 | 相机持有/自拍/支架/拆解由上述 5 个 `CameraPoses` 错误覆盖；Album/Interplanar Projector 无玩家姿态调用 | 0 |
| 明确后续阶段 | 高密度集中在 Shader、capture、viewfinder、截图与 RenderTarget 相邻 API | 未处理 |

基线有 190 个唯一 `file:line`，但部分行产生多个 javac error，所以 javac 总数为 195。结束时唯一位置为 181、javac 总数 186；消失的 9 个唯一位置正是本阶段上述 2 + 2 + 5。

## 旧手持照片数据流

第一人称旧路径：

`renderHandsWithItems -> renderArmWithItem -> 注入点（已 push PoseStack，并已计算 isMainHand/arm） -> 单手或双手照片矩阵 -> PhotographRenderer.render(ItemStack,...,MultiBufferSource,light)`。

业务条件与变换：

- 只识别 `PhotographItem` 与 `StackedPhotographsItem`；普通物品继续进入原版分支。
- 当正在看取景器时，旧逻辑弹出本方法的 PoseStack 并取消手持渲染；本阶段保留该边界判断，但没有进入 viewfinder 内部迁移。
- 主手照片且副手为空使用双手 map-like 姿态；其它情况使用单手姿态。
- 单手以 arm 正负号镜像，提交玩家手臂，然后应用 equip/swing 平移、X/Y 旋转、Y/Z 180 度、`0.38` 缩放、`(-0.5,-0.5,0)` 与 Z 翻转。
- 双手保留 map tilt、equip、swing、双臂提交、`2.0` 和 `0.38` 缩放及相同照片朝向。
- 旧照片调用固定 paper=true、backside=false、RGBA=255、当前 packed light。

旧 PhotographRenderer 对 stacked item 最多显示三张：按索引 2、1、0 自底向顶；下层只绘纸张，偏移为 `0.03125*i`，Z 为 `0.002-i/1000`，每层亮度降低 `0.2*i`；顶层 Z=`0.002` 并绘制完整照片。该语义由当前遗留 helper 与上游 1.21.1 源码交叉确认后恢复。

第三人称旧源码没有发现 PhotographRenderer immediate 调用。原流程是：

`HumanoidMobRenderer.extractHumanoidRenderState -> ArmedEntityRenderState.right/left ItemStackRenderState -> PlayerItemInHandLayer -> ItemInHandLayer.submitArmWithItem -> ItemStackRenderState.submit`。

因此旧第三人称照片实际落到 `exposure:item/photograph -> minecraft:item/generated` 的普通物品模型。本阶段按目标把玩家手中的单张/叠放照片恢复为动态照片提交；普通实体和普通物品仍走原流程。

## Minecraft 26.1.2 实际架构

直接检查 `minecraft-merged-1b20f767e8-26.1.2.jar` 后确认：

- 没有 `ItemInHandRenderState` 或 `HandRenderState`。`ItemInHandRenderer.renderArmWithItem(AbstractClientPlayer,float,float,InteractionHand,float,ItemStack,float,PoseStack,SubmitNodeCollector,int)` 仍在方法内计算手、equip、attack/swing 与物品矩阵。
- `ItemInHandRenderer.renderItem` 临时创建 `ItemStackRenderState`，经 `ItemModelResolver.updateForTopItem` 提取后调用 `ItemStackRenderState.submit(PoseStack,SubmitNodeCollector,light,overlay,outline)`。
- 第一人称手臂由 `renderPlayerArm` / `renderMapHand` 直接向同一 collector 提交；照片替代不能取消这些 helper 内的手臂提交。
- 玩家 renderer 已更名/重构为 `AvatarRenderer`，state 为 `AvatarRenderState extends HumanoidRenderState`。
- `AvatarRenderer.extractRenderState` 调用公共静态 `HumanoidMobRenderer.extractHumanoidRenderState(LivingEntity,HumanoidRenderState,float,ItemModelResolver)`，这是玩家与 humanoid mob 共享的姿态提取边界。
- `HumanoidModel<T extends HumanoidRenderState>` 的真实动画入口是 `setupAnim(HumanoidRenderState)`；原版先完成头/身体、步行、arm pose、attack 与 crouch，再执行两个 `AnimationUtils.bobModelPart`。
- 第三人称玩家物品层为 `PlayerItemInHandLayer`；其父层先 `translateToHand`，再应用左右/幼体偏移、attack/use transform，最后提交 `ItemStackRenderState`。

## 新手持照片请求/state 设计

第一人称不建立长期 state：`renderArmWithItem` 方法内调用 `prepareHeld`，请求被同一次调用的 submit 消费。equip、swing、主副手、arm 与 display context 都已由当前方法和矩阵表达，不复制进对象。

第三人称 state 分开保存右、左手 `HeldPhotographRenderRequest`。请求只含：

- 顶层 Phase 15 `PhotographRenderRequest`，其内部仍是 `ImageRenderRequest`、纸张/overlay Identifier、纸张旋转和正反面标志。
- stacked 时最多两个 paper-only request：纹理 Identifier、旋转、XY/Z 偏移、light 与 RGBA。
- 一个 `stacked` 布尔。

它不含 Player、LivingEntity、Level、ItemStack、Camera、CameraStandEntity、ExposureData 或像素数组。`List.copyOf` 固定最多两个下层纸张对象；像素仍只在 Phase 15 `ImageRenderer` cache 中。

`ImageRenderer.request` 在提取阶段只取得缓存句柄；world submit 的 `RenderedImageInstance.submit` 才保证纹理上传并调用 `submitCustomGeometry`。所以正常的首次非空 world request 可在同帧提交，不使用 GUI 的一帧重试，也不创建静态纹理 fallback。照片数据本身为空时 request 为 EMPTY，state 不清空原版 item state；这与 Phase 15 Item Frame 的 EMPTY/fallback 边界一致。

## ItemInHandRendererMixin 迁移

required mixin 仍目标 `ItemInHandRenderer`，注入 descriptor 改为上述 26.1.2 官方签名，位置仍为第一个 `ItemStack.isEmpty()` 调用：此时 `isMainHand`、`arm` 已生成且 PoseStack 已 push。

shadow 的 `renderPlayerArm`、`renderMapHand` 仅把 buffer 参数改为 `SubmitNodeCollector`。单/双手所有旧矩阵常量、equip/swing/map tilt、左右符号、手臂可见性判断与最终 pop/cancel 顺序保持。只有 held request 有效时才取消；普通物品完全不执行自定义矩阵。

照片继续走 `PhotographRenderer.submitHeld -> PhotographRenderer.submit -> ImageRenderer/TextureRenderer -> SubmitNodeCollector.submitCustomGeometry`，未调用 `GUI_TEXTURED`、未新建 `MultiBufferSource`、未恢复 immediate backend。

## HumanoidModel 与玩家姿态迁移

旧业务分支全部被显式编码为 `CameraPoseRenderState.Pose`：

- `HOLDING`：活动手控制相机，双臂跟随头部；保留 action 动画和左右臂 0.1/0.5 bob multiplier。
- `SELFIE`：相机手臂跟随头部；保留当前 camera entity 时的左右 `0.25` yaw 偏移，活动臂 bob multiplier 为 0。
- `STAND`：提取时根据操作者眼睛、stand 眼睛和 body yaw 算出受限头部 X/Y 角；model state 不保存 stand。
- `DISASSEMBLED`：未激活但手持拆解相机；本地第一人称仍跳过该第三人称模型姿态。
- `NONE`：普通玩家/生物，重置 multiplier 后完全沿用原版姿态。

action progress 使用 renderer 传入的 `partialTick` 从现有 `CameraOperator` 同步状态插值，再应用原 `EASE_OUT_CUBIC` 与前后半程折返。Camera pose registry 通过 `Identifier` 查找，不把 `CameraItem` 放入 state。

`HumanoidModelMixin` 在原版 attack/crouch 计算之后、SPYGLASS/bob 区段之前应用姿态；原版 `setupAnim` 不取消。两个 bob 调用仍重定向到 `AnimationUtils.bobModelPart`，只替换 Exposure 指定手臂的 multiplier。帽子使用 26.1.2 `head.storePose()` / `hat.loadPose(...)` 保持头部同步。

原 Real Camera 跳过条件只在提取阶段保留为 `NONE`；没有修改 compat 类或扩张到 Real Camera 迁移。

## 第三人称玩家手持照片

`AvatarRendererMixin` 在 state 提取尾部独立准备左右手请求；只有请求有效才清空对应原版 item model state。`PlayerItemInHandLayerMixin` 在官方 `submitArmWithItem(AvatarRenderState,ItemStackRenderState,ItemStack,HumanoidArm,PoseStack,SubmitNodeCollector,int)` 头部读取请求。

自定义提交复现父层的：

- model `translateToHand`，因此 crouching、swimming、riding、attack 和 Humanoid pose 已包含在手臂矩阵中；
- X=-90、Y=180 旋转；
- 左右手符号、普通/幼体偏移；
- STAB attack 与 use-item transform；
- `minecraft:item/generated` 对应的第三人称 `(0,3/16,1/16)` translation、`0.55` scale 和照片平面居中。

单张第三人称请求启用 paper backside，避免从背面观察时丢失纸张；stacked 路径保持旧顶层 backside=false 语义。light 与 RGBA 从提取时写入请求；左右手由 arm 选择独立 request，不共享可变状态。

## PhotographRenderer immediate overload 清理结果

Phase 15 已删除旧 `render(ItemStack,...,MultiBufferSource,...)` overload，本阶段开始时只有 `ItemInHandRendererMixin` 两个失效调用。本阶段迁移后：

- 全部 Java 调用方为 0；
- `PhotographRenderer` 没有 immediate overload 或空兼容 method；
- 玩家/手持参与路径不创建 `MultiBufferSource`；
- 新 stacked 支持只使用 collector request/submit，不重建动态纹理缓存。

Shader、RenderTarget、viewfinder/capture 路径没有 PhotographRenderer immediate 调用方，因此没有需要为后续阶段保留的 overload。

## Mixin 审计

| Mixin | 26.1.2 目标/注入点 | 风险 |
|---|---|---|
| `ItemInHandRendererMixin` | `renderArmWithItem(...,PoseStack,SubmitNodeCollector,int)`；首个 `ItemStack.isEmpty` | 依赖 `isMainHand`、`arm` 局部变量表；merged jar 已核对。 |
| `HumanoidMobRendererMixin` | 静态 `extractHumanoidRenderState(...,ItemModelResolver)` TAIL | Avatar 和 humanoid mob 都调用；非 CameraOperator 为 EMPTY。 |
| `HumanoidRenderStateMixin` | `HumanoidRenderState` interface/field | 单一明确类型 record，无实体引用。 |
| `HumanoidModelMixin` | `setupAnim(HumanoidRenderState)`；首个 SPYGLASS field 之前；redirect bob | FIELD/ordinal 与两个 bob 调用需运行时 apply 验证。 |
| `AvatarRendererMixin` | `extractRenderState(Avatar,AvatarRenderState,float)` TAIL | 26.1.2 bridge/generic descriptor 已由 javap 确认。 |
| `AvatarRenderStateMixin` | `AvatarRenderState` 左右请求字段 | 无 ItemStack/实体新增字段。 |
| `PlayerItemInHandLayerMixin` | Avatar 专用 `submitArmWithItem(...)` HEAD | shadow 继承的 `getParentModel` 与泛型 bridge 需运行时 apply 验证。 |

所有条目仍受 `exposure-common.mixins.json` 的 `required: true`、`defaultRequire: 1` 约束；没有 optional、`require=0` 或删除 required mixin。客户端 state/mixin 类只在 client mixin 列表，不进入 dedicated server mixin 列表。

由于 common 仍不能完成编译，尚不能启动客户端，故 Mixin apply 风险只能通过 merged jar descriptor、bytecode local table 与 javac/AP 静态证据降低，不能声明运行时已验证。

## 普通路径与禁止范围

- 非 Exposure Photograph/Stacked Photographs：第一人称立即返回原版；第三人称 state request 为 EMPTY，原版 `ItemStackRenderState` 不清空。
- 普通玩家/生物：`CameraPoseRenderState.NONE`，原版 setupAnim、crouch/swim/ride/use/attack 与 bob 继续执行。
- 玩家手臂没有被取消：第一人称 helper 在照片 submit 前仍执行；第三人称照片附着到已经计算完成的 model arm。
- 未修改 Shader loading/compile/uniform、GameRenderer、RenderTarget/framebuffer、后台/直接截图、ViewfinderOverlay/Shader/controls、CameraCapture、EntitiesInFrame、CameraItem/EnderMan 观察签名、JEI/Jade/Create/RealCamera/Easy Anvils。
- 未修改 networking packet/schema、ExtraData、SavedData、NBT key、Frame、照片文件路径/格式、像素编码、CaptureParameters 或拍摄算法。
- 未修改 Phase 13 item model JSON、Phase 14 CameraStand renderer/state、Phase 15 texture/cache/frame/Item Frame/GUI/Tooltip 后端。

## 分组与最终验证

| 命令/日志 | 结果 |
|---|---|
| `:common:compileJava` baseline — `phase16-common-baseline.log` | 195 errors |
| 第一组架构/request — `phase16-group1-hand-architecture.log` | 193；两个 immediate 调用消失 |
| 第二组 ItemInHand — `phase16-group2-item-in-hand.log` | 193；ItemInHand/held request 无新诊断 |
| 第三组 Humanoid state — `phase16-group3-humanoid-state.log` | 186；HumanoidModel 2 + CameraPoses 5 消失 |
| 第四组第三人称 — `phase16-group4-third-person.log` | 186；Avatar/hand-layer 无新诊断 |
| 最终 `:common:compileJava` — `phase16-common-compile-final.log` | 186 errors |
| `:fabric:compileJava` — `phase16-fabric-compile-final.log` | common 186 处停止；fabric javac 未开始 |
| `clean build --continue` — `phase16-clean-build-final.log` | common 186；资源/sources/AW 后续任务继续，整体失败 |
| `:common:validateAccessWidener` — `phase16-common-accesswidener.log` | 只报告既有 3 个旧目标 |
| `:fabric:validateAccessWidener` — `phase16-fabric-accesswidener.log` | 同上 |

Access Widener 剩余验证失败目标保持不变：

1. `GameRenderer.loadEffect(ResourceLocation)`；
2. `GameRenderer.getFov(Camera,float,boolean)`；
3. `EnderMan.isLookingAtMe(Player)`。

本阶段没有修改这三条，也没有修改 AW 文件其它条目。

## 剩余错误分布

最终 javac 为 186 errors、181 个唯一源码位置。主要唯一位置分布：

- viewfinder：约 66（Overlay 31、controls 22、shader 8、zoom 3、viewfinder 2）；
- capture/截图任务与 capture shader：约 40；
- 通用 `client.util.Shader`：9；
- 其它 Shader/RenderTarget 相邻调用包括 Gamma、post effect 与 packet handler；
- 剩余为 GUI API、ResourceImage/export、Bugger 工具和少量其它未迁移 API。

本阶段参与的 ItemInHand、Photograph held request、Avatar state/layer、Humanoid state/model 与 CameraPoses 文件均不在最终错误列表。剩余错误已主要集中于本阶段明确禁止的 Shader、RenderTarget、截图、取景器和 capture 边界。

## 静态检查结果

- `ItemInHandRendererMixin` 不再调用 immediate PhotographRenderer。
- 玩家/手持参与路径没有 `MultiBufferSource` 或旧 `VertexConsumer`。
- 没有空 renderer、固定照片、删除照片分支或把成功请求降级成普通 item model。
- 新 render state/request 不保存 Player、LivingEntity、Level、ItemStack、Camera、stand 或像素数组。
- 普通 item state 仅在 held request 非空时清除；左右手分别处理。
- 第一人称主/副手、左右镜像、单/双手、swing/equip 分支保留。
- Humanoid 普通姿态不取消原版 `setupAnim`。
- required/defaultRequire 未降级。
- Phase 15 cache、世界/GUI backend、Item Frame/frame renderer 未回退。
- AW 验证仍精确为同 3 个禁区旧目标。

## 风险、失败尝试与运行时待验证

风险：

- 第一人称矩阵、双手遮挡、左手镜像和 stacked Z 排序需要可启动客户端后的视觉回归。
- 第三人称手持层按 Exposure 当前 `item/generated` 默认 transform 复现；资源包若只覆写 photograph model 的 display transform，自定义动态照片不会自动读取该覆写，存在资源包视觉差异风险。
- 第三人称透明 sorting、paper backside、outline 与不同 player model/slim arm 需要运行时检查。
- Humanoid SPYGLASS FIELD 注入、ItemInHand locals、Avatar generic bridge 和继承 shadow 存在 Mixin apply 风险。
- state 提取使用 passed partial tick 代替旧全局 timer 查询，语义更符合 render-state 架构，但应对 action animation 做逐帧比较。
- CameraOnStand 头部角度现在在 extraction 快照中计算；应验证快速转头、跨帧 stand 更新和远端玩家插值。

失败/调整记录：

- 初次用 PowerShell 默认读取 Phase 15/13 报告出现 UTF-8 误解码，随后以显式 UTF-8 完整重读。
- `jar`/`javap` 不在 PATH，改用 Gradle 实际 JVM `C:\Program Files\Java\jdk-25.0.3\bin` 检查 merged jar。
- 普通搜索未直接找到 stacked renderer；随后以当前 helper 和上游 1.21.1 原始源码交叉核对，避免漏掉下层纸张、顺序、亮度和偏移。
- 没有为了错误数量尝试空 renderer、optional mixin、删除分支或触碰禁区代码。

## 下一阶段建议

建议新建对话，使用 **GPT-5.6 Terra**，单独开展 Shader / RenderTarget / screenshot / viewfinder / CameraCapture 架构阶段。理由是剩余高密度错误已经跨越 GPU pipeline、framebuffer 生命周期、GUI extraction 与后台 capture 调度，需要先按 26.1.2 merged jar 和当前 Blaze3D/Fabric API 重建完整数据流，再分组实现；不应在本线程继续扩张。

下一阶段仍应保持 networking、ExtraData/SavedData、照片格式/像素编码、Frame、相机拍摄算法以及 GameRenderer/EnderMan AW 目标独立审计，避免把渲染架构迁移与数据兼容混在一起。
