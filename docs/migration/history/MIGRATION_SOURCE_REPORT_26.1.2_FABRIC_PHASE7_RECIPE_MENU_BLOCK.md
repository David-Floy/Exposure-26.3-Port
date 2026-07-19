# Exposure 26.1.2 Fabric 迁移报告：第七阶段

## 阶段范围与结论

本阶段只迁移 Recipe / RecipeSerializer、服务端 Menu / Container 监听、Lightroom 的非 NBT 生命周期，以及 PhotographFrameItem 的实体放置入口。未修改渲染/UI、照片存储格式、ExtraData/NBT/SavedData、网络 payload/codec、相机拍摄算法或 Class Tweaker。

`common` 的 javac 错误由 **899** 降至 **873**，净减少 **26**。本阶段锁定的 Recipe、Menu listener、Lightroom/Flash 非 NBT 生命周期和 PhotographFrameItem placement 错误均已从最终日志消失。构建仍按预期失败；`:fabric:compileJava` 被其前置任务 `:common:compileJava` 阻断，Fabric javac 尚未开始。

## 修改文件

1. `common/src/main/java/io/github/mortuusars/exposure/world/item/crafting/recipe/serializer/ComponentTransferringRecipeSerializer.java`
   - 将旧的 `implements RecipeSerializer` 改为构造 26.1.2 final `RecipeSerializer` record 的工厂。
   - 保留等价 `MapCodec` 与 `StreamCodec`，并迁移到 `Ingredient.CODEC`、`ItemStack.CODEC`。
   - 保留 `category`、动态 source 字段、`ingredients`、`result` 的字段名和字段顺序。

2. `common/src/main/java/io/github/mortuusars/exposure/world/item/crafting/recipe/ComponentTransferringRecipe.java`
   - 适配无参 `CustomRecipe` 构造器。
   - 显式保留并返回原 `CraftingBookCategory`。
   - 将 `assemble(CraftingInput, HolderLookup.Provider)` 迁为 `assemble(CraftingInput)`。
   - 增加等价 `PlacementInfo`；匹配、组件转移和产物复制逻辑不变。

3. `FilmDevelopingRecipe.java`、`PhotographAgingRecipe.java`、`PhotographCopyingRecipe.java`
   - 将 `getSerializer()` 返回类型收窄为各自的类型安全 serializer。
   - 胶片瓶回收、画笔耐久、照片复制代数与组件转移逻辑未改。

4. `common/src/main/java/io/github/mortuusars/exposure/Exposure.java`
   - 将四个 recipe serializer supplier 改为具体泛型类型。
   - 注册 ID 仍为 `film_developing`、`photograph_copying`、`photograph_aging`、`component_transferring`。

5. `common/src/main/java/io/github/mortuusars/exposure/Register.java`
6. `fabric/src/main/java/io/github/mortuusars/exposure/fabric/RegisterImpl.java`
   - 将 recipe serializer 注册边界泛型化，避免丢失具体 recipe 类型。
   - 注册表、ID 和 Fabric 注册行为不变。

7. `common/src/main/java/io/github/mortuusars/exposure/world/inventory/AbstractCameraAttachmentsMenu.java`
   - `SimpleContainer.addListener` 已被移除，改为覆写 `setChanged()` 后调用原 `onContainerChanged`。
   - 附件槽位布局、过滤规则、物品移动和同步行为未改，也没有 no-op。

8. `common/src/main/java/io/github/mortuusars/exposure/world/block/LightroomBlock.java`
   - 旧 `onRemove` 迁到 26.1.2 的 `affectNeighborsAfterRemoval` 邻居更新入口。
   - `neighborChanged` 迁到带 `Orientation` 的新签名，并使用原版 `hasNeighborSignal(pos)` 查询当前红石输入。
   - 打印启动条件、折射板状态和比较器输出规则未改。

