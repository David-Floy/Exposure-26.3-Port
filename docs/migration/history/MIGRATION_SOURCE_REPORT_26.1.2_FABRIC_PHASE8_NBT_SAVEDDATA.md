# Exposure 26.1.2 Fabric 迁移报告：第八阶段

> 状态：兼容设计先行、实现与静态验证均已完成。

## 数据兼容设计草案（实现前）

### 当前持久化边界

- `ExtraData`：Frame、CaptureParameters、EntityInFrame 和 advancement predicate 的可扩展 NBT 字典。已知 key 包括 `projected`、`chromatic`、`color_channel`、`shutter_speed`、`timestamp`、`focal_length`、`flash`、`selfie`、`on_stand`、`pos`、`pitch`、`yaw`、`light_level`、`day_time`、`dimension`、`biome`、`weather`、`in_cave`、`underwater`、`structures`；第三方或旧版本未知 key 也可能存在。
- `CameraStandEntity`：`CooldownTime`、`Cooldown`、`Malfunctioned`、`Camera`、`HasRedstoneSignal`、`RedstoneReleaseDelay`、`Owner`。
- `PhotographFrameEntity`：`Item`、`IsGlowing`、`ItemRotation`、`FrameItem`、`Size`、`Facing`、`Invisible`。
- `LightroomBlockEntity`：原版容器 `Items`，以及 `SelectedFrame`、`Progress`、`PrintTime`、`PrintedPhotographsCount`、`AdvanceFrame`、`PrintMode`、`LastPlayerId`；另保留读取更旧的 `Inventory.Items` 层级。
- `ExposureData`：世界 `data/exposures/<id>.dat` 中的照片像素索引数据，字段为 `width`、`height`、`pixels`、`palette`、`tag`；不会改变像素编码。
- `ExposureFrameHistory`：世界级 `exposure_frame_history.dat`，按玩家 UUID 保存最近 Frame 列表。
- `CameraInstances` 仅为运行时集合，不是 SavedData，本阶段不改变。

### ExtraData 组合模型

旧设计继承 `CompoundTag`，目的是在保留任意 NBT 字段的同时，通过 `ExtraData.Type<T>` 提供类型化访问。26.1.2 的 `CompoundTag` 已为 final，故改为组合：

- `ExtraData` 内部持有一个私有 `CompoundTag`。
- 从外部 tag 构造时深拷贝，避免共享修改；`copy()` 深拷贝整个 tag。
- `asTag()` / `copyTag()` 返回深拷贝，供 codec、predicate 和边界适配使用；不公开可变内部引用。
- `merge(CompoundTag)` 与 `merge(ExtraData)` 保持旧递归 merge 语义，未知字段逐项深拷贝并保留。
- `CODEC` / `STREAM_CODEC` 仍直接编码同一个 CompoundTag，不增加包装 key，不改变外部 NBT/网络字段结构。
- `EMPTY` 保持只读语义，避免共享空实例被写入。
- `ExtraData.Type<T>` 的业务默认值保持旧 CompoundTag 行为：缺失或类型不符时由类型化读取返回 empty/旧默认值，并记录原有错误策略。

### ValueInput / ValueOutput 边界

- Entity 与 BlockEntity 只在框架读写方法边界改用 `ValueInput` / `ValueOutput`。
- 基础字段继续使用相同 key；ItemStack 使用 `ItemStack.CODEC` 写入和读取，生成的 NBT 层级与旧 `ItemStack.save/parse` 等价。
- UUID 使用 `UUIDUtil.CODEC`，保持 int-array NBT 表示。
- Lightroom 的当前 `Items` 使用 `ContainerHelper` 新 Value API；旧 `Inventory.Items` 用 `CompoundTag.CODEC` 从同名 child 读取，作为只读兼容分支保留。
- Optional 缺失只采用旧代码已有的字段默认值，不会用空数据覆盖已成功读取的数据。

### SavedData / 文件路径

