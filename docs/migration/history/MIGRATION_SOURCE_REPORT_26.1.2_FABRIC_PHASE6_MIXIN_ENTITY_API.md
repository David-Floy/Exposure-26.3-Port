# Exposure 26.1.2 Fabric 第六阶段：Mixin 与实体 / 物品 API

日期：2026-07-08

## 阶段结论

本阶段只迁移了非渲染、非网络、非存储的低风险 Mixin 与实体、物品、方块和按键 API。

- common Java 错误由 954 条下降到 899 条，减少 55 条。
- Mixin 路径错误由 26 条下降到 21 条。
- 剩余 21 条 Mixin 错误全部属于客户端渲染 Mixin 或网络线程 Mixin，本阶段按限制保留。
- `world/item` 路径错误由 42 条下降到 30 条。
- `world/entity` 路径错误由 64 条下降到 35 条。
- `world/block` 路径错误由 28 条下降到 24 条。
- 本阶段目标中的 `KeyboardHandler` / `client/input/Key.java` 错误归零。
- fabric javac 仍被 common 的 899 条错误阻断。
- 没有修改照片保存、图像编码、frame 数据结构、网络协议、渲染管线或 NBT 存储设计。

## 基线错误归类

初始 `:common:compileJava` 完整报告 954 条错误：

| 类别 | 基线数量 |
|---|---:|
| Mixin 路径 | 26 |
| `world/item` | 42 |
| `world/entity` | 64 |
| `world/block` | 28 |
| 本阶段按键输入目标 | 3 |
| 渲染/UI（跳过，类别有重叠） | 610 |
| NBT/存储（跳过，类别有重叠） | 41 |
| 网络（跳过，类别有重叠） | 5 |

可机械确认的主要问题包括 Player/ItemCombinerMenu 超类构造器、ServerPlayer drop 描述符、Minecraft setLevel 参数、ItemStack cooldown、EntityType tag、Biome precipitation、世界 clock、Entity server-side damage/drop、KeyMapping Category/KeyEvent 等。

## 已处理的 Mixin target / signature

### `LocalPlayerMixin`

`Player` 构造器已从旧 `(Level, BlockPos, float, GameProfile)` 变为 `(Level, GameProfile)`。Mixin 超类构造器同步调整；它只用于满足 Mixin 类编译，不改变 LocalPlayer 创建逻辑。

### `ServerPlayerMixin`

- 同步调整 Player 超类构造器。
- `ServerPlayer.drop(boolean)` 当前描述符为 `(Z)V`，回调由 `CallbackInfoReturnable<Boolean>` 改为 `CallbackInfo`。
- 原有 `ServerEvents.itemDrop(...)` 调用和注入时机 `HEAD` 保持不变。

### `MinecraftMixin`

`Minecraft.setLevel` 当前签名为 `setLevel(ClientLevel)`。移除已不存在的 `ReceivingLevelScreen.Reason` 参数和 import，保留 level unload 判断与 `ClientEvents.levelUnloaded()` 调用。

该文件中的 GameRenderer、RenderTarget、后台截图相关注入没有修改。

### `AnvilMenuMixin`

`ItemCombinerMenu` 构造器新增 `ItemCombinerMenuSlotDefinition`。Mixin 构造器传入与 26.1.2 原版 AnvilMenu 相同的两个输入槽和一个结果槽定义：

- input 0: `(27, 47)`
- input 1: `(76, 47)`
- result 2: `(134, 47)`

Interplanar Projector 的命名长度判断、配置判断和 `validateName` wrap operation 未改变。

### Mixin 配置审计

- `exposure-common.mixins.json` package、required、client/mixins 列表均未修改。
- `PlayerMixin`、`ServerPlayerMixin`、`client.LocalPlayerMixin`、`client.MinecraftMixin`、`AnvilMenuMixin` 仍在原配置中。
- `fabric/src/main/resources/exposure.mixins.json` 未修改。
- required Mixin 没有改为 optional，也没有移除任何核心 Mixin。

Mixin 源码通过 javac 不代表所有 runtime injection point 已验证；当前仍需等 common/fabric 可完整启动后执行 Mixin apply 测试。

## 已处理的实体 / 物品 / 相机外围 API

### `CameraItem`

