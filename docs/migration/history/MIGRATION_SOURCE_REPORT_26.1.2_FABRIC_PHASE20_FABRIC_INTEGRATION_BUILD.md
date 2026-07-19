# Minecraft 26.1.2 Fabric Phase 20：Fabric integration、测试迁移与完整构建

## 1. 输入、规则与精确基线

- 工作区未发现 `AGENTS.md`，且 `D:/Codex/Exposure` 不是 Git 仓库；本阶段以文件级修改清单、编译日志和静态审计记录变更。
- 已读取 Phase 19、Phase 18、Phase 17 报告及用户指定的 Phase 19 编译、clean build 与 Access Widener 日志。菜单/网络按需追溯 Phase 9，item-model 染色边界按需追溯 Phase 13；没有全量扫描全部历史报告。
- 实际依赖基线：Gradle 9.4.0、Java 25.0.3、Loom 1.16.3、Fabric API 0.150.0+26.1.2、Forge Config API Port 26.1.4、Minecraft 26.1.2 merged jar `minecraft-merged-c1df423a35`。
- `./gradlew :common:compileJava`：成功，production common 错误 0。
- `./gradlew :fabric:compileJava`：失败，Fabric production 错误仍为 35；完整日志为 `phase20-fabric-baseline.log`。
- 当前 35 条与 Phase 19 完全一致，没有数量差异。

## 2. Fabric 基线 35 条逐条错误表

“语义边界”列中的 `协议` 指业务 packet schema；菜单 open-data 属于独立菜单序列化边界，不计入 Phase 9 packet schema。

