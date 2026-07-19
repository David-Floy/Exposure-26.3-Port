# Phase 13：模型与 item model 迁移

日期：2026-07-14  
目标：Minecraft 26.1.2 Fabric（Gradle 9.4.0、Java 25、Loom 1.16.3）

## 结论

本阶段完成了 Exposure 的旧模型标识、额外方块模型加载和 `ItemProperties`/legacy override 的迁移边界。`common` Java 错误从 Phase 12 的 **390** 降为 **303**（-87）；其中旧 `ModelResourceLocation`、旧 `BakedModel`、旧 `ItemProperties` 和旧 `ItemRendererMixin` 已不再出现在参与编译的源码中。

动态物品外观没有被固定、删除或降级为 missing model。所有已注册 Exposure 物品都有 26.1.2 `assets/exposure/items/<item>.json` 根定义；Camera、Chromatic Sheet、Stacked Photographs、Album 与 Interplanar Projector 的原状态选择、阈值和 fallback 都已重建到新的 item-model JSON/属性体系。

剩余的 `CameraStandEntityRenderer` 与 Photograph Frame renderer 错误属于 `BlockStateModel` 后续提交到新 RenderPipeline 的问题。本阶段只将它们的额外模型查询边界迁移为 `Identifier`/`BlockStateModel`，没有修改 `RenderType`、Vertex、buffer 或实体渲染提交。

## 26.1.2 实际模型架构

本阶段直接检查了本工作区的 `minecraft-merged-1b20f767e8-26.1.2.jar`、Fabric API 0.150.0+26.1.2 及其 model-loading module。

- `ModelResourceLocation` 和旧 `net.minecraft.client.resources.model.BakedModel` 不再是物品渲染边界。
- 方块模型仍由 `UnbakedModel`、`ModelBakery`、`ModelManager` 发现并烘焙；运行时方块状态模型为 `BlockStateModel`/`BlockStateModelPart`。`ModelManager` 没有旧 `getModel(ModelResourceLocation)`。
- 物品根定义位于 `assets/<namespace>/items/<item id>.json`。JSON 的 `model` 字段是 `ItemModel` 树；当前原版节点包括 `model`、`condition`、`range_dispatch`、`select`、`composite`、`empty` 和 `special`。
- `ItemModelResolver` 把选择后的 `ItemModel` 写入 `ItemStackRenderState`；`ItemModel#update` 接收 stack、display context、`ClientLevel`、owner 与 seed。
- 旧 `ItemProperties.register` 已不存在。数值选择由 `RangeSelectItemModelProperty`，布尔选择由 `ConditionalItemModelProperty` 提供。26.1.2 的两项 mapper 都是 private `LateBoundIdMapper`，没有公开 Fabric property-registration API。
- Fabric 0.150.0+26.1.2 的额外模型入口是 `ModelLoadingPlugin.Context#addModel(ExtraModelKey, UnbakedExtraModel)`；旧 `addModels(Identifier)` 不存在。`SimpleUnbakedExtraModel.blockStateModel` 负责把单一额外 block model 接入 reload/bake，`FabricModelManager#getModel(ExtraModelKey)` 取得随 reload 刷新的结果。
- item-model property 都只读取已有 ItemStack/Data Component 和现有客户端状态；没有新增 NBT、Data Component、packet 或文件路径依赖。它们只会从 `ExposureClient.init()`（Fabric client entrypoint 调用）注册，dedicated server 不会加载这些客户端类。

## 修改文件

### Java 与 Fabric 注册

- `common/.../ExposureClient.java`
  - 删除旧 `ItemProperties.register` 调用及 item-only 的额外模型集合。
  - 在客户端初始化中注册新的 property mapper；额外方块模型集合改为稳定的 `Set<Identifier>`。
- `common/.../client/render/model/ExposureItemModelProperties.java`（新增）
  - 实现 7 个 condition 与 3 个 range property，保留原 9 个 property ID，并增加只表达原 `0.5` 自拍视角分支的 `exposure:camera_selfie_user`。
- `common/.../PlatformHelperClient.java`
  - Service 边界由 `ModelResourceLocation -> BakedModel` 迁移为 `Identifier -> BlockStateModel`。
- `fabric/.../PlatformHelperClientImpl.java`
  - 为每个额外 block model 建立稳定 `ExtraModelKey<BlockStateModel>`；Fabric plugin reload 时以 `SimpleUnbakedExtraModel.blockStateModel` 注册，查询使用 `FabricModelManager#getModel`。没有本地 BakedModel 缓存，因此 reload 后不会保留陈旧模型。
