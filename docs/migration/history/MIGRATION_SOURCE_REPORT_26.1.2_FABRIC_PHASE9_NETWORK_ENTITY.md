# Exposure 26.1.2 Fabric 迁移报告：第九阶段

> 范围：剩余非渲染实体 API 与 Fabric networking。  
> 结果：本阶段目标源码迁移已完成；`common` 错误由 791 降至 781，Fabric javac 仍被 common 阻断。

## 1. 基线与约束

- 基线日志：`phase9-common-baseline.log`，`common` 为 791 个错误。
- Phase 8 的 `ExtraData` 组合模型、未知字段深拷贝、SavedDataType、旧文件路径和非法旧照片 ID 可逆路径迁移均未修改。
- 现有 payload 的 ID、方向、字段、字段编码顺序和业务 handler 调用没有合并、拆分或改写。
- 渲染、Screen、Shader、截图、取景器、照片像素编码、相机拍摄算法和 11 条旧 Class Tweaker 目标均未处理。
- 基线首层 javac 诊断中，354 条明确属于渲染/UI；持久化错误为 0。本阶段直接相关诊断为网络/handler、`ServerGamePacketListenerMixin`、CameraStand、PhotographFrame 和少量实体外围 API。

## 2. 修改文件与原因

### common

1. `network/packet/PacketDefinition.java`
   - 新增 Fabric play payload 定义载体。
   - codec 类型为 `StreamCodec<? super RegistryFriendlyByteBuf, T>`，同时接纳真正需要 registry 的 codec 和仍可安全使用 `FriendlyByteBuf` 的 codec，不再使用 unchecked cast。
2. `network/packet/C2SPackets.java`
3. `network/packet/S2CPackets.java`
4. `network/packet/CommonPackets.java`
   - 由旧 `TypeAndCodec<? extends FriendlyByteBuf, ...>` 聚合改为强类型 `PacketDefinition`。
   - 列表成员和顺序不变；`query_exposure_data` 继续保持历史上的未注册状态。
5. `network/handler/ClientPacketsHandler.java`
   - 客户端消息由旧 `displayClientMessage(..., false)` 等价迁移为 `sendSystemMessage(...)`。
   - CameraStand 停止控制判断改用公开 `Minecraft#getCameraEntity()`。
   - 两个 shader handler 错误按本阶段限制保留。
6. `network/packet/serverbound/AlbumSignC2SP.java`
   - codec 未变；增加服务器端槽位范围和 author 与实际玩家名一致性校验。
7. `network/packet/serverbound/AlbumSyncNoteC2SP.java`
   - codec 未变；保留 AlbumMenu 校验并增加 `[0, AlbumContent.MAX_PAGES)` 页码校验。
8. `network/packet/serverbound/OpenCameraAttachmentsInCreativePacketC2SP.java`
   - codec 未变；增加 creative 状态和槽位范围校验。
   - 当前 Fabric receiver 已在服务器主线程执行，移除旧 API 时代多余的一次 `server.execute` 排队；有效请求的业务调用仍为同一个 `openCameraAttachments`。
9. `mixin/ServerGamePacketListenerMixin.java`
   - `player.serverLevel()` 改为 26.1 official API 的协变 `player.level()`。
   - target、`HEAD` 注入点、`cancellable`、required 配置和 ItemRenameMenu 业务均不变。
10. `world/entity/CameraStandEntity.java`
    - 旧 `lerpTo` 覆盖迁移为 26.1 的 `lerpPositionAndRotationStep`；仍只同步位置并继续忽略 vanilla 旋转插值，保留 Exposure 自有全精度旋转包的语义。
    - 旧 ItemStack 瞬态实体上下文改走本地适配器。
11. `world/entity/PhotographFrameEntity.java`
    - 移除已不存在的 vanilla `HANGING_ENTITY` predicate 常量引用，改为等价 `instanceof HangingEntity`。
    - 旧 ItemStack 瞬态实体上下文改走本地适配器。
12. `world/item/util/ItemStackEntityContext.java`
    - 为 26.1 已删除的 `ItemStack#set/getEntityRepresentation` 提供非持久、非网络的等价上下文。
    - 使用弱 key 和弱 value；不添加 data component，不进入 ItemStack codec、NBT、packet 或相等性。
13. `world/item/camera/CameraItem.java`
    - tooltip 的 CameraStand 上下文改读 `ItemStackEntityContext`。
    - 服务端 `inventoryTick` 迁移到 `(ItemStack, ServerLevel, Entity, EquipmentSlot)`。
    - 原客户端清理失配 active 标记的逻辑保留为 `clientInventoryTick`，未删除。