- `SavedData.Factory` 改为 `SavedDataType`，直接复用已有 `CODEC`；数据内部字段不变。
- 合法的小写照片 ID 使用 `Identifier.fromNamespaceAndPath("exposures", id)`。新 SavedDataStorage 将 namespace 映射为目录，因此仍指向旧路径 `data/exposures/<id>.dat`。
- 旧 ID 可能含玩家名中的大写或其他 Identifier 非法字符。仅对此类 ID 使用可逆 UTF-8 十六进制路径 `data/exposures/_encoded/<hex>.dat`；首次加载/保存时显式移动旧文件，逻辑照片 ID、Frame 引用和网络字段不变。列表会反向解码，删除会同时覆盖迁移前后路径。
- Frame history 必须继续读取旧根文件 `data/exposure_frame_history.dat`。设计采用合法的 `.` namespace，使解析路径为 `data/./exposure_frame_history.dat`，文件系统上与旧路径相同；不创建第二份历史文件，也不静默丢历史。
- `ExposureData` 仍存在 overworld 的 SavedDataStorage，保持原来全服务器共享照片仓库语义；Frame history 同样继续使用 overworld storage。

### 风险控制

- 不改变照片图像编码、Frame codec、相机文件格式或网络包字段顺序。
- 最大兼容风险是 Value API 对错误类型的报告方式、`.` namespace 的路径规范化，以及旧非法 ID 的首次文件迁移；已通过本地 API 签名、静态 key 搜索和构建日志验证代码边界，但仍需可启动游戏进行真实旧世界回归。
- 不吞掉 codec 错误；SavedData 继续由 Mojang codec/SavedDataStorage 报告解析失败。

## 实现结果

### 修改文件与原因

1. `common/src/main/java/io/github/mortuusars/exposure/util/ExtraData.java`
   - 由 `extends CompoundTag` 改为私有 `CompoundTag` 组合。
   - 增加深拷贝 `asTag()` / `copyTag()`、组合式 `copy()`、`merge(ExtraData)` 与 `merge(CompoundTag)`。
   - codec 仍直接读写原 compound，不增加包装层；`EMPTY` 保持不可写。

2. `common/src/main/java/io/github/mortuusars/exposure/advancements/predicate/ExtraDataPredicate.java`
   - predicate 改为显式比较 ExtraData 的 detached CompoundTag。
   - 保留子集匹配、key 遍历、嵌套 NBT 比较和缺失字段语义。

3. `CameraStandEntity.java`、`CameraStandRedstoneControl.java`
   - 实体读写边界改为 `ValueInput` / `ValueOutput`。
   - ItemStack 使用 `ItemStack.CODEC`，Owner 使用 `UUIDUtil.CODEC`；所有旧 key 不变。

4. `PhotographFrameEntity.java`
   - 实体读写边界改为 Value API；Item/FrameItem 使用 `ItemStack.CODEC`。
   - `Facing` 仍写旧 byte 3D direction ID；同时将已封装的 HangingEntity direction 字段迁到 getter/raw setter。

5. `LightroomBlockEntity.java`
   - BlockEntity 读写边界改为 Value API，当前 `Items` 交给新 `ContainerHelper`。
   - 继续读取旧 `Inventory.Items` 层级，并以 registry-aware ItemStack codec 解码。
   - 进度、经验、打印模式、选中帧和最后玩家 UUID 的 key 与默认值不变。

6. `ExposureData.java`
   - 删除旧 `SavedData.Factory` 和手写 save/load 外壳，改为按照片 ID 缓存的 `SavedDataType<ExposureData>`。
   - 复用原 `CODEC`，像素、调色板与 tag 结构不变。

7. `ExposureFrameHistory.java`
   - 改为静态 `SavedDataType`；使用 `.` namespace 保持根路径 `data/exposure_frame_history.dat`。
   - UUID → Frame 列表 codec 和 32 帧限制不变。

8. `ExposureRepository.java`
   - `DimensionDataStorage` 迁为 `SavedDataStorage`，get/set/computeIfAbsent 使用 `SavedDataType`。
   - 保持 overworld 全服务器共享存储；加入非法旧 ID 的可逆路径迁移、列表解码和安全路径检查。

9. `ExposeCommand.java`、`LoadCommand.java`
   - 将旧 typed `contains` 改为 Optional string 类型检查；命令数据和 CaptureParameters codec 不变。

10. `client/util/bugger/Bugger.java`
    - 调试查看器使用 `TagValueOutput` 捕获实体 NBT；只迁移保存边界，不涉及渲染逻辑。

### DataFixTypes 选择