- Player action-bar 消息：`displayClientMessage(..., true)` 等价迁移为 `sendOverlayMessage(...)`。
- inventory slot access：旧 `SlotAccess.forContainer` 改为 `SlotAccess.of`，getter/setter 仍直接操作原 inventory slot。
- entity type tag：`EntityType.is(tag)` 改为 `builtInRegistryHolder().is(tag)`。
- cooldown：按 26.1.2 API 使用同一个 camera `ItemStack` 执行 `isOnCooldown`、`getCooldownPercent`、`addCooldown`，保留 cooldown group 语义。
- ServerPlayer level：`serverLevel()` 改为 `level()`。
- 时间：旧 `getDayTime()` 对应 26.1 world clock，改用当前维度的 `getDefaultClockTime()`。
- 降水：旧 Biome 单参数调用改为 `Level.precipitationAt(BlockPos)`。

没有修改 shutter、timer、film、capture、frame 创建或照片生成流程。

### `CameraStandEntity`

- 可骑乘矿车判断改为 `AbstractMinecart.isRideable()`。
- `interact(Player, InteractionHand)` 增加 26.1.2 的 hit location `Vec3` 参数；原交互分支不变。
- action-bar 消息迁移为 `sendOverlayMessage`。
- `hurt(DamageSource, float)` 迁移为 server-side `hurtServer(ServerLevel, DamageSource, float)`。
- invulnerability 使用新的基础实体检查 `isInvulnerableToBase`。
- kill、spawnAtLocation 和掉落逻辑显式使用传入的 `ServerLevel`。
- `isControlledByLocalInstance` 对齐为 `isLocalClientAuthoritative`，原 operator/local-player 判断不变。

伤害、移除相机、创造模式、掉落规则和破坏阈值未改变。

### `PhotographFrameEntity`

- `interact` 增加 hit location `Vec3`。
- 伤害迁移到 `hurtServer`，保留“先移除照片、爆炸再破坏框体”的原逻辑。
- `dropItem` 增加 `ServerLevel`，spawn 与 gamerule 判断使用同一 server level。
- `Direction.getNormal()` 改为 `getUnitVec3i()`。

照片/frame NBT 读写未处理。

### 其他外围 API

- `CameraStandItem.moveTo(...)` 改为等价的 `snapTo(...)`。
- `CameraInHand` 遍历旧私有 inventory `items` 改为 `getNonEquipmentItems()`；仍只扫描原非装备背包槽。
- `DebugCommand` 中 ServerPlayer level accessor 改为 `level()`。
- `FlashBlock` 的 skylight 和 waterlogged `updateShape` 签名迁移到 `LevelReader`、`ScheduledTickAccess`、`RandomSource` 新形式，water tick 与 super 调用保持不变。
- `LightroomBlock.getAnalogOutputSignal` 增加 26.1.2 的 Direction 参数，原信号计算不变。

Lightroom 的 block removal 与 neighborChanged 生命周期无法仅凭签名保证等价，因此没有迁移，仍留作后续专门审计。

## KeyMapping / 简单客户端输入

- 注册自定义 `KeyMapping.Category`，ID 为 `exposure:main`。
- KeyMapping 构造器使用 Category 对象，不再传旧字符串 category。
- `KeyMapping.matches(int, int)` 改为 `matches(new KeyEvent(key, scanCode, modifiers))`。
- 15 个已有语言文件新增 `key.category.exposure.main`，值复用原 `category.exposure` 翻译。
- 15 个语言 JSON 已使用 UTF-8 解析验证。

Screen 内部的旧 keyPressed/matches 签名属于 UI 输入迁移，本阶段未处理。

## 修改文件列表

### Java

- `common/src/main/java/io/github/mortuusars/exposure/mixin/AnvilMenuMixin.java`
- `common/src/main/java/io/github/mortuusars/exposure/mixin/ServerPlayerMixin.java`
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/LocalPlayerMixin.java`
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/MinecraftMixin.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/input/Key.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/input/KeyboardHandler.java`
- `common/src/main/java/io/github/mortuusars/exposure/commands/exposure/DebugCommand.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/block/FlashBlock.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/block/LightroomBlock.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/camera/CameraInHand.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/entity/CameraStandEntity.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/entity/PhotographFrameEntity.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/CameraStandItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/camera/CameraItem.java`

### 语言资源

以下 15 个 `common/src/main/resources/assets/exposure/lang/*.json` 新增 KeyMapping category 翻译：

- `de_de.json`, `en_us.json`, `es_ar.json`, `es_es.json`, `es_mx.json`
- `fr_fr.json`, `ja_jp.json`, `ko_kr.json`, `pl_pl.json`, `pt_br.json`
- `ru_ru.json`, `tr_tr.json`, `uk_ua.json`, `zh_cn.json`, `zh_tw.json`

### 验证产物