| # | 文件:行 | 旧类型/方法 | 26.1.2 实际候选入口 | 组别 | 环境 | 语义边界 | 连锁诊断 |
|---:|---|---|---|---|---|---|---|
| 1 | `ExposureFabric.java:3` | `api.neoforge.v4.NeoForgeConfigRegistry` import | FCAP 26.1.4 `fabric.api.v5.ConfigRegistry` | 1 config | 通用 | 配置生命周期/文件 | 否 |
| 2 | `ExposureFabricClient.java:3` | `api.neoforge.v4.client.ConfigScreenFactoryRegistry` import | FCAP 26.1.4 `fabric.api.v5.client.ConfigScreenFactoryRegistry` | 1 config | client-only | 配置 Screen 生命周期 | 否 |
| 3 | `ExposureFabricClient.java:23` | `client.keybinding.v1.KeyBindingHelper` import | `client.keymapping.v1.KeyMappingHelper` | 2 client API | client-only | 输入注册 | 否 |
| 4 | `ExposureFabricClient.java:25` | `ColorProviderRegistry` import | item-model `ItemTintSource` + `ItemTintSources.ID_MAPPER`；Fabric 23.1.1 已无 Item color provider | 2 client API | client-only | item-model/resource decode | 否 |
| 5 | `ExposureFabricClient.java:27` | `HudRenderCallback` import | `client.rendering.v1.hud.HudElementRegistry.addLast`，回调为 `GuiGraphicsExtractor, DeltaTracker` | 2 client API | client-only | HUD render-state extraction | 否 |
| 6 | `ExposureFabricClient.java:28` | `TooltipComponentCallback` import | `ClientTooltipComponentCallback` | 2 client API | client-only | tooltip 组件映射 | 否 |
| 7 | `LevelRendererMixin.java:9` | `FastColor` import | 26.1.2 `EntityRenderState.outlineColor` 由 `EntityRenderer.extractRenderState` 使用 `ARGB.opaque(teamColor)` 生成 | 4 renderer mixin | client-only | Mixin/render state | 否 |
| 8 | `LevelRendererMixin.java:29` | 旧 `renderLevel(... LightTexture, Matrix4f, ...)` handler | `extractVisibleEntities(Camera, Frustum, DeltaTracker, LevelRenderState)` | 4 renderer mixin | client-only | Mixin descriptor/提取生命周期 | 否 |
| 9 | `PlatformHelperImpl.java:11` | `PacketByteBufs` import | Netty `Unpooled.buffer()` 包装为 `RegistryFriendlyByteBuf` | 3 menu glue | 通用/server | 菜单 open-data 序列化 | 否 |
| 10 | `PlatformHelperImpl.java:12` | `ExtendedScreenHandlerFactory` import | `menu.v1.ExtendedMenuProvider<D>` | 3 menu glue | 通用/server | 菜单 open-data 生命周期 | 否 |
| 11 | `RegisterImpl.java:10` | `ExtendedScreenHandlerType` import | `menu.v1.ExtendedMenuType<T,D>` | 3 menu glue | 通用 | 菜单 open-data codec | 否 |
| 12 | `ExposureFabric.java:74` | `NeoForgeConfigRegistry.register(SERVER, spec)` | `ConfigRegistry.INSTANCE.register(modId, SERVER, spec)` | 1 config | 通用/server | 配置加载/reload/save | 是：#1 |
| 13 | `ExposureFabric.java:75` | `NeoForgeConfigRegistry.register(COMMON, spec)` | `ConfigRegistry.INSTANCE.register(modId, COMMON, spec)` | 1 config | 通用 | 配置加载/reload/save | 是：#1 |
| 14 | `ExposureFabric.java:76` | `NeoForgeConfigRegistry.register(CLIENT, spec)` | `ConfigRegistry.INSTANCE.register(modId, CLIENT, spec)`；仅 spec 注册，不引用客户端类 | 1 config | 通用注册、client 文件类型 | 配置加载/reload/save | 是：#1 |
| 15 | `ExposureFabricClient.java:41` | v4 Screen factory register | v5 `register(String, BiFunction<String,Screen,Screen>)`；`ConfigurationScreen::new` 保持匹配 | 1 config | client-only | 配置 Screen 生命周期 | 是：#2 |
| 16 | `ExposureFabricClient.java:43` | `ColorProviderRegistry.ITEM.register` | 注册 `exposure:camera_glass` ItemTintSource codec，并在 camera model leaves 声明两个 tint source（base white + glass） | 2 client API | client-only | item model/resource decode | 是：#4 |
| 17 | `ExposureFabricClient.java:45` | `KeyBindingHelper::registerKeyBinding` | `KeyMappingHelper::registerKeyMapping` | 2 client API | client-only | 输入注册 | 是：#3 |
| 18 | `ExposureFabricClient.java:63` | `TooltipComponentCallback.EVENT` | `ClientTooltipComponentCallback.EVENT`，原 Photograph 条件/返回保持 | 2 client API | client-only | tooltip 映射 | 是：#6 |
| 19 | `ExposureFabricClient.java:66` | `HudRenderCallback.EVENT` | `HudElementRegistry.addLast(exposure:camera_stand_tooltip, CameraStandTooltip::render)` | 2 client API | client-only | HUD 顺序/抽取 | 是：#5 |
| 20 | `LevelRendererMixin.java:30` | `Camera.getEntity()` | `Camera.entity()`；新设计在 extraction handler 判断 | 4 renderer mixin | client-only | Mixin/render state | 否 |
| 21 | `LevelRendererMixin.java:49` | `FastColor.ARGB32.red` | 不再手工设置 buffer color；提取出的 `EntityRenderState.outlineColor` 保留原版来源 | 4 renderer mixin | client-only | outline render state | 是：#7 |
| 22 | `LevelRendererMixin.java:49` | `FastColor.ARGB32.green` | 同 #21 | 4 renderer mixin | client-only | outline render state | 是：#7 |
| 23 | `LevelRendererMixin.java:49` | `FastColor.ARGB32.blue` | 同 #21 | 4 renderer mixin | client-only | outline render state | 是：#7 |
| 24 | `LevelRendererMixin.java:55` | `Camera.getPosition()` | `Camera.position()`；新提取路径不再手工计算提交坐标 | 4 renderer mixin | client-only | camera render state | 否 |
| 25 | `PlatformHelperImpl.java:48` | `ExtendedScreenHandlerFactory<byte[]>` 类型 | `ExtendedMenuProvider<byte[]>` | 3 menu glue | 通用/server | 菜单 open-data | 是：#10 |
| 26 | `PlatformHelperImpl.java:48` | `new ExtendedScreenHandlerFactory` | `new ExtendedMenuProvider<byte[]>()` | 3 menu glue | 通用/server | 菜单 open-data | 是：#10 |
| 27 | `PlatformHelperImpl.java:49` | 旧 factory override 失配 | `getScreenOpeningData(ServerPlayer): byte[]` | 3 menu glue | 通用/server | 菜单编码 | 是：#10 |
| 28 | `PlatformHelperImpl.java:51` | `PacketByteBufs.create()` | `Unpooled.buffer()` + `RegistryFriendlyByteBuf` | 3 menu glue | 通用/server | registry-aware 菜单编码 | 是：#9 |
| 29 | `PlatformHelperImpl.java:59` | `createMenu` override 失配 | `ExtendedMenuProvider` 继承 `MenuProvider.createMenu`，原委托保持 | 3 menu glue | 通用/server | 服务端权威菜单创建 | 是：#10 |
| 30 | `PlatformHelperImpl.java:64` | `getDisplayName` override 失配 | `ExtendedMenuProvider` 继承 `MenuProvider.getDisplayName`，原委托保持 | 3 menu glue | 通用/server | 菜单标题 | 是：#10 |
| 31 | `RegisterImpl.java:102` | `ExtendedScreenHandlerType<E,byte[]>` 类型 | `ExtendedMenuType<E,byte[]>` | 3 menu glue | 通用 | 菜单 codec/客户端构造 | 是：#11 |
| 32 | `RegisterImpl.java:102` | `new ExtendedScreenHandlerType` | `new ExtendedMenuType<>(factory, BYTE_ARRAY codec)` | 3 menu glue | 通用 | 菜单 codec/客户端构造 | 是：#11 |
| 33 | `IdentifiableResourceReloadListenerWrapper.java:13` | 未实现旧 `reload` 后的新抽象方法 | 实现 `reload(SharedState, Executor, PreparationBarrier, Executor)` | 5 reload | 通用包装器；当前用于 client resource | SharedState/线程/barrier | 否 |
| 34 | `IdentifiableResourceReloadListenerWrapper.java:27` | 旧六参数 `reload` 的 `@Override` | 新四参数 override | 5 reload | 同 #33 | SharedState/线程/barrier | 是：#33 |
| 35 | `IdentifiableResourceReloadListenerWrapper.java:29` | 向 delegate 调用旧六参数 `reload` | 原样转发同一个 `SharedState`、prepare executor、barrier、apply executor | 5 reload | 同 #33 | SharedState/异常传播 | 是：#33 |

