# Exposure 26.1.2 Fabric 第五阶段：Advancement / Criterion / Predicate

日期：2026-07-08

## 阶段结论

本阶段完成了 Minecraft 26.1.2 下 Exposure 自定义 criterion trigger 与物品数据组件 predicate 的注册迁移。

- common 编译错误由 965 条下降到 954 条，共减少 11 条。
- `ItemSubPredicate` / `ITEM_SUB_PREDICATE_TYPE` 错误清零。
- `Register` 中 criterion / predicate 相关错误清零。
- `FrameExposedTrigger`、`FramePrintedTrigger`、`FramePredicate` 相关编译错误清零。
- 所有本阶段允许处理的 Advancement / Criterion / predicate 错误清零。
- `ExtraDataPredicate` 仍有 1 条 `CompoundTag.getAllKeys()` 错误；它属于明确禁止处理的 ExtraData/NBT 边界，本阶段没有修改。
- trigger ID、predicate ID、codec 字段、advancement requirements、奖励和达成条件均未改变。

## 基线错误归类

初始 `:common:compileJava` 报告 965 条错误。本阶段相关错误为：

- `ItemSubPredicate` 类已不存在：`Exposure.java`、`Register.java`。
- `BuiltInRegistries.ITEM_SUB_PREDICATE_TYPE` 已不存在：Fabric `RegisterImpl` 的旧注册入口。
- `FramePredicate` 仍实现旧 `matches(ItemStack, Frame)` 签名。
- `FrameExposedTrigger` 中 3 处旧 `ServerPlayer.serverLevel()`。
- `FramePrintedTrigger` 中 1 处旧 `ServerPlayer.serverLevel()`。
- `ExtraDataPredicate` 中 1 处旧 `CompoundTag.getAllKeys()`，归入 NBT 禁止处理范围。

Trigger 的 `Codec`、`SimpleCriterionTrigger` 继承关系和主要匹配逻辑在基线中没有结构性错误。

## 自定义 Criterion / Trigger 审计

Exposure 注册 4 个自定义 trigger ID：

| ID | 实现 | 语义 |
|---|---|---|
| `exposure:frame_exposed` | `FrameExposedTrigger` | 拍摄 frame 时匹配相机、frame、画面位置和画面内实体。 |
| `exposure:frame_printed` | `FramePrintedTrigger` | 打印 frame 时匹配打印位置、frame 和输出物品。 |
| `exposure:photograph_enderman_eyes` | 原版 `PlayerTrigger` | 保留原玩家事件触发语义。 |
| `exposure:successfully_project_image` | 原版 `PlayerTrigger` | 保留原玩家事件触发语义。 |

### `FrameExposedTrigger.TriggerInstance`

仍为 codec 驱动 record，JSON 字段保持：

- `player`
- `camera`
- `frame`
- `location_in_frame`
- `entities_in_frame`

匹配语义未变：可选 camera 与 frame 必须匹配；位置条件要求画面内至少一个位置匹配；每个实体条件都要求画面内至少一个实体满足相应 `ContextAwarePredicate`。

### `FramePrintedTrigger.TriggerInstance`

仍为 codec 驱动 record，JSON 字段保持：

- `player`
- `location`
- `frame`
- `item`

匹配语义未变：打印位置、frame 和结果物品分别执行原有可选 predicate。

两个 trigger 只把 26.1.2 已不存在的 `ServerPlayer.serverLevel()` 改为返回 `ServerLevel` 的 `ServerPlayer.level()`。没有改变服务器端触发流程，也没有把条件改成恒真或恒假。

## Predicate 注册迁移

Minecraft 26.1.2 已不再提供旧 `ItemSubPredicate.Type` 与 `ITEM_SUB_PREDICATE_TYPE`。对应系统现在使用：

- `DataComponentPredicate.Type<T>`
- `DataComponentPredicate.ConcreteType<T>`
- `BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE`

Exposure 的 `exposure:frame` 已迁移为：

```java
new DataComponentPredicate.ConcreteType<>(FramePredicate.CODEC)
```

注册 ID 仍为 `exposure:frame`，`FramePredicate.CODEC` 完全保留，因此数据包中以下字段均未改变：

- `identifier`
- `type`
- `photographer`
- `shutter_speed`
- `focal_length`
- `light_level`
- `day_time`
- `entities_in_frame_count`
- `entities_in_frame`
- `extra_data`

`Register.itemSubPredicate(...)` 的方法名暂时保留，以避免无必要地破坏 common API；其类型合约和 Fabric 注册表已经切换为 `DataComponentPredicate`。它不再引用旧 Minecraft 类型。

`SingleComponentItemPredicate` 在 26.1.2 中要求 `matches(T)`。`FramePredicate` 原先的 `matches(ItemStack, Frame)` 没有使用 `ItemStack`，因此移除该旧适配方法并把现有 `matches(Frame)` 标记为 override，判断逻辑逐行保持不变。

Entity predicate `exposure:tamed` 仍使用 `EntitySubPredicate`、`MapCodec<TamedPredicate>` 和 `ENTITY_SUB_PREDICATE_TYPE`；该路径在 26.1.2 中仍有效，没有改动其 `is_tamed` 字段或匹配语义。

## Advancement JSON 与 datagen 审计

