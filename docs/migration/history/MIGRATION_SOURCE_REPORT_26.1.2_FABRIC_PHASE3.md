# Exposure 26.1.2 Fabric 第三阶段迁移报告

日期：2026-07-07

## 阶段结论

本阶段只进行了低风险的 official naming、基础类型和简单签名迁移，没有改写照片存储、图像编码、网络协议、渲染管线、相机文件格式或核心玩法。

`common` 仍能稳定进入 javac，但错误总量仍超过 javac 默认显示上限，因此修改前后都显示 `1,000 个错误`。这个数字不能用于判断真实总量是否下降。可以确认的是，本阶段针对的错误族已经从首批诊断中消失：`isClientSide` 字段访问、Inventory 选中槽字段、旧 FriendlyByteBuf Identifier 方法、旧 tooltip override、旧 ClickEvent/HoverEvent 构造方式、`InteractionResult.sidedSuccess`、旧权限整数、`ResourceKey.location()`、`SoundEvent.getLocation()`、旧 GameRules 常量等均不再出现。

剩余首批错误已经明显集中到渲染/UI、NBT/存储、Mixin 注入接口、Advancement、网络和其他系统级迁移，符合本阶段的停止条件。

## 工作区说明

阶段开始时，工作区已有约 154 个 common Java 文件相对上游 1.21.1 快照发生机械命名变化，例如 `ResourceLocation -> Identifier` 和部分 official package 迁移。这些变化不是本阶段补丁产生的；本阶段保留了它们，并以当前工作区为基础继续工作。由于当前目录没有 `.git` 元数据，不能用 Git 精确分离更早阶段、并发变更与本阶段变更。

## 本阶段修改文件

### 基础世界、实体、菜单和物品 API