9. `common/src/main/java/io/github/mortuusars/exposure/world/block/entity/LightroomBlockEntity.java`
   - `Direction.getNormal()` 迁为 `getUnitVec3i()`。
   - 在 `preRemoveSideEffects` 中调用父类默认容器掉落，再执行 Exposure 原有经验掉落。
   - 未修改任何 NBT 读写字段或照片/frame 持久化。

10. `common/src/main/java/io/github/mortuusars/exposure/world/item/PhotographFrameItem.java`
    - 使用原版悬挂实体同类入口 `EntityType.createDefaultStackConfig(...)` 应用新的 typed entity data。
    - 放置尺寸回退、方向、位置、生存检查、声音、事件及物品消耗逻辑不变；entity data 未被丢弃。

`FlashBlock` 经审计后没有剩余的本阶段错误，因此未修改。

## Recipe / Serializer 兼容性

- serializer/type ID 未改变。
- 自定义 JSON 字段未改变：`category`、`film` / `photograph` / `source`、`ingredients`、`result`。
- `common/src/main/resources/data/exposure/recipe` 下 recipe JSON 未修改或删除。
- 匹配逻辑、source 组件转移、实际产物和剩余物逻辑未改变。
- 网络 codec 保持原字段顺序；这不是 Exposure 游戏网络协议迁移。

## 验证结果

### `:common:compileJava`

- 基线：899 个错误。
- 修改后：873 个错误。
- 结果：失败，但错误减少 26 个。
- Recipe / RecipeSerializer：本阶段 18 个错误清零。
- Menu / SimpleContainer：已识别的 listener 错误清零。
- Lightroom/Flash 非 NBT 生命周期：已识别错误清零。
- PhotographFrameItem placement：2 个错误清零。

### `:fabric:compileJava`

- `:common:compileJava` 先以 873 个错误失败。
- `:fabric:compileJava` 本身未开始，因此没有新的 Fabric 实现层 javac 结果。

### `clean build --continue`

- 失败符合预期。
- `common` javac 仍为 873 个错误。
- `common` 与 `fabric` 的 `validateAccessWidener` 仍报告既有 11 条旧目标：RenderType/RenderStateShard、GameRenderer、ItemProperties、EnderMan。
- 按本阶段限制未修改 access widener / Class Tweaker。

验证日志：

- `phase7-common-baseline.log`
- `phase7-common-compile-final.log`
- `phase7-fabric-compile-final.log`
- `phase7-clean-build-final.log`

## 明确跳过与剩余错误

- 客户端渲染/UI：GuiGraphics、screen/layout、tooltip、model、RenderType、shader、截图与取景器；仍是最大错误簇。
- NBT/存储：ExtraData、CompoundTag、ValueInput/ValueOutput、SavedData、DimensionDataStorage，以及 LightroomBlockEntity 当前约 20 个 NBT 相关错误。
- 网络：CustomPacketPayload、PayloadTypeRegistry、ServerPlayNetworking、ClientPlayNetworking、payload codec 和照片同步协议。
- 照片/frame 数据、图像编码、照片文件格式、相机拍摄算法。
- JEI、Jade、Create、RealCamera、Easy Anvils。
- 旧 Class Tweaker / access widener 的渲染、ItemProperties 和 EnderMan 条目。

## 下一阶段建议

建议下一阶段单独处理 **ValueInput / ValueOutput、ExtraData 与实体/BlockEntity 持久化边界**，先制定向后兼容策略再动代码。它们不仅形成当前稳定错误簇，也阻止 CameraStandEntity、PhotographFrameEntity、LightroomBlockEntity 等核心服务端类完成抽象方法迁移。之后再独立处理 Fabric networking；渲染与相关 Class Tweaker 应合并为另一个专门阶段，避免把客户端管线风险混入存储迁移。

## 核心业务误伤风险

没有删除、stub、no-op 或默认成功/失败处理；没有修改照片数据、网络包字段、相机算法或渲染管线。当前风险较低。需要在后续可运行环境中重点回归两点：Lightroom 被破坏时物品与经验是否各掉落一次，以及所有方向红石边沿是否仍只触发一次打印流程。