14. `mixin/client/LocalPlayerMixin.java`
    - 在既有客户端玩家 tick 注入中遍历玩家容器并调用上述 `clientInventoryTick`，补回 26.1 不再从 `Item.inventoryTick` 触发客户端逻辑后的等价行为。

### fabric

15. `network/fabric/FabricC2SPackets.java`
    - 使用 `PayloadTypeRegistry.serverboundPlay()`。
    - 强类型注册 C2S codec 与 `ServerPlayNetworking` receiver，无 unchecked cast。
16. `network/fabric/FabricS2CPackets.java`
    - 使用 `PayloadTypeRegistry.clientboundPlay()`；强类型注册 S2C codec。
17. `network/fabric/FabricS2CPacketHandler.java`
    - 强类型注册客户端 receiver；仍只从 Fabric client entrypoint 调用。
18. `network/fabric/FabricClientPacketSender.java`
    - 将 `ClientPlayNetworking.send` 隔离到 `@Environment(EnvType.CLIENT)` 类，避免服务器初始化时加载客户端网络实现。
19. `network/fabric/PacketsImpl.java`
    - `sendToServer` 委托给上述 client-only sender；指定玩家、过滤广播、全体广播和范围广播逻辑未变。

## 3. 完整 packet 清单

下表中的字段按**线上编码顺序**列出，而不是只按 record 声明顺序列出。所有 ID 的 namespace 均为 `exposure`。

| ID | 方向 | 线上字段顺序 / codec |
|---|---|---|
| `active_camera_remove` | S2C | `operatorEntityId: VAR_INT` |
| `active_camera_in_hand_set` | S2C | `operatorEntityId: VAR_INT` → `cameraId: CameraId` → `hand: VAR_INT ordinal` |
| `active_camera_on_stand_set` | S2C | `operatorEntityId: VAR_INT` → `cameraId: CameraId` → `cameraStandId: VAR_INT` |
| `camera_stand_set_rotations` | S2C | `entityId: VAR_INT` → `yRot: FLOAT` → `xRot: FLOAT` |
| `camera_stand_stop_controlling` | S2C | `standId: VAR_INT` |
| `shader_apply` | S2C | `shaderLocation: optional Identifier` |
| `clear_rendering_cache` | S2C | 无字段，unit codec |
| `create_chromatic_exposure` | S2C | `id: STRING_UTF8` → `layers: List<ExposureIdentifier>` |
| `exposure_data_changed` | S2C | `id: STRING_UTF8` |
| `unique_sound_play` | S2C | `id: STRING_UTF8` → `entityId: VAR_INT` → `sound: SoundEvent.DIRECT_STREAM_CODEC` → `source: ordinal` → `volume: FLOAT` → `pitch: FLOAT` |
| `unique_sound_play_shutter_ticking` | S2C | `entityId: VAR_INT` → `cameraId: CameraId` → `volume: FLOAT` → `pitch: FLOAT` → `durationTicks: VAR_INT` |
| `unique_sound_stop` | S2C | `id: STRING_UTF8` → `sound: SoundEvent.DIRECT_STREAM_CODEC` |
| `show_exposure_command` | S2C | `frames: List<Frame>` → `negative: BOOL` |
| `exposure_data_response` | S2C | `id: STRING_UTF8` → `result: RequestedPalettedExposure` |
| `shutter_opened` | S2C | 无字段，unit codec |
| `capture_start` | S2C | `templateId: Identifier` → `captureParameters: CaptureParameters` |
| `capture_start_debug_rgb` | S2C | `templateId: Identifier` → `captureProperties: List<CaptureParameters>(max 3)` |
| `export` | S2C | `ids: List<STRING_UTF8>` → `size: VAR_INT ordinal` → `look: VAR_INT ordinal` |
| `export_stop` | S2C | 无字段，unit codec |
| `album_sign` | C2S | `slot: VAR_INT` → `title: STRING_UTF8` → `author: STRING_UTF8` |
| `album_update_note` | C2S | `pageIndex: VAR_INT` → `text: STRING_UTF8` |
| `active_camera_set_setting` | C2S | `setting: CameraSetting` → `encodedValue: byte[<=4096]` |
| `open_camera_attachments` | C2S | `cameraSlotIndex: VAR_INT` |
| `exposure_request` | C2S | `id: STRING_UTF8` |
| `active_camera_release` | C2S | 无字段，unit codec |
| `interplanar_projection_finished` | C2S | `cameraId: CameraId` → `successful: BOOL` → `error: optional TranslatableError` |
| `exposure_data` | C2S | `id: STRING_UTF8` → `exposure: ExposureData` |
| `camera_stand_turn` | C2S | `entityId: VAR_INT` → `yRot: DOUBLE` → `xRot: DOUBLE` |
| `active_camera_deactivate` | C2S + S2C 注册 | 无字段，unit codec；现有调用方只从客户端发往服务器 |
| `query_exposure_data` | 历史上未注册 | `identifier: ExposureIdentifier`；没有发送调用，保留源码但不擅自激活 |