### 2.1 分类汇总

| 组 | 诊断数 | 根错误/连锁 | 说明 |
|---|---:|---:|---|
| 1 FCAP initializer/config | 6 | 2 / 4 | v4 → v5；Screen 只在 client initializer |
| 2 客户端 API/provider/HUD | 8 | 4 / 4 | key mapping、item tint source、tooltip、HUD extraction |
| 3 menu/network glue | 11 | 3 / 8 | 当前 Fabric menu open-data payload；不触碰 Phase 9 packet |
| 4 LevelRendererMixin | 7 | 3 / 4 | immediate render → entity render-state extraction |
| 5 SharedState reload | 3 | 1 / 2 | 新 reload 合约完整转发 |
| **总计** | **35** | **13 / 22** | 与 Phase 19 一致 |

## 3. 已确认的当前 API 边界（修改前）

- FCAP `ConfigRegistry` 的三参数 overload 生成 `%s-%s.toml`，因此现有默认文件名仍为 `exposure-server.toml`、`exposure-common.toml`、`exposure-client.toml`。COMMON/CLIENT 注册时立即从 Fabric config 目录加载；SERVER 在 `SERVER_STARTING` 从 world `serverconfig`（并以全局 config 为 fallback）加载，停止时 unload。文件缺失会创建；解析/重建仍失败时抛 `RuntimeException`，不会静默继续。
- FCAP v5 Screen factory公开入口仍存在，参数为 mod id 与 parent Screen，且 FCAP 自己用独立 client entrypoint；Exposure 的 Screen 注册必须保留在 `ExposureFabricClient`。
- 源码中的 `net.neoforged.fml.config.ModConfig` 与 `net.neoforged.neoforge.client.gui.ConfigurationScreen` 是 FCAP 26.1.4 jar 公开的兼容 API 类型，并非新增 NeoForge loader/dependency；后者只在 Fabric client source/entrypoint 中解析。
- 当前 Fabric API 菜单实现通过内部 open-screen custom payload 传输 `ExtendedMenuType` 的 `StreamCodec<RegistryFriendlyByteBuf,D>`；Exposure 将保持 `D=byte[]`，从而不修改 common menu 构造签名或任何字段顺序。
- 26.1.2 `LevelRenderer` 已分离 `extractLevel`/`renderLevel`。旧 Mixin 的业务目的，是 Camera Stand 成为 camera entity 时补回 vanilla 明确跳过的 LocalPlayer。当前等价点是 `extractVisibleEntities`：加入完整 `EntityRenderState`，之后由原版 `submitEntities` 提交；outlineColor、光照、name tag、shadow 与 fire 状态均由原版 extractor 生成。
- `PreparableReloadListener.SharedState` 持有本次 reload 的 ResourceManager 与 identity-keyed listener state。wrapper 不创建/覆盖 key，只把同一个 SharedState 与两个 executor、barrier 转发给 delegate；异常保持在 future 中传播。

## 4. 修改文件与原因

- `fabric/src/main/java/io/github/mortuusars/exposure/fabric/ExposureFabric.java`
  - FCAP 配置注册从已删除的 NeoForge 命名 v4 入口迁移到 Fabric v5 `ConfigRegistry`；SERVER、COMMON、CLIENT 三个现有 spec 和注册顺序不变。
- `fabric/src/main/java/io/github/mortuusars/exposure/fabric/ExposureFabricClient.java`
  - 配置 Screen 迁移到 FCAP v5 client registry；KeyMapping、tooltip、HUD、item tint 与 client reload listener 迁移到 26.1.2 实际 API。
- `common/src/main/resources/assets/exposure/items/camera.json`
  - 在 14 个实际 camera model leaf 上声明两个 tint source：index 0 为不改变底色的常量，index 1 为 camera glass 动态 tint；模型选择树、model ID 和 predicate 均未改变。