- `phase6-common-baseline.log`
- `phase6-common-compile-final.log`
- `phase6-fabric-compile-final.log`
- `phase6-clean-build-final.log`

## 明确跳过的内容

### 渲染 / UI

- `ExposureLightTextureMixin`
- `HumanoidModelMixin`
- `ItemFrameRendererMixin`
- `ItemRendererMixin`
- `BuggerScreenOverlayMixin`
- GameRenderer/RenderTarget、model baking、GuiGraphics、shader、截图、取景器和 Screen layout/input 整体迁移
- CameraItem tooltip 中已移除的 ItemStack entity representation 机制

### 网络

- `ServerGamePacketListenerMixin`
- payload、codec、send/receive、Fabric networking handler
- CameraStand 的旧 `lerpTo` 网络插值迁移

### NBT / 存储

- `ExtraData extends CompoundTag`
- `CompoundTag.getAllKeys()`
- CameraStand/PhotographFrame/Lightroom 的 ValueInput、ValueOutput 和 NBT 字段读取
- SavedData、DimensionDataStorage、照片/frame 持久化

### 需要更多业务上下文

- CameraItem/CameraStand 的 ItemStack entity representation 替代设计
- CameraItem 新 server-only `inventoryTick` 与原 client-side 状态校正的拆分
- EnderMan `isLookingAtMe` 新参数（对应旧 Class Tweaker 条目，未修改）
- Lightroom removal/neighbor lifecycle
- Screen key event 整体签名

### Class Tweaker

没有修改 `RenderType`、`RenderStateShard`、`GameRenderer`、`ItemProperties`、`EnderMan` 等旧条目。validateAccessWidener 失败仍为此前已知结果。

## 验证结果

### `./gradlew :common:compileJava`

- Gradle 配置成功，common 进入 javac。
- 基线：954 条错误。
- 最终：899 条错误。
- 共减少 55 条。

最终分类：

| 类别 | 最终数量 | 变化 |
|---|---:|---:|
| Mixin 路径 | 21 | -5 |
| `world/item` | 30 | -12 |
| `world/entity` | 35 | -29 |
| `world/block` | 24 | -4 |
| 本阶段按键输入目标 | 0 | -3 |

类别存在路径重叠，因此变化数不能直接相加解释总数。

### `./gradlew :fabric:compileJava`

- Gradle 配置和依赖解析成功。
- 仍先执行 `:common:compileJava`，在 common 的 899 条错误处失败。
- `:fabric:compileJava` 自身 javac 尚未开始。
- 没有暴露新的 Fabric 构建脚本级错误。

### `./gradlew clean build --continue`

- common 进入 Java 编译。
- fabric clean、资源处理、include jars、sources jar 执行。
- 失败任务仍为 `:common:compileJava`、`:common:validateAccessWidener`、`:fabric:validateAccessWidener`。
- fabric javac 仍未开始。

## 剩余错误类别

最终 899 条诊断中：

- 渲染/UI/模型/截图/取景器：约 610 条。
- NBT/ExtraData/存储：约 41 条。
- 实体路径：35 条，多数已与 NBT、entity representation 或插值系统耦合。
- 物品路径：30 条，包含新 inventory tick、recipe 和 UI tooltip 边界。
- 方块路径：24 条，主要为 Lightroom 生命周期和 BlockEntity 存储。
- Mixin：21 条，均为渲染或网络范围。
- networking：约 5 条。

## 下一阶段建议

继续回避 610 条渲染大簇时，下一阶段最合适的是单独处理“Recipe / Menu / Block 生命周期与非 NBT 容器 API”：

1. `CustomRecipe` / `RecipeSerializer` 新类型体系；
2. `SimpleContainer` listener 与 menu 数据同步；
3. Lightroom/Flash 的剩余非存储 block callback；
4. PhotographFrameItem 的新 entity data placement API。

之后需要在以下高风险方向中明确选择独立阶段：

- ExtraData/NBT/SavedData 数据兼容；
- networking payload/codec；
- 客户端渲染/UI；
- entity interpolation 与对应 Class Tweaker。

## 核心业务误伤风险

总体风险低到中等。大多数修改是由 26.1.2 实际签名直接确认的参数/方法迁移。实体伤害与掉落现在显式运行在 `ServerLevel`，原条件、阈值、掉落和创造模式分支保持不变；KeyMapping 翻译也完整迁移。

需要后续运行验证的部分是 Mixin apply、CameraStand/PhotographFrame 的实际交互与伤害、waterlogged FlashBlock tick，以及自定义 KeyMapping category 显示。没有使用 null、no-op、stub、恒真/恒假或删除功能来换取错误下降。