- `fabric/.../ExposureFabricClient.java`
  - 仍仅从 `ClientModInitializer` 注册模型；调用新的 `registerModels`。
- `common/.../client/render/CameraStandEntityRenderer.java`
  - 仅迁移 stand/mount 的模型 key 与查询变量为 `Identifier`/`BlockStateModel`；RenderType/提交代码未动。
- `common/.../client/render/PhotographFrameEntityRenderer.java`
  - 仅迁移 frame 模型定位与查询为 `Identifier`/`BlockStateModel`；照片和 frame 绘制代码未动。
- `common/.../client/render/GlassPhotographFrameEntityRenderer.java`
  - 覆盖方法同步改为 `Identifier`。
- 删除 `CameraModel.java`、`CompositeModel.java` 与 `ItemRendererMixin.java`，并从 `exposure-common.mixins.json` 删除 mixin 条目。
  - 删除的仅是已完全由新 item-model JSON 取代的旧 `BakedModel` 选择/拼接路径；所有原分支已在 `items/camera.json` 中保留。

### item-model 资源

新增 `assets/exposure/items/` 下的 19 个注册物品根：

- 静态转发：`aged_photograph`、`black_and_white_film`、`color_film`、`high_sensitivity_black_and_white_film`、`high_sensitivity_color_film`、`developed_black_and_white_film`、`developed_color_film`、`photograph`、`broken_interplanar_projector`、`signed_album`、`photograph_frame`、`glass_photograph_frame`、`camera_stand`、`lightroom`。
- 动态树：`camera`、`chromatic_sheet`、`stacked_photographs`、`album`、`interplanar_projector`。

旧 `assets/exposure/models/item/*.json` 保留为 cuboid/generated 模型源：新根中的 `minecraft:model` 节点直接引用它们。它们原有的 `overrides` 字段未被删除；26.1.2 的实际选择语义由新的 `assets/exposure/items/*.json` 承担，因此资源包可继续覆盖相同模型 ID、parent、textures 与视觉结构。

## ModelResourceLocation 映射与额外模型

| 旧键 | 新键 | 用途 |
| --- | --- | --- |
| `exposure:block/photograph_frame_{small,medium,large}#standalone` | `exposure:block/photograph_frame_{small,medium,large}` + `ExtraModelKey<BlockStateModel>` | 相框额外 block model |
| `exposure:block/glass_photograph_frame_{small,medium,large}#standalone` | 同 path 的 `Identifier` + `ExtraModelKey` | 玻璃相框额外 block model |
| `exposure:block/camera_stand#standalone` | `exposure:block/camera_stand` + `ExtraModelKey` | 支架模型 |
| `exposure:block/camera_stand_mount#standalone` | `exposure:block/camera_stand_mount` + `ExtraModelKey` | 支架 mount 模型 |
| `exposure:item/...#standalone` 的旧 Camera/附件查询 | `assets/exposure/items/camera.json` 中的 `minecraft:model` leaf | 物品模型选择 |

`standalone` 是旧 `ModelResourceLocation` variant；26.1.2 的 extra-model API 只接收真实模型 `Identifier`，因此移除 variant 而没有合并任何 path。额外模型的 key 常驻、bake 结果由 `ModelManager` reload 替换，避免旧缓存跨资源 reload。

## 动态 item model 语义

| 物品 | property / 数据来源 | 新节点与顺序 | 原阈值和 fallback |
| --- | --- | --- | --- |
| Camera | `camera_gold`：已有 `CAMERA_GOLD` component；`camera_active`/`camera_selfie`：`CameraItem` getter；`camera_has_lens`/`camera_has_flash`：既有附件；`camera_selfie_user`：原自拍 predicate 的 `0.5` 分支 | `select(display_context)` 先选 GUI；随后依次 `camera_selfie_user`、`camera_selfie`、`camera_active`、normal。每个非 GUI 状态以 `composite` 保留 body、自拍杆/viewfinder、flash、lens 顺序 | GUI 为 `camera_gui`/`camera_gold_gui`；自拍 view entity 为 `selfie_stick`；其它路径没有附件即只保留对应 body；没有 default/missing shortcut |
| Chromatic Sheet | `exposure:channels`，`ChromaticSheetItem#getLayers(stack).size()/10f` | `range_dispatch` | 0.1 -> one，0.2 -> two，0.3 -> three；fallback three |
| Stacked Photographs | `exposure:count`，现有 photographs size/100f | `range_dispatch` | 0.02 -> two，0.03 -> three；fallback three |
| Album | `exposure:photos`，现有 `AlbumItem#getPhotographsCount/100f` | `range_dispatch` | 0.01 -> one，0.02 -> two，0.03 -> three；fallback album |
| Interplanar Projector | `exposure:projector_active`，现有 `CAN_PROJECT` 配置和 custom name | `condition` | true -> active；false -> base |

