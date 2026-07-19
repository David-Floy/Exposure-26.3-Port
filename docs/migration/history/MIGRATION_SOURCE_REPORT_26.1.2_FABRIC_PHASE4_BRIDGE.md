# Exposure 26.1.2 Fabric 第四阶段：Bridge 与 Injected Interfaces

日期：2026-07-08

## 结论

第四阶段完成了 Fabric-only 平台绑定和玩家 injected interfaces 恢复，没有改写照片数据、图像编码、渲染、Advancement、NBT 或网络协议。

- `Player` 在开发编译环境中重新明确实现 `CameraHolder` 与 `CameraOperator`。
- 基线中的 13 个 `Player` / `ServerPlayer` / `LocalPlayer cannot be converted to CameraHolder/CameraOperator` 错误全部消失。
- common 编译从至少 1000 条、被 javac 上限截断，下降到完整可见的 965 条错误。
- `PlatformHelper`、`PlatformHelperClient`、`Register`、`Packets` 已移除 Architectury `@ExpectPlatform`。
- Fabric 入口会在 `Exposure.init()` / `ExposureClient.init()` 之前绑定真实平台实现。
- 没有使用 `null`、空 Optional、空列表、no-op、stub、删除调用方或 unsafe cast 来绕过 injected-interface 错误。

## 根因

旧构建通过 `common/src/main/resources/architectury.common.json` 注入接口：

```json
"net/minecraft/class_1657": [
  "io/github/mortuusars/exposure/world/entity/CameraHolder",
  "io/github/mortuusars/exposure/world/entity/CameraOperator"
]
```

`class_1657` 是旧 Yarn 名称。Fabric-only Loom 构建不消费 Architectury 的 `injected_interfaces` 元数据，因此运行时 `PlayerMixin implements CameraHolder, CameraOperator` 仍在，但 javac 看不到 `Player` 与两个接口之间的类型关系。

## Injected interfaces 恢复方式

在 `common/src/main/resources/exposure.accesswidener` 中添加了 Loom 1.16 Class Tweaker interface injection：

```text
inject-interface net/minecraft/world/entity/player/Player io/github/mortuusars/exposure/world/entity/CameraHolder
inject-interface net/minecraft/world/entity/player/Player io/github/mortuusars/exposure/world/entity/CameraOperator
```

这两条使用 Minecraft 26.1.2 official internal name。两个接口中的所有方法均为 `default`，符合 Class Tweaker 要求。现有 `PlayerMixin` 继续提供真实实现；`ServerPlayer` 和 `LocalPlayer` 通过继承 `Player` 获得编译期接口关系，原有 `ServerPlayerMixin` 与 `LocalPlayerMixin` 继续覆盖平台侧行为。