- `fabric/src/main/java/io/github/mortuusars/exposure/fabric/PlatformHelperImpl.java`
  - 服务器菜单打开入口迁移到 `ExtendedMenuProvider<byte[]>`，仍用 registry-aware buffer 执行原 writer，随后由当前 Fabric menu open payload 传输字节。
- `fabric/src/main/java/io/github/mortuusars/exposure/fabric/RegisterImpl.java`
  - 扩展菜单类型迁移到 `ExtendedMenuType<E, byte[]>`；保持 `BYTE_ARRAY` envelope、原客户端 menu factory 和 common 服务边界。
- `fabric/src/main/java/io/github/mortuusars/exposure/fabric/mixin/LevelRendererMixin.java`
  - 从已经不存在的即时 `renderLevel` 实体绘制路径迁移到 26.1.2 `extractVisibleEntities` render-state 抽取点。
- `fabric/src/main/java/io/github/mortuusars/exposure/fabric/resources/IdentifiableResourceReloadListenerWrapper.java`
  - 实现 `prepareSharedState` 与四参数 SharedState reload 合约，并透明转发 future、executor、barrier 和异常。
- `fabric/src/main/java/io/github/mortuusars/exposure/fabric/resources/ExposureFabricClientReloadListener.java`
  - listener ID 改由当前 resource-loader 注册入口显式提供；清理实现仍继承 common listener。
- `common/src/test/java/io/github/mortuusars/exposure/core/util/CodecsTests.java`
  - 用 26.1.2 `IntTag.intValue()`、`ByteTag.byteValue()` 和实际 `ListTag` codec 结果替代删除的 getter；补足 decode 后数组断言。
- `common/src/test/java/io/github/mortuusars/exposure/core/warehouse/server/FrameHistoryTests.java`
  - 旧 `save/load` 外壳改为生产 `ExposureFrameHistory.CODEC` + `NbtOps.INSTANCE` 的真实 encode/parse round-trip；生产持久化代码未加测试专用兼容入口。
- `common/build.gradle`
  - 增加 Gradle 9.4 test worker 所需的 `testRuntimeOnly org.junit.platform:junit-platform-launcher`；不改变测试 source set 或测试选择。

工作区不是 Git 仓库，因此不能用 commit diff 还原变更范围；以上列表由本阶段逐组编辑记录、最终源码与构建输出交叉核对。`.gradle-home` 是为绕开 Windows Unicode 用户目录下 Java argfile 解码问题建立的工作区级 Gradle 运行环境，不属于生产源码。

## 5. 分组与最终验证

### 5.1 Group 1：Fabric initializer 与 FCAP 26.1.4

配置注册现在使用：

```java
ConfigRegistry.INSTANCE.register(Exposure.ID, ModConfig.Type.SERVER, Config.Server.SPEC);
ConfigRegistry.INSTANCE.register(Exposure.ID, ModConfig.Type.COMMON, Config.Common.SPEC);
ConfigRegistry.INSTANCE.register(Exposure.ID, ModConfig.Type.CLIENT, Config.Client.SPEC);
```

- 三参数 overload 继续生成 `exposure-server.toml`、`exposure-common.toml`、`exposure-client.toml`；分类、spec、配置对象、默认值和注册顺序没有变化。
- COMMON/CLIENT 在注册时从 Fabric config 目录加载；SERVER 在 server starting 时从世界 `serverconfig` 加载并在停止时 unload，沿用 FCAP 生命周期。
- FCAP 会创建缺失文件并尝试修正无效值；解析/重建仍失败会抛出异常，没有静默回退后继续。
- 配置 Screen 使用 FCAP v5 `ConfigScreenFactoryRegistry.INSTANCE.register(Exposure.ID, ConfigurationScreen::new)`，只从 `ExposureFabricClient` 调用。
- `ExposureFabric` 没有引用 `Screen`、`Minecraft`、`net.minecraft.client` 或 Fabric client API；没有新增 Forge/NeoForge 依赖或反射。

结果：35 → 29，`phase20-group1-config-initializer.log`。

### 5.2 Group 2：client API、provider 与事件

- KeyMapping：使用 `KeyMappingHelper::registerKeyMapping`。ID 仍为 `key.exposure.camera_controls`，默认键仍为 `UNKNOWN`（未绑定），category 仍为 `exposure:main`。
- item color：Fabric 26.1.2 已没有旧 `ColorProviderRegistry.ITEM`。新增 client-only `CameraGlassTintSource implements ItemTintSource`，codec 注册为 `exposure:camera_glass`，实际颜色仍调用 `CameraItem.getGlassTintColor(stack, 1)`。14 个 camera model leaf 加入两个 tint slot，保持原 tint index 1 的玻璃语义。
- tooltip：迁移到 `ClientTooltipComponentCallback.EVENT`；Photograph 条件、组件内容和返回顺序不变。
- HUD：迁移到抽取式 `HudElementRegistry.addLast(exposure:camera_stand_tooltip, CameraStandTooltip::render)`；回调仍是 `GuiGraphicsExtractor, DeltaTracker`，保持 Phase 18 的 render-state extraction，没有恢复即时 GuiGraphics 绘制。
- client tick、world lifecycle、connection、entity renderer、block entity renderer、client command 与其原注册顺序保留；没有删除回调、空 lambda 或重复注册。
- client resource reload 在本组保持 client initializer 边界，具体 SharedState 迁移见 5.5。