Camera gold leaf、attachment 和 display transforms 均仍引用原模型 JSON；没有改变 namespace、path、纹理、父模型或相机 Data Component。`camera_selfie_user` 是为了精确表示旧 property 在当前 camera entity 时返回 `0.5` 的特殊选择，而不是用客户端猜测替换已有状态。

## Class Tweaker

删除的旧条目：

```text
net/minecraft/client/renderer/item/ItemProperties register
  (Lnet/minecraft/world/item/Item;Lnet/minecraft/resources/ResourceLocation;
   Lnet/minecraft/client/renderer/item/ClampedItemPropertyFunction;)V
```

原用途是 legacy predicate 注册；所有调用方已迁移，故该成员没有用途。

新增且已验证存在的 item-model 条目：

```text
ConditionalItemModelProperties.ID_MAPPER : LateBoundIdMapper
RangeSelectItemModelProperties.ID_MAPPER : LateBoundIdMapper
```

初次直接访问失败（字段是 private）；改为以上两条 26.1.2 official 名称和 descriptor 后，`ExposureItemModelProperties` 在 `:common:compileJava` 中不再产生错误。`validateAccessWidener` 只剩 10 条明确禁止本阶段处理的旧 RenderType/RenderStateShard、GameRenderer 与 EnderMan 目标；ItemProperties 旧目标不再出现在验证输出。没有修改其它条目。

## 验证

| 检查 | 结果 |
| --- | --- |
| `phase13-common-baseline.log` | 390 common Java errors |
| `phase13-group1-model-identifiers.log` | 306 errors；模型标识/旧 BakedModel/旧 mixin 清除后 |
| `phase13-group2-item-model.log` | 303 errors；item property mapper access 修正后 |
| `phase13-common-compile-final.log` | 303 errors，失败仅在后续范围 |
| `phase13-fabric-compile-final.log` | 被 `:common:compileJava` 的 303 errors 阻断；fabric javac 未开始 |
| `phase13-clean-build-final.log` | 303 common errors；两模块 AW 仅报告剩余 10 个禁区旧目标 |
| `phase13-common-accesswidener.log`、`phase13-fabric-accesswidener.log` | ItemProperties 旧目标已消失；剩余同一批 10 个 RenderType/GameRenderer/EnderMan 目标 |
| JSON 检查 | 所有 `assets/exposure/items/**/*.json` 与 `models/**/*.json` 可解析；模型 parent/model 引用和模型 texture 引用均存在；19 个注册物品没有缺失 item 根 |
| 静态扫描 | 没有旧 `ModelResourceLocation`、`BakedModel` 或 `ItemProperties` 调用；没有 `ItemRendererMixin` 配置条目 |

`common` 余下 303 个 javac errors 的可见 source diagnostics 主要落在 RenderPipeline/实体 renderer、动态照片/image texture、Shader/截图/取景器以及其它非模型 API。模型和 item-model 相关旧 API 已不再是阻断项；相框/支架 renderer 的最终 `BlockStateModel` 提交仍须放在 RenderPipeline 阶段完成。

## 明确未处理

- RenderType、RenderStateShard、RenderPipeline、VertexConsumer、BufferBuilder、MultiBufferSource。
- 实体 renderer 的新提交模型、LightTexture、Shader、RenderTarget、截图、取景器、CameraCapture、照片像素上传和 PhotographRenderer。
- EnderMan 新观察签名、JEI/Jade/Create/RealCamera/Easy Anvils。
- networking、ExtraData、SavedData、Frame、CaptureParameters、照片格式和相机算法。
- Phase 10–12 客户端输入与普通 GuiGraphics 二维绘制。

## 风险与下一步

静态资源和编译诊断验证通过模型边界，但由于 common 仍不能完成，尚不能在游戏内对 resource reload、Camera 的所有组合及相框视觉做运行时回归。最大剩余风险在下一阶段迁移新的 RenderPipeline/实体 renderer 提交方式时，必须保持本阶段 `BlockStateModel` 查询与 Camera item-model tree 不被回退。

下一阶段应新建对话并使用 **GPT-5.6 Terra**。当前剩余错误已经集中在 RenderPipeline、客户端渲染 Mixin、Shader、截图、取景器和照片动态渲染；Terra 更适合继续按 merged jar 签名拆分这些渲染边界。首先建议处理 RenderPipeline 与客户端 renderer/Mixin，再单独处理 Shader/截图/取景器。