- `common/src/main/java/io/github/mortuusars/exposure/event/ServerEvents.java`
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/EntityMixin.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/block/LightroomBlock.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/block/entity/LightroomBlockEntity.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/entity/CameraStandEntity.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/entity/PhotographFrameEntity.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/inventory/AlbumMenu.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/inventory/LightroomMenu.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/AlbumItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/CameraStandItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/InterplanarProjectorItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/PhotographFrameItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/PhotographItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/StackedPhotographsItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/camera/CameraItem.java`

主要修改：`Level.isClientSide()`、`Inventory.getSelectedSlot()`、`Inventory.getSelectedItem()`、`InteractionResult.SUCCESS/SUCCESS_SERVER`、`GameRules.ENTITY_DROPS` 和 `ResourceKey.identifier()`。没有删除物品、实体、菜单或交互分支。

### Tooltip、文本和命令事件

- `common/src/main/java/io/github/mortuusars/exposure/world/item/BrokenInterplanarProjectorItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/ChromaticSheetItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/DevelopedFilmItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/FilmRollItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/SignedAlbumItem.java`
- 上一组中的 `AlbumItem.java`、`InterplanarProjectorItem.java`、`PhotographItem.java`、`CameraItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/task/ExportExposuresTask.java`
- `common/src/main/java/io/github/mortuusars/exposure/commands/ExposureCommand.java`
- `common/src/main/java/io/github/mortuusars/exposure/commands/ShaderCommand.java`
- `common/src/main/java/io/github/mortuusars/exposure/commands/exposure/DebugCommand.java`
- `common/src/main/java/io/github/mortuusars/exposure/commands/exposure/ExportCommand.java`
- `common/src/main/java/io/github/mortuusars/exposure/commands/exposure/ExposeCommand.java`
- `common/src/main/java/io/github/mortuusars/exposure/commands/exposure/LoadCommand.java`

主要修改：适配新的 `Item.appendHoverText` consumer 签名；用局部列表保留原有 tooltip 生成顺序；迁移 `ClickEvent.RunCommand`、`ClickEvent.OpenFile`、`HoverEvent.ShowText`、`HoverEvent.ShowItem`；命令权限迁移到 `Permissions` 常量。没有改变命令业务流程。

### Registry、Identifier、颜色和声音

- `common/src/main/java/io/github/mortuusars/exposure/commands/argument/ColorPaletteArgument.java`
- `common/src/main/java/io/github/mortuusars/exposure/commands/exposure/PaletteCommand.java`
- `common/src/main/java/io/github/mortuusars/exposure/data/ColorPalettes.java`
- `common/src/main/java/io/github/mortuusars/exposure/data/Filters.java`
- `common/src/main/java/io/github/mortuusars/exposure/data/Lenses.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/level/LevelUtil.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/level/storage/ExposureIdentifier.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/level/storage/ExposureData.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/capture/template/CaptureTemplate.java`
- `common/src/main/java/io/github/mortuusars/exposure/network/handler/ClientPacketsHandler.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/SensitiveFilmItem.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/sound/UniqueSoundManager.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/image/modifier/pixel/AgedHSBEffect.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/image/modifier/pixel/NegativeEffect.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/image/modifier/pixel/NegativeFilmEffect.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/image/modifier/pixel/TintedNegativeFilmEffect.java`

主要修改：`RegistryAccess.lookupOrThrow`、registry `getValue`、`ResourceKey.identifier()`、FriendlyByteBuf 的 `readIdentifier/writeIdentifier`、`SoundEvent.location()`、`SoundInstance.getIdentifier()` 和 `ARGB.colorFromFloat`。像素 modifier 中只重命名了与 `ARGB` 类型冲突的参数，没有改动算法。

### 新增验证产物

- `phase3-common-compile-final.log`
- `phase3-fabric-compile-final.log`
- `phase3-clean-build-final.log`
- 本报告 `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE3.md`

## 已完成的通用迁移类别

- Resource/Identifier：完成本轮可机械确认的 key accessor 和 buffer 方法迁移。
- InteractionResult：移除 `sidedSuccess`，保持客户端 success、服务端 success 的原语义分支。
- Component/Tooltip：完成 9 个物品 tooltip override 的新签名迁移。
- Registry/Holder 基础入口：完成 `lookupOrThrow`、`getValue` 和 key identifier 的简单迁移。
- Sound/GameRules：完成 `SoundEvent.location()`、`SoundInstance.getIdentifier()` 和 `ENTITY_DROPS`。
- 命令文本事件：完成新的 Click/Hover record 类型构造。
- 权限：完成整数权限级别到 `Permissions` 常量迁移。
- 基础客户端/颜色命名：完成可直接由 26.1.2 类签名验证的部分。

Fabric 官方 26.1 移植说明明确指出 26.1 使用 official mappings 且存在大规模 Fabric API 改名；本阶段按这一方向执行。参考：[Porting to 26.1](https://docs.fabricmc.net/develop/porting/) 和 [Fabric API changes](https://docs.fabricmc.net/develop/porting/fabric-api)。

## 未处理的高风险类别

- Advancement / Criterion：仍有 trigger、criterion 和 advancement 构造 API 错误，未进行整体迁移。
- `ExtraData extends CompoundTag`：`CompoundTag` 已为 final；未改继承设计，也未触碰照片/帧存储格式。
- 新 NBT/存储 API：`ValueInput`、`ValueOutput`、`SavedData`、`DimensionDataStorage` 等尚未迁移。
- Fabric networking：payload、codec、注册和收发流程未重设计。
- 渲染/UI：`GuiGraphics`、RenderType/RenderStateShard、模型、shader、截图捕获、取景器仍是最大错误簇。
- Mixin：未修改 target 或 inject point。
- 实体注入接口：`Player/ServerPlayer cannot be converted to CameraHolder/CameraOperator` 表明原有 injected-interface/桥接机制需要单独核对。
- EntityType tag、Biome precipitation、cooldown、item property、KeyMapping category 等需要更多上下文，未做猜测式修改。
- JEI、Jade、Create、Ponder、Flywheel、Registrate、RealCamera、Easy Anvils 均未恢复。

## Architectury / common-fabric 边界

仍需后续审计的桥接文件包括：

- `common/src/main/java/io/github/mortuusars/exposure/PlatformHelper.java`
- `common/src/main/java/io/github/mortuusars/exposure/PlatformHelperClient.java`
- `common/src/main/java/io/github/mortuusars/exposure/Register.java`
- `common/src/main/java/io/github/mortuusars/exposure/network/Packets.java`

本阶段没有为这些方法添加 `null`、空实现或 stub，也没有大改调用方。

## Class Tweaker / access widener

`common/build.gradle` 和 `fabric/build.gradle` 都仍声明 `common/src/main/resources/exposure.accesswidener`。文件头保持 `classTweaker v1 official`，所有条目保持原样。

`clean build --continue` 中，`:common:validateAccessWidener` 与 `:fabric:validateAccessWidener` 都实际执行并失败，证明 Loom 正在读取该文件。失效目标集中在旧 RenderType/RenderStateShard 类与字段、GameRenderer 方法、ItemProperties 注册方法和 EnderMan 方法等。因为它们没有阻止 `:common:compileJava` 展开源码错误，本阶段没有注释任何条目。Class Tweaker 迁移规则参考：[Class Tweakers](https://docs.fabricmc.net/develop/class-tweakers/)。

## 验证结果

### `./gradlew :common:compileJava`

- Gradle 配置成功，`:common:compileJava` 进入 javac。
- 结果：失败，仍达到 `1,000 个错误`显示上限。
- 构建脚本级错误：无。
- 数量结论：无法从上限数字证明总错误数下降；可证明本阶段目标错误族为 0，剩余诊断被更深层系统错误补位。

### `./gradlew :fabric:compileJava`

- Gradle 配置成功。
- 任务先执行项目依赖 `:common:compileJava`，并在 common 的 1000 条上限处失败。
- `:fabric:compileJava` 自身 javac 尚未开始，因此不能宣称 fabric 源码已完成首轮编译。
- 没有发现 fabric build script 或依赖解析错误。

### `./gradlew clean build --continue`

- Gradle 配置成功。
- `common` 进入 Java 编译。
- fabric 的 clean、资源处理、include jar、sources jar 等任务执行；fabric Java 编译仍受 common 失败阻断。
- 失败任务：`:common:compileJava`、`:common:validateAccessWidener`、`:fabric:validateAccessWidener`。
- 除源码编译和已知 Class Tweaker 旧目标外，没有新的构建脚本级错误。

## 下一阶段建议

建议顺序如下：

1. **Architectury bridge 与 injected interfaces**：先确认 Fabric-only 的 `PlatformHelper`、`Register`、`Packets` 实现边界，以及 `CameraHolder/CameraOperator` 如何在 26.1 Loom 下声明。这个问题跨越大量相机调用方，优先级最高。
2. **Advancement / Criterion**：边界相对独立，完成后能压掉一组注册与触发错误。
3. **ExtraData / 新存储 API**：必须先设计组合替代继承并验证数据兼容，不能机械修；应单独成阶段。
4. **Networking**：等待数据模型稳定后迁移 payload/codec，避免反复修改协议。
5. **Class Tweaker**：Java 类型迁移和渲染 API方案明确后，再逐条核对 official 目标；现在修会与渲染重构互相干扰。

之后再单独处理渲染/UI/Shader/截图捕获。不要把渲染迁移混入桥接、存储或网络阶段。

## 核心业务误伤风险

本阶段补丁风险较低：没有删除、stub 或重排核心系统；tooltip 使用局部列表保持原有生成逻辑；颜色效果只改变量名和等价颜色工厂；交互结果保持客户端/服务端分支。唯一需要持续审计的是阶段开始前已存在的批量机械迁移，特别是 `InteractionResult` 从旧 holder 语义迁移到新 item transformation 语义的代码，不能仅凭编译通过认定行为等价。