结果：29 → 21，随后 common 与 Fabric 均可继续编译；`phase20-group2-client-events.log`。

### 5.3 Group 3：菜单 open-data 与 network glue

#### 通用 envelope

- 服务端 `PlatformHelperImpl` 建立 `RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess())`，调用原 `Consumer<RegistryFriendlyByteBuf>` writer，复制为 `byte[]` 后在 `finally` 释放 buffer。
- `ExtendedMenuProvider<byte[]>` 仍委托原 `MenuProvider.createMenu` 和 `getDisplayName`，并由 `ServerPlayer.openMenu` 走原版/Fabric 当前 open-screen payload。
- 注册端使用 `ExtendedMenuType<E, byte[]>` 和 `ByteBufCodecs.BYTE_ARRAY.mapStream(Function.identity())`。客户端收到数据后重新包装为带 `RegistryAccess` 的 `RegistryFriendlyByteBuf`，调用原 common `MenuFactory`，随后在 `finally` 释放。
- `byte[]` 只是 Fabric menu open payload 的 envelope；内部字段仍由原 writer/reader 逐字段编码。没有把菜单数据并入 Phase 9 业务 packet，也没有用字符串、JSON、Java serialization、null、空 payload 或固定默认值绕过。
- 当前 `BYTE_ARRAY` codec 没有额外人为降低上限；读侧长度由 open-screen payload 的可读字节边界约束。所有实际菜单字段均为固定长度或原有 `ItemStack` codec。

#### 菜单审计表

| menu type ID | 服务端打开入口/权威 provider | 附加字段线上顺序与 codec | 客户端构造与 Screen | 验证/错误处理 |
|---|---|---|---|---|
| `exposure:camera_in_hand` | `CameraItem` 创建 provider；`ServerPlayer.openMenu` | 1. camera `slotIndex`: `INT`；2. `openedFromGUI`: `BOOL` | 同序读取并构造 camera-in-hand menu；`CameraAttachmentsScreen` | 构造时检查 slot 仍为 camera；`stillValid` 比较 CameraId；无效数据沿原路径失败 |
| `exposure:camera_on_stand` | `CameraStandEntity` provider | 1. stand `entityId`: `INT` | 按 entity ID 查找并构造；`CameraAttachmentsScreen` | entity 不是 `CameraStandEntity` 时明确抛错；`stillValid` 检查 camera 与交互距离 |
| `exposure:album` | `AlbumItem` provider | 1. album slot: `VAR_INT` | `AlbumMenu`；`AlbumScreen` | 检查 slot 中为 AlbumItem；`stillValid` 保持原 item/slot 验证 |
| `exposure:signed_album` | 当前没有 server open 调用；历史 menu type 仍注册 | 1. album slot: `VAR_INT` | factory 仍可构造 `SignedAlbumMenu`；没有 `MenuScreens` 注册；实际 `SignedAlbumItem` 直接打开 client-only `AlbumViewScreen` | 这是迁移前即存在的 dormant 类型；未伪造 server path。`SignedAlbumMenu.stillValid` 检查 `AlbumItem` 而非 SignedAlbumItem 的历史不一致留作运行时审计 |
| `exposure:lectern_album` | `LecternMixin`/lectern provider | 1. 非空 album stack: `ItemStack.STREAM_CODEC` | reader 使用 `ItemStack.OPTIONAL_STREAM_CODEC`；`LecternAlbumScreen` | merged jar 反编译确认 `STREAM_CODEC` 在拒绝 empty 后委托 OPTIONAL wire codec，因此合法非空值线上兼容；menu 委托 lectern/container validity。写读接受域不完全对称，列为运行时风险 |
| `exposure:lightroom` | Lightroom block/menu provider | 1. `BlockPos`: `writeBlockPos` | `readBlockPos` 后查找 block entity；`LightroomScreen` | block entity 类型错误明确抛错；`stillValid` 由对应 block entity/menu 保持 |
| `exposure:item_rename` | item rename provider | 1. slot: `INT` | 同序读取；`ItemRenameScreen` | 检查目标 stack 非空；`stillValid` 保持同一 item class/slot 语义 |