官方 Fabric 26.1.2 Saved Data 文档展示了 `SavedDataType` 与 codec 的推荐结构。文档示例传入 null data fixer，但本地 26.1.2 `SavedDataStorage.readTagFromDisk` 对已有文件会直接调用该值；为确保旧文件可读，本迁移使用最接近任意动态 compound 的 `SAVED_DATA_COMMAND_STORAGE`。这是基于本地官方类实现的保守推断，必须列为运行时旧世界回归重点。参考：[Fabric Saved Data 26.1.2](https://docs.fabricmc.net/develop/serialization/saved-data)。

### 格式兼容结论

- 已知 Entity、BlockEntity、ExtraData 和 SavedData key 均保留。
- ExtraData 未知字段在构造、copy、merge、codec 往返中深拷贝保留。
- Frame、ExposureData、CaptureParameters 的 codec 字段和网络 StreamCodec 字段顺序未改。
- 照片图像像素编码、照片文件内容结构和相机算法未改。

## 验证结果

### 编译

- `:common:compileJava`：873 → **791**，净减少 **82**；任务仍失败。
- ExtraData / CompoundTag 本阶段错误：清零。
- Entity / BlockEntity ValueInput/ValueOutput 持久化错误：清零。
- SavedData / SavedDataStorage 本阶段错误：清零。
- `:fabric:compileJava`：仍被 `:common:compileJava` 前置失败阻断，Fabric javac 尚未开始。
- `clean build --continue`：common 仍为 791 个错误；common/fabric 的 access widener 仍是既有 11 条 RenderType、RenderStateShard、GameRenderer、ItemProperties、EnderMan 旧目标。

### 静态兼容检查

- `extends CompoundTag`：0。
- `DimensionDataStorage`：0。
- `SavedData.Factory`：0。
- `SavedDataType` / `SavedDataStorage` 声明存在且 ID 路径明确。
- 旧持久化 key 全部仍能在源码中定位。
- 本阶段修改文件中没有新增 null/no-op/stub/默认成功或默认失败实现。

验证日志：

- `phase8-common-baseline.log`
- `phase8-common-compile-final.log`
- `phase8-fabric-compile-final.log`
- `phase8-clean-build-final.log`

## 明确跳过与剩余类别

- 渲染/UI/Shader、截图、取景器、相册和照片绘制仍是最大错误簇。
- Fabric networking payload/codec、packet handler 和照片同步协议未修改。
- PhotographFrameEntity、CameraStandEntity 仍有与持久化无关的基础实体/API 错误。
- Mixin target/inject point 未处理。
- JEI、Jade、Create、RealCamera、Easy Anvils 未处理。
- 11 条旧 access widener / Class Tweaker 渲染及 EnderMan 目标未修改。

## 旧存档兼容风险

- Java 层 key 和 codec 结构保持兼容，但项目尚不能启动，无法完成真实旧世界 round-trip 测试。
- `DataFixTypes.SAVED_DATA_COMMAND_STORAGE` 是为避免已有 SavedData 在 null data fixer 下读取失败的保守选择；需用旧世界样本确认 DFU 不改变 Exposure compound。
- 非法 Identifier 的旧照片 ID 会执行一次文件移动。移动失败会记录明确错误并保留旧文件，不会删除原数据；但该照片在迁移成功前仍无法由新 SavedDataStorage 读取。
- `.` namespace 经本地 Identifier/路径实现确认映射至旧历史文件位置，仍应在 Windows 与 Linux 各做一次路径回归。

## 下一阶段建议

先做一个小型旧世界兼容验证夹具：准备包含大小写照片 ID、ExtraData 未知嵌套字段、进行中 Lightroom、CameraStand 和 PhotographFrame 的旧 NBT 样本，在能启动 common 后验证读写前后结构。源码迁移方面，建议下一阶段集中处理剩余非渲染实体 API 与 Fabric networking payload/codec；之后再将客户端 UI 基础 API和渲染管线拆成两个独立阶段。

## 核心业务误伤风险

未删除核心功能，未改照片算法、图像编码、网络协议字段或渲染管线。当前源码改动风险为中等：Java/NBT 结构兼容性较高，但 SavedData 文件路径与 DataFixer 行为必须通过真实旧世界运行验证后才能视为完全闭环。