审计结果：声明 ID 共 30 个且全部唯一；参与现有注册的逻辑 ID 为 29 个。迁移前后集合一致。`active_camera_on_stand_set` 的 codec 顺序原本就与 record 字段排列不同，本阶段特意保持原线上顺序。

## 4. payload、codec 与 buffer 迁移

- 每个 payload 原有 `CustomPacketPayload.Type` 与 `Identifier` 未修改。
- 每个 payload 原有 `STREAM_CODEC` 未修改，没有重排或删减字段。
- 没有把 `FriendlyByteBuf` 全局机械替换成 `RegistryFriendlyByteBuf`。
- 原本依赖 registry 的 payload 继续使用 `RegistryFriendlyByteBuf`；纯基础字段 payload 继续使用 `FriendlyByteBuf` codec。`PacketDefinition` 只在注册边界用逆变类型安全地兼容两者。
- `Frame.STREAM_CODEC`、`ExposureIdentifier.STREAM_CODEC`、`ExposureData.STREAM_CODEC` 和 `ExtraData.STREAM_CODEC` 均未修改。
- `ExtraData` 仍直接编码原 `CompoundTag`，没有新包装 key、JSON 或 Java 序列化层。
- 在当前项目实际使用的 Minecraft 26.1.2 merged jar 与 `fabric-networking-api-v1 6.3.1` jar 上执行了独立泛型/API 烟雾编译，新的 registry 与 receiver 签名通过。

Fabric 26.1 的官方迁移表明确将 `playC2S/playS2C` 改为 `serverboundPlay/clientboundPlay`；官方 networking 文档也要求在 receiver 前注册相应 payload 类型：