没有菜单附加数据依赖 UUID、Frame、Camera 设置对象或 Phase 9 custom payload；相机菜单只传原有 slot/bool 或 stand entity ID。所有 7 个 menu ID 保持。Screen 注册只在 `ExposureFabricClient`，menu type 与 server open glue 不引用 Screen 类。

结果：21 → 10，`phase20-group3-menu-network-glue.log`。

### 5.4 Group 4：LevelRendererMixin

旧用途不是 Phase 17 screenshot/RenderTarget 或 Phase 18 Viewfinder overlay：vanilla 在 camera entity 不是 LocalPlayer 时明确跳过 LocalPlayer，而 Camera Stand 视角需要把本地玩家补回世界实体渲染。旧 Mixin 在 `renderLevel` 中即时调用 renderer，并手工处理坐标和 outline buffer。

Minecraft 26.1.2 merged jar 的真实边界为：

```text
renderLevel(
  GraphicsResourceAllocator, DeltaTracker, boolean, CameraRenderState,
  Matrix4fc, GpuBufferSlice, Vector4f, boolean, ChunkSectionsToRender
)V

extractVisibleEntities(
  Camera, Frustum, DeltaTracker, LevelRenderState
)V
```

新 Mixin 使用完整 official descriptor 注入 `extractVisibleEntities` 在调用 `ClientLevel.entitiesForRendering()` 之前的同一实体抽取时机。仅当 `camera.entity()` 是 `CameraStandEntity` 时：

1. 保持 tick 0 时 `xOld/yOld/zOld` 初始化行为；
2. 按 frozen state 和 `DeltaTracker` 取得 partial tick；
3. 调用原版 `EntityRenderDispatcher.extractEntity` 得到明确类型 `EntityRenderState`；
4. 添加到 `LevelRenderState.entityRenderStates`；
5. 若状态 glowing 且 outlines 启用，设置 `haveGlowingEntities`。

outline color、光照、火焰、阴影和 name tag 现在由原版 entity state extractor 产生，不再手工复制旧 buffer 逻辑。Mixin 配置仍是 `required: true`、`defaultRequire: 1`，没有改 optional、没有降低 require；它只列在 Fabric mixin JSON 的 `client` 数组。common 没有同目标重复注入。

结果：10 → 3，`phase20-group4-level-renderer-mixin.log`。

### 5.5 Group 5：SharedState resource reload

`IdentifiableResourceReloadListenerWrapper` 现在：

- 将 `prepareSharedState(SharedState)` 原样委托给被包装 listener；
- 将同一个 `SharedState`、preparation executor、`PreparationBarrier` 和 apply executor 传给 delegate 的四参数 `reload`；
- 直接返回 delegate future，因此 prepare/apply 顺序、barrier 和异常传播不变；
- 不读取或写入任何 SharedState key，不会覆盖其他 listener/mod 的 identity state。

client listener 仍为 `ExposureFabricClientReloadListener`，ID 仍为 `exposure:clear_client_exposures_cache`，注册到 `ResourceType.CLIENT_RESOURCES`。common 的 `SimplePreparableReloadListener<Boolean>` 仍在 preparation executor 上 prepare，跨 barrier 后在 apply executor 调用 `ClientEvents.resourcesReloaded()`，继续取消 capture 并清理 exposure store、render、image caches。原代码没有显式 listener dependency；当前注册顺序保持，也没有把 client listener 放进 dedicated server data reload。

结果：3 → 0，`phase20-group5-resource-reload.log`。至此 Fabric production 35 条全部消除。

### 5.6 测试迁移

生产源码清零后，`:common:compileTestJava` 的实际 baseline 与预期相同，共 10 条：

| 组 | 数量 | 迁移 |
|---|---:|---|
| `IntTag.getAsInt` | 4 | 对实际 codec 结果 `ListTag` 中的 `IntTag` 使用 `intValue()`，保持 32 位有符号断言 |
| `ByteTag.getAsByte` | 4 | 对 `ByteTag` 使用 `byteValue()`，保持 8 位有符号断言 |
| Frame history 旧 save/load | 2 | 使用 `ExposureFrameHistory.CODEC.encodeStart/parse(NbtOps.INSTANCE)` 做结构断言和双向 round-trip |

Frame history 测试继续覆盖 UUID 根 key、UUID 对应 Frame 列表、Frame identifier 数据结构、写后读与读后写；原 32 帧限制测试保留。没有改变生产 NBT key、Codec、SavedDataType、文件路径、未知 ExtraData 兼容语义或 Frame history 算法，也没有传 null RegistryAccess。

Gradle 9.4 启动 JUnit Platform 时还需要显式 launcher runtime，因此在测试依赖中加入 `junit-platform-launcher`。最终执行 39 tests，0 failures、0 errors、0 skipped；`phase20-common-test-final.log` 与 `phase20-common-test-run.log`。

### 5.7 最终构建矩阵