- 共检查 29 个内置 advancement JSON。
- 其中 13 个文件使用 Exposure 自定义 trigger。
- 所有 JSON 均能被 JSON 解析器读取。
- 自定义 trigger ID 仍为上述 4 个 ID。
- JSON 中使用的顶层自定义条件字段为 `camera`、`frame`、`entities_in_frame`、`item`，均与 trigger codec 匹配。
- 没有修改 parent、criteria、requirements、rewards 或玩家达成条件。
- 当前 common/fabric 源码中没有 Advancement datagen provider 或旧 `Advancement.Builder` 代码，因此无 builder API 需要迁移。

客户端 item model predicate（旧 `ItemProperties` 等）属于渲染/客户端模型系统，不是本阶段的 advancement predicate，未处理。

## 修改文件列表

| 文件 | 修改原因 |
|---|---|
| `common/src/main/java/io/github/mortuusars/exposure/Register.java` | 将旧 `ItemSubPredicate.Type` 合约迁移到 `DataComponentPredicate.Type`，保持 bridge API。 |
| `common/src/main/java/io/github/mortuusars/exposure/Exposure.java` | 用 `ConcreteType<>(FramePredicate.CODEC)` 注册 `exposure:frame`。 |
| `common/src/main/java/io/github/mortuusars/exposure/advancements/predicate/FramePredicate.java` | 对齐 `SingleComponentItemPredicate.matches(Frame)` 新签名。 |
| `common/src/main/java/io/github/mortuusars/exposure/advancements/trigger/FrameExposedTrigger.java` | 使用 `ServerPlayer.level()`，保留 trigger codec 与匹配逻辑。 |
| `common/src/main/java/io/github/mortuusars/exposure/advancements/trigger/FramePrintedTrigger.java` | 使用 `ServerPlayer.level()`，保留 trigger codec 与匹配逻辑。 |
| `fabric/src/main/java/io/github/mortuusars/exposure/fabric/RegisterImpl.java` | 改用 official criterion package 和 `DATA_COMPONENT_PREDICATE_TYPE` 注册表。 |

没有修改任何 advancement JSON。

新增验证产物：

- `phase5-common-baseline.log`
- `phase5-common-compile-final.log`
- `phase5-fabric-compile-final.log`
- `phase5-clean-build-final.log`

## 验证结果

### `./gradlew :common:compileJava`

- Gradle 配置成功，common 进入 javac。
- 最终结果：954 条错误，较基线 965 条减少 11 条。
- `ItemSubPredicate` / 旧 predicate registry 错误：0。
- criterion trigger 错误：0。
- `FrameExposedTrigger` / `FramePrintedTrigger` 错误：0。
- `FramePredicate` 错误：0。
- `Register.java` 错误：0。
- Advancement 目录仅剩 `ExtraDataPredicate` 的 1 条 NBT API错误。

### `./gradlew :fabric:compileJava`

- Gradle 配置和依赖解析成功。
- 任务仍先执行 `:common:compileJava`，并在 common 的 954 条错误处停止。
- `:fabric:compileJava` 自身 javac 尚未开始。
- 因此 `RegisterImpl` 的新 Fabric 注册调用尚未由 fabric javac 完整验证，但 common service 类型已对齐，未出现新的 common bridge 错误。

### `./gradlew clean build --continue`

- common 进入 Java 编译。
- fabric 的 clean、资源处理、include jars 和 sources jar 任务执行。
- 失败任务仍为 `:common:compileJava`、`:common:validateAccessWidener`、`:fabric:validateAccessWidener`。
- Class Tweaker 失败仍来自此前保留的旧 RenderType、GameRenderer、ItemProperties、EnderMan 条目；本阶段未修改。
- fabric Java 编译仍受 common 失败阻断。

## 剩余错误类别

最终 954 条 javac 诊断按关键词/路径统计（类别可能重叠）：

- 客户端渲染、UI、模型、截图/取景器：约 610 条。
- 相机核心文件中的 26.1 API：约 66 条。
- NBT、ExtraData、SavedData 与存储：约 41 条。
- Mixin：约 26 条。
- Fabric API 事件/注册：约 25 条。
- networking payload/codec：约 5 条。
- 本阶段可处理的 Advancement/Criterion/predicate：0 条。
- Advancement 目录内、但属于禁止处理的 ExtraData/NBT：1 条。

## 下一阶段建议

为了继续让 common 错误下降并尽早启动 fabric javac，建议下一阶段处理“非渲染、非网络、非存储的简单 Mixin 与实体/物品 API 签名”。这约涉及 26 条 Mixin 和一部分相机外围 API，边界比 610 条渲染簇更清晰。

之后再分别规划：

1. ExtraData/NBT/SavedData 数据兼容设计；
2. Fabric networking payload/codec；
3. 客户端渲染/UI/Shader 大阶段；
4. 对应渲染迁移后的旧 Class Tweaker 条目。

## 核心业务误伤风险

风险较低。Trigger ID、predicate ID、codec 字段、JSON、requirements、奖励和实际判断逻辑均保持不变。`FramePredicate` 只移除了不再存在且未使用 `ItemStack` 参数的旧适配签名。没有修改照片保存、图像编码、相机算法、网络协议、渲染或 NBT 数据结构。

唯一待后续运行验证的点是 Fabric `DATA_COMPONENT_PREDICATE_TYPE` 注册，因为 fabric javac 尚未启动；实现使用的是 Minecraft 26.1.2 实际 `BuiltInRegistries` 与 `DataComponentPredicate.ConcreteType` 签名，没有加入 fallback 或空实现。