- [Fabric 26.1 networking](https://docs.fabricmc.net/develop/networking)
- [Porting Fabric API to 26.1](https://docs.fabricmc.net/develop/porting/fabric-api)

## 5. 注册、发送、receiver 与线程边界

- C2S 类型：通用初始化时注册到 `serverboundPlay`，随后注册服务器 receiver。
- S2C 类型：通用初始化时注册到 `clientboundPlay`；客户端 receiver 只在 `ExposureFabricClient` 中注册。
- 客户端专用 `ClientPlayNetworking` 只存在于 client receiver 和 `@Environment(CLIENT)` sender 中。
- 发送给指定玩家仍使用 `ServerPlayNetworking.send(player, packet)`。
- `sendToClients`、`sendToAllClients` 与 `sendToPlayersNear` 的玩家集合、维度检查、排除玩家和三维半径判断均未扩大或改为全服广播。
- CameraStand 新追踪玩家的首次全精度旋转仍由 `ServerEntityMixin#addPairing` 发给该玩家；周期同步仍保持原调用点。
- 当前 Fabric `ServerPlayNetworking` receiver 在服务器线程执行，`ClientPlayNetworking` receiver 在客户端渲染线程执行。因此 handler 继续直接调用，未增加网络线程上的世界访问，也没有重复排队改变 handler 顺序。

### 服务端信任边界

- CameraStand 转向：仍先解析实体，再要求发送者是 operator 或 owner。
- 相机设置/释放：仍只操作服务器记录的 active camera。
- 照片上传：Phase 8 的 `ExposureRepository#validateUpload` 预期 ID 与超时校验未改；客户端不能任意写入未被服务器期待的照片 ID。
- 照片请求：仍经过 Phase 8 的安全路径映射，不把客户端字符串直接拼成越界路径。
- Album note：仍要求当前菜单是 `AlbumMenu`，并新增页码边界校验。
- Album sign：新增槽位边界和 author 身份一致性校验；对应槽位仍必须实际为 AlbumItem。
- 创意附件菜单：新增 creative 状态、槽位边界和槽内 CameraItem 校验。
- `interplanar_projection_finished` 仍只对存在、正在等待且未超时的 CameraInstance 生效；CameraInstance 当前没有显式 owner 字段，因此无法在不扩大设计范围的情况下增加玩家归属核验，此项保留为中等服务端信任风险。

## 6. ServerGamePacketListenerMixin

- 原始目的：截获 vanilla anvil rename packet，在玩家打开 Exposure `ItemRenameMenu` 时复用同一 rename 输入包。
- target 仍为 `ServerGamePacketListenerImpl#handleRenameItem(ServerboundRenameItemPacket)`；26.1.2 本地类签名已核对存在。
- 注入仍为 `HEAD`、`cancellable = true`，mixin 配置仍 `required: true`、`defaultRequire: 1`。
- `PacketUtils.ensureRunningOnSameThread` 的 ServerLevel 参数改为 `player.level()`。
- ItemRenameMenu 有效性判断、名称写入和取消 vanilla handler 的条件不变。
- 该 Mixin 与 Exposure custom payload receiver 不重复，未删除或降级为 optional。

## 7. 非渲染实体 API 结果

- `CameraStandEntity`：新插值覆盖签名有效，继续由服务器权威位置与 Exposure 自有旋转同步驱动；本文件最终 javac 错误为 0。
- `PhotographFrameEntity`：HangingEntity 冲突检测与瞬态 stack 上下文完成迁移；本文件最终 javac 错误为 0。
- ItemStack 实体上下文不会写入 NBT、data component 或网络，不影响 Phase 8 存储格式。
- CameraItem 服务端 tick 与客户端 active 清理均保留。客户端逻辑转移到既有 LocalPlayer tick 注入，是因为 26.1 的 `ItemStack#inventoryTick` 只在 `ServerLevel` 分发到 Item。
- `EntitiesInFrame#getBoundingBoxForCulling` 仍有 1 条 API 错误；它直接影响拍摄实体选择算法，本阶段按限制未用 `getBoundingBox()` 猜测替换。
- CameraItem 剩余 3 条：两条 Screen shift 输入和一条 EnderMan 新签名/旧 Class Tweaker 目标，均属于明确跳过范围。

## 8. 验证结果

### Gradle

- `./gradlew :common:compileJava`
  - 791 → **781**，净减少 **10**。
  - payload 定义、CameraStand、PhotographFrame、ServerGamePacketListenerMixin 本阶段目标错误为 0。
  - 网络目录只剩 `ClientPacketsHandler` 的 2 条 shader 渲染 API 错误，按限制保留。
- `./gradlew :fabric:compileJava`
  - 仍在前置 `:common:compileJava` 失败，Fabric javac 未开始。
  - 没有绕开 common；另外完成了实际 26.1.2/Fabric 6.3.1 jar 的 networking API 烟雾编译。
- `./gradlew clean build --continue`
  - common 仍为 781 条错误。
  - common/fabric 的 access-widener 验证仍报告原有 11 条 RenderType、RenderStateShard、GameRenderer、ItemProperties、EnderMan 旧目标；本阶段未修改。

日志：

- `phase9-common-baseline.log`
- `phase9-network-common-check.log`
- `phase9-entity-common-check.log`
- `phase9-common-compile-final.log`
- `phase9-fabric-compile-final.log`
- `phase9-clean-build-final.log`

### 静态检查

- 30 个声明 packet ID 均唯一；迁移前后集合一致。
- 参与编译源码中的旧 `playC2S` / `playS2C`、旧 TypeAndCodec 聚合和 networking unchecked cast 已清除。
- payload codec 本体未改；没有字段删除、重排、默认填充或空 payload 替代。
- client receiver 只由 client entrypoint 注册；client sender 有显式 client 环境隔离。
- 网络层没有给 ExtraData、Frame、ExposureIdentifier 增加包装结构。
- 未新增 null、no-op、stub、恒真/恒假 handler。`QueryExposureDataC2SP` 中原有的注释实现仍保持未注册、未调用，未作为本阶段绕过手段。

## 9. 旧数据兼容验证夹具规范

当前 common 无法完整编译，未为测试重构主代码。后续可启动测试环境后至少准备以下样本：

1. 非法 Identifier 旧照片 ID：逻辑 ID 使用 `Alice_2025-Frame`，旧文件位于 `data/exposures/Alice_2025-Frame.dat`；验证首次读取迁移到 `_encoded/<UTF-8 hex>.dat` 后逻辑 ID、Frame 引用和网络字段仍为原字符串。
2. 未知 ExtraData：Frame 的 `extra_data` 除已知 key 外加入嵌套 compound、list、byte/int/long array 和未知标量；执行 codec/NBT/packet 往返后逐项深比较。
3. 处理中 Lightroom：保存 Items、SelectedFrame、Progress、PrintTime、PrintedPhotographsCount、AdvanceFrame、PrintMode、LastPlayerId，并验证处理状态和槽位不丢失。
4. CameraStand：含 Camera、CooldownTime、Cooldown、Malfunctioned、HasRedstoneSignal、RedstoneReleaseDelay、Owner；验证实体读取、同步后相机 stack 和故障/冷却状态一致。
5. PhotographFrame：含 Item、IsGlowing、ItemRotation、FrameItem、Size、Facing、Invisible；验证放置方向、尺寸和照片内容一致。
6. `exposure_frame_history.dat`：至少两个玩家 UUID、每人多帧并含未知 ExtraData；验证仍从旧根路径读取且 32 帧上限语义不变。
7. 网络 round-trip：为表中每个 active packet 做 encode→decode 字段逐项相等检查，特别锁定 `active_camera_on_stand_set` 的历史线上顺序和 `Frame/ExtraData/ExposureData` 复杂字段。

## 10. 剩余错误、风险与下一阶段

### 剩余主要类别

- 客户端 UI/渲染、RenderState、模型、Shader、framebuffer、截图和取景器仍是最大错误簇。
- `ClientPacketsHandler` 的 2 条 shader handler 错误属于渲染迁移。
- `EntitiesInFrame` 的 culling box API 需要在相机拍摄算法阶段做语义验证。
- CameraItem 的 Screen shift 输入和 EnderMan 新观察签名仍未处理。
- Fabric 模块自身 javac 仍需在 common 成功后做完整验证。
- 11 条旧 Class Tweaker 目标保持原状。

### 风险判断

- 协议兼容风险：低到中。ID、方向、字段、codec 与顺序未变，但尚不能进行真实双端 round-trip。
- 线程安全风险：低。当前 Fabric receiver 的主线程保证已核对，旧多余服务器排队只在一个 handler 中移除。
- dedicated server 类加载风险：低。客户端 receiver 仅在 client entrypoint，sender 有 `@Environment(CLIENT)` 隔离。
- 服务端信任风险：中。关键槽位、菜单、实体 owner/operator 和照片上传预期校验均保留或加强；Interplanar CameraInstance 缺少显式 owner 是既有设计风险。
- 核心业务误伤风险：低到中。未改照片编码、文件格式、ExtraData/SavedData、packet schema 或相机拍摄算法；瞬态 ItemStack 实体上下文和新插值覆盖仍需游戏内 CameraStand/PhotographFrame 回归。

### 下一阶段建议

先单独处理**客户端非渲染 UI 基础 API**（Screen 输入、消息、控件事件与布局类型），进一步压缩大量前置错误；随后另开渲染专门阶段处理 RenderState、模型、Shader、截图和取景器，并与 11 条相关 Class Tweaker 一起核对。`EntitiesInFrame` 应与相机捕获语义测试放在独立小阶段，不应以机械 bounding-box 替换提前处理。common 能通过后，第一时间运行 Fabric javac、dedicated server 类加载和上述全 packet round-trip 测试。

## 11. 阶段交接

- 已完成：26.1 payload registry 命名迁移、强类型 codec 注册、C2S/S2C receiver、client-only sender、服务器主线程等价处理、关键 C2S 输入校验、ServerGamePacketListenerMixin、CameraStand/PhotographFrame 和 ItemStack 瞬态实体上下文。
- 重要决策：保留 29 个 active ID 和 1 个历史 dormant ID；不激活 `query_exposure_data`；不全局改 buffer 类型；不修改任何 packet codec；不触碰 Phase 8 存储方案。
- 验证：common 781；Fabric javac 未开始；networking API 独立烟雾编译通过；旧 access widener 11 条失败不变。
- 失败尝试/限制：全项目仍因渲染/UI 等源码错误无法进入 Fabric javac，无法启动真实客户端/服务器或运行旧世界与协议 round-trip。
- 下一步：客户端非渲染 UI 基础 API → common 可编译后 Fabric javac/协议测试 → 独立渲染与 Class Tweaker 阶段。