官方规则参考：[Fabric Interface Injection 26.1.2](https://docs.fabricmc.net/develop/class-tweakers/interface-injection) 与 [Class Tweakers](https://docs.fabricmc.net/develop/class-tweakers/)。

单独运行 `:common:validateAccessWidener` 时，新注入条目没有产生验证错误。验证任务仍因原有 11 个 RenderType、GameRenderer、ItemProperties、EnderMan 等旧目标失败；这些旧签名按阶段限制未修改。

## Architectury bridge 处理方式

### common API

四个原有静态 API 类保留调用方可见的方法：

- `PlatformHelper`
- `PlatformHelperClient`
- `Register`
- `Packets`

每个类新增一个明确的 `Service` 合约及一次性 `bind(...)`。静态方法委托给已绑定的 Fabric 实现。若 Fabric 入口没有绑定，代码会抛出带原因的 `IllegalStateException`，不会返回伪造的空结果或静默跳过业务。

`Register.menuType` 的公开泛型由冗余的 `<T extends MenuType<E>> Supplier<T>` 收窄为等价且类型安全的 `<E> Supplier<MenuType<E>>`，消除了 Fabric `ExtendedScreenHandlerType` 实现中对 `T` 的不安全强转；所有现有调用方本来就声明为 `Supplier<MenuType<具体菜单>>`。

### Fabric 实现

旧 Fabric 实现类改为实现对应 service：

- `PlatformHelperImpl implements PlatformHelper.Service`
- `PlatformHelperClientImpl implements PlatformHelperClient.Service`
- `RegisterImpl implements Register.Service`
- `PacketsImpl implements Packets.Service`

`ExposureFabric.onInitialize()` 在 `Exposure.init()` 之前绑定平台、注册和网络实现。`ExposureFabricClient.onInitializeClient()` 在 `ExposureClient.init()` 之前绑定客户端实现。这样能保证 Exposure 的静态注册开始前，Fabric 注册后端已经可用。

`Packets` 只迁移了桥接调用方向；packet 类型、ID、codec、字段顺序、handler 和 C2S/S2C 注册内容均未修改。26.1 networking payload/codec 正式迁移仍留给后续阶段。

### 移除的 Architectury 内容

- 删除 common 对 `dev.architectury:architectury-injectables` 的 compile-only 依赖。
- 删除根构建中的 Architectury Maven 仓库。
- 删除已被 Class Tweaker 替代的 `architectury.common.json`。
- common 与 fabric 的参与编译源码中已无 `@ExpectPlatform` 或 `dev.architectury` 引用。
- 被排除的 JEI 兼容源码中仍有一处 `@ExpectPlatform`；本阶段按限制没有处理 JEI，该文件不参与编译和运行。

## Mixin 审计

### `exposure-common.mixins.json`

- package 仍为 `io.github.mortuusars.exposure.mixin`。
- `PlayerMixin` 仍在 required `mixins` 列表。
- `ServerPlayerMixin` 仍在 required `mixins` 列表。
- `client.LocalPlayerMixin` 仍在 required `client` 列表。
- `PlayerMixin` target 为 official `Player.class`，并实现 `CameraHolder, CameraOperator`。
- required 状态和 `defaultRequire: 1` 未改变。

### Fabric mixin

`fabric/src/main/resources/exposure.mixins.json` 仍声明 Fabric package、plugin、`LevelRendererMixin` 和 `AlbumItemFabricMixin`。它不负责玩家接口注入，也未修改。

仓库中没有单独的 `exposure-client.mixins.json`；客户端 common mixin 位于 `exposure-common.mixins.json` 的 `client` 数组。这一结构保持不变。

Mixin JSON、本阶段涉及的 Mixin 类及其 target/inject point均未修改。RealCamera、Easy Anvils 和其他可选兼容仍保持排除。

## 修改文件列表

| 文件 | 修改原因 |
|---|---|
| `build.gradle` | 移除仅供 Architectury injectables 使用的 Maven 仓库。 |
| `common/build.gradle` | 移除 Architectury injectables compile-only 依赖。 |
| `common/src/main/resources/exposure.accesswidener` | 添加 Player -> CameraHolder/CameraOperator 的 official Class Tweaker 注入。 |
| `common/src/main/resources/architectury.common.json` | 删除；旧 Yarn Architectury 注入已由 Class Tweaker 等价替代。 |
| `common/src/main/java/io/github/mortuusars/exposure/PlatformHelper.java` | 保留静态 API，改为显式 Fabric service 委托。 |
| `common/src/main/java/io/github/mortuusars/exposure/PlatformHelperClient.java` | 保留客户端静态 API，改为客户端 service 委托。 |
| `common/src/main/java/io/github/mortuusars/exposure/Register.java` | 保留注册入口，改为类型化 Fabric registry service 委托。 |
| `common/src/main/java/io/github/mortuusars/exposure/network/Packets.java` | 保留 packet API，改为 Fabric packet service 委托；使用 `ServerLevel.getServer()` 替代私有字段访问。 |
| `fabric/src/main/java/io/github/mortuusars/exposure/fabric/PlatformHelperImpl.java` | 实现 `PlatformHelper.Service`，保留原 Fabric 语义。 |
| `fabric/src/main/java/io/github/mortuusars/exposure/fabric/PlatformHelperClientImpl.java` | 实现客户端 service。 |
| `fabric/src/main/java/io/github/mortuusars/exposure/fabric/RegisterImpl.java` | 实现 registry service，并对齐 common 泛型合约。 |
| `fabric/src/main/java/io/github/mortuusars/exposure/network/fabric/PacketsImpl.java` | 实现 packet service，不修改协议。 |
| `fabric/src/main/java/io/github/mortuusars/exposure/fabric/ExposureFabric.java` | 在 common 初始化前绑定三个服务。 |
| `fabric/src/main/java/io/github/mortuusars/exposure/fabric/ExposureFabricClient.java` | 在客户端 common 初始化前绑定客户端服务。 |

新增验证产物：

- `phase4-common-baseline.log`
- `phase4-common-compile-final.log`
- `phase4-fabric-compile-final.log`
- `phase4-clean-build-final.log`
- `phase4-classtweaker-validation.log`

## 编译结果

### 基线 `./gradlew :common:compileJava`

- common 进入 javac。
- 达到 `1,000 个错误`显示上限，真实数量至少为 1000。
- 发现 13 个唯一的 `CameraHolder` / `CameraOperator` 玩家转换错误。

### 最终 `./gradlew :common:compileJava`

- common 进入 javac。
- 完整报告 965 个错误，不再触及 1000 条上限。
- `CameraHolder` / `CameraOperator` 玩家转换错误：0。
- Architectury annotation / binding 错误：0。
- `Packets.java` 错误：0。
- 错误数量至少下降 35 条；其中 13 个明确属于本阶段目标的类型转换错误已验证消失。

### 最终 `./gradlew :fabric:compileJava`

- Gradle 配置和依赖解析成功。
- 先执行项目依赖 `:common:compileJava`，在 common 的 965 条源码错误处失败。
- `:fabric:compileJava` 自身 javac 尚未开始，因此 Fabric 实现中的 26.1 API错误仍需在 common 继续压缩后展开。
- 没有新的构建脚本级错误。

### 最终 `./gradlew clean build --continue`

- common 进入 Java 编译。
- fabric 的 clean、资源处理、include jars、sources jar 等任务执行。
- 失败任务：`:common:compileJava`、`:common:validateAccessWidener`、`:fabric:validateAccessWidener`。
- 两个 Class Tweaker 验证任务只报告原有旧成员目标；新增接口注入条目有效。
- fabric Java 编译仍受 common 项目依赖阻断。

## 剩余错误类别

对最终 965 条 javac 诊断做关键词/路径归类（类别可能重叠）：

- 客户端渲染、UI、模型、截图/取景器：约 610 条，当前最大错误簇。
- 相机核心文件中的 26.1 基础/实体 API：约 66 条；本阶段没有改业务逻辑。
- NBT、ExtraData、SavedData 与存储：约 41 条。
- Mixin target/方法签名：约 26 条。
- Fabric API 事件/注册调用：约 25 条。
- Advancement、Criterion、predicate：约 10 条。
- networking payload/codec：约 5 条。

与 bridge 类同名但未处理的诊断：

- `PlatformHelperClient` 的 `BakedModel` / `ModelResourceLocation` 属于 26.1 客户端模型与渲染迁移。
- `Register` 的 `ItemSubPredicate` 属于 Advancement/predicate API迁移。
- `RegisterImpl`、`PacketsImpl` 中可能存在的 Fabric 26.1 API错误要等 fabric javac 真正开始后才能完整确认。

## 下一阶段建议

下一阶段不建议立刻进入渲染这个最大且高耦合的错误簇。建议先选择边界较清晰的一组：

1. **Advancement / Criterion / predicate 注册迁移**：可同时解除 `Register` 的剩余系统类型错误。
2. **Mixin 方法签名审计**：只处理非渲染、非网络的简单 official target/signature，继续压缩 common。
3. 当 common 错误足够少、fabric javac 能启动后，先审计 `RegisterImpl` 和平台菜单 API，再规划 networking。
4. ExtraData/NBT 与渲染分别作为独立高风险阶段处理。

## 核心业务误伤风险

风险较低。接口注入恢复的是旧 Architectury 已存在的类型关系；真实行为仍由原 `PlayerMixin`、`ServerPlayerMixin`、`LocalPlayerMixin` 实现。平台 bridge 只改变绑定方式，不改变调用方 API 或业务数据。

需要后续验证的运行时风险只有初始化顺序与 Fabric 实现 API版本：当前入口已明确保证先 bind 再 init，并在漏绑/重复绑定时快速失败。照片保存、照片图像编码、相机文件格式、网络包内容、字段顺序和渲染管线均未触碰。