| 命令 | 结果 | 日志 |
|---|---|---|
| `./gradlew :common:compileJava` | 成功 | `phase20-common-compile-final.log` |
| `./gradlew :fabric:compileJava` | 成功 | `phase20-fabric-compile-final.log` |
| `./gradlew :common:compileTestJava` | 成功 | `phase20-common-test-compile-final.log` |
| `./gradlew :fabric:compileTestJava` | 任务存在，`NO-SOURCE` | `phase20-fabric-test-compile-final.log` |
| `./gradlew :common:validateAccessWidener` | 成功 | `phase20-common-accesswidener.log` |
| `./gradlew :fabric:validateAccessWidener` | 成功 | `phase20-fabric-accesswidener.log` |
| `./gradlew clean build --continue` | 成功，包含 tests | `phase20-clean-build-final.log` |
| `./gradlew build` | 成功 | `phase20-build-final.log` |

产物为 `common/build/libs/exposure-common-26.1.2-1.9.18.jar` 与 `fabric/build/libs/exposure-fabric-26.1.2-1.9.18.jar`（以及 sources jars）。本阶段按要求没有启动 `runClient` 或 `runServer`。

## 6. 环境与 dedicated server 隔离静态审计

| 检查 | 结果 |
|---|---|
| 通用 initializer 是否引用 client 类/API | `ExposureFabric` 无 `net.minecraft.client`、Screen、renderer 或 Fabric client API 引用 |
| 配置 Screen | 仅 `ExposureFabricClient` 注册；通用 initializer 只注册 spec |
| KeyMapping、tint、tooltip、HUD、renderer、client reload | 均从 client entrypoint 注册 |
| menu type/server open | `PlatformHelperImpl`、`RegisterImpl` 不引用 Screen；Screen factory 注册在 client entrypoint |
| client Mixin | `LevelRendererMixin` 只在 mixin JSON `client` 数组；`required`/`defaultRequire` 保持 |
| reload 环境 | cache listener 只注册到 `CLIENT_RESOURCES`，没有进入 server data reload |
| dedicated server client class 解析 | 通用初始化路径未发现通向 LevelRenderer、Minecraft client、Screen、Shader 或 client event 类的直接引用 |
| Environment 注解 | 未新增错误的跨环境注解依赖；隔离依赖 Fabric 独立 `main`/`client` entrypoint 与 client mixin 列表 |
| common Service 绑定 | common、Fabric compile 和完整 build 证明 Fabric 实现仍完整绑定；未恢复 Architectury |

这是静态/链接期结论；真正的 dedicated server classloader 验证留到 Phase 21 启动测试。

## 7. 协议、ID、资源与业务语义审计

- 声明的 custom packet ID 仍为 30 个且唯一；活跃 `PacketDefinition` 集合仍为 29 个。
- `query_exposure_data` 仍只存在于历史 packet 类，没有加入 C2S definitions 或 Fabric receiver 注册。
- Phase 9 packet 的 ID、方向、字段顺序和 `StreamCodec` 均未修改；菜单 open-data 仍是独立 vanilla/Fabric 边界。
- 7 个 menu ID 和各自字段线上顺序保持，详见 5.3。
- registry ID、已有配置文件名、KeyMapping ID/category、translation key、texture ID 与 reload listener ID 保持。
- 为当前 API 增加两个必要的 client 技术 ID：`exposure:camera_glass`（ItemTintSource codec）和 `exposure:camera_stand_tooltip`（HUD element）。它们不替换既有业务/资源 ID，也不改变 packet 或保存格式。
- Phase 13 `ExtraModelKey` 和 item model 选择树保持；唯一资源改动是给 14 个既有 camera leaf 声明 tint source。
- Phase 17 的 19 个 post effect JSON 文件未修改；ShaderManager、RenderTarget 与异步 ScreenshotReadback ownership 未回退。
- Phase 18 的 Viewfinder、FOV、CameraCapture、EntitiesInFrame 和 EnderMan 路径未修改或回退。
- ExtraData、SavedData、Frame、ExposureData、照片路径/格式、像素编码、调色板、曝光、胶片与相机业务算法均未修改。

## 8. 失败尝试、环境问题与遗留风险

### 8.1 已解决的失败尝试

1. 最初尝试在 common item-model 初始化类中注册 tint codec；common classpath 中 `ItemTintSources.ID_MAPPER` 不是 public，而 Fabric Access Widener 后的 client classpath 才允许访问。最终把 codec 注册移到 `ExposureFabricClient`，没有扩大 common 抽象或使用反射。
2. Windows 用户目录包含中文字符。Gradle 9.4 将 test worker Java 参数写到该目录的 UTF-8 argfile，而 Java launcher 按系统代码页读取，导致 `GradleWorkerMain` 在 JUnit 启动前 `ClassNotFoundException`。`chcp 65001` 无效。最终使用工作区 ASCII 路径 `D:\Codex\Exposure\.gradle-home`，顶层目录为真实目录、其缓存子目录用 junction 复用原缓存，测试正常启动。
3. test worker 启动后暴露缺少 JUnit Platform launcher runtime，补入标准 test runtime dependency。
4. 26.1.2 `NbtOps` 对这些 codec 的运行时结果为元素化 `ListTag`，不是旧测试假定的 primitive array tag；测试改为检查真实 tag 元素并执行 decode round-trip。
5. Frame history 首轮结构断言使用精确 SNBT 字符串，UUID key 在当前输出中带引号，导致 2 个断言失败；改为结构化 NBT 断言，避免把无业务意义的文本渲染差异当作持久化格式失败。

诊断过程保存在 `phase20-common-test-diagnostic.log`；最终没有通过禁用测试、排除 source set 或修改生产格式来消除错误。

### 8.2 Phase 21 必须验证的风险

- FCAP 三类配置的首次创建、reload/save、错误报告，以及配置 Screen 从 Mod Menu/入口打开。
- camera tint codec 的资源 decode、14 个模型分支的 glass tint 与 GUI/手持视觉。
- 7 个菜单的 server/client open-data 对称；尤其是 dormant `signed_album` menu type 和 lectern album 写读接受域不完全一致。
- `LevelRendererMixin` 的 descriptor/注入点实际 apply，以及 Camera Stand 视角下 LocalPlayer、outline、light/name/shadow/fire 状态。
- client resource reload 的实际顺序、异常传播和 cache/capture 清理。
- dedicated server 启动时是否存在工具链静态检查未触发的 client classloader 解析。
- 旧世界/旧仓库数据通过当前 SavedData/Codec 的真实 round-trip。
- `.gradle-home` 是本机 Unicode 路径兼容措施；换机或 CI 可用普通 ASCII `GRADLE_USER_HOME`，不应把 junction 当成项目运行依赖提交。

## 9. Phase 21 运行时验证计划与模型建议

本阶段 production、tests 与完整 build 已清零；下一阶段应新建对话，只做首次运行时验证，不再重开已完成的编译迁移。建议使用 **GPT-5.6 Sol**：Phase 21 涉及 required Mixin、渲染状态、Shader 和跨 client/server 生命周期，优先使用旗舰推理能力。若后续只是重复固定测试矩阵且更看重成本，可用 Terra。依据：[OpenAI 当前 GPT-5.6 模型指南](https://developers.openai.com/api/docs/guides/latest-model.md)。

Phase 21 顺序：

1. 短时 `runClient` 启动，记录首个确定性失败，不长期挂起。
2. 审计全部 Mixin apply，重点检查 required `LevelRendererMixin` descriptor 与注入次数。
3. 触发资源 reload，确认 listener 顺序、异常传播和 19 个 Shader/post effect 全部编译。
4. 从主菜单进入新世界和旧世界，检查注册、配置与资源加载生命周期。
5. 检查 dynamic photo、album/camera/lightroom GUI 与 camera glass tint 视觉。
6. 检查 Viewfinder overlay、FOV、Camera Stand 控制与退出恢复。
7. 分别验证 direct capture 与 background capture、readback、取消和资源 reload 中断。
8. 验证 Camera Stand、本地玩家补绘、实体 outline 与 Frame renderer。
9. 短时 dedicated server 启动，并检查日志/类加载中没有 `net.minecraft.client`、Screen、LevelRenderer 或 Shader 类误加载。
10. 用旧世界、旧照片仓库和 Frame history 做读写后重载 round-trip，比较 UUID、Frame 上限和未知 ExtraData 字段。

## 10. 阶段交接摘要

1. **已完成内容**：35 条 Fabric production javac 错误、10 条 common test javac 错误全部清零；FCAP、client API、7 个菜单、LevelRendererMixin、SharedState reload 和 JUnit runtime 已迁移。
2. **关键决策**：菜单继续使用 `byte[]` open-data envelope 保存原 registry-aware writer/reader；LocalPlayer 补绘迁到原版 entity render-state extraction；tint codec 留在 client initializer；测试直接走生产 Codec。
3. **验证结果**：common/Fabric production、common tests（39/39）、Fabric test `NO-SOURCE`、双 Access Widener、`clean build --continue` 和 `build` 全部成功。
4. **失败尝试**：common tint 注册不可见、Unicode Gradle worker argfile、缺 launcher、NBT 实际 ListTag 与 SNBT 文本断言问题均已解决并记录。
5. **遗留问题**：没有已知编译阻塞；配置/菜单/Mixin/reload/视觉/dedicated server/旧数据仍需首次运行时验证，不能由本阶段构建结果替代。
6. **下一步**：新建 Phase 21 对话，按第 9 节清单使用 GPT-5.6 Sol 执行短时 client/server 与功能矩阵。
