# Exposure 26.1.2 Fabric 迁移报告：Phase 10（客户端非绘制 UI 基础 API）

日期：2026-07-14  
目标：只迁移 Screen 生命周期外围、输入事件、控件回调、文本编辑、焦点和菜单 UI 绑定；不迁移任何绘制、模型、Shader、截图、取景器、网络或存储代码。

## 基线与结果

| 检查项 | Phase 9 基线 | Phase 10 结果 |
| --- | ---: | ---: |
| `:common:compileJava` Java 错误 | 781 | 673 |
| 变化 | — | **-108** |
| `:fabric:compileJava` | 被 common 阻断 | 仍被 common 阻断，未进入 Fabric javac |
| `clean build --continue` | 失败 | 失败：673 个 common 源码错误，另有既有 11 条 access widener 目标错误 |

本阶段的可见错误下降来自旧的 `int`/`char`/坐标输入签名迁移至 26.1.2 事件对象。没有删除 Screen、Widget、菜单交互、C2S 请求、文本编辑分支或照片业务逻辑。

## 已完成的 UI 基础迁移

### 输入事件与修饰键

Minecraft 26.1.2 的键盘和鼠标控件入口已由散列参数改为事件对象。本阶段将可分离的 UI 入口改为：

- 键盘：`KeyEvent`；保留 key、scancode、modifier 的原有匹配语义。
- 字符：`CharacterEvent`；`TextFieldHelper` 直接接收原始字符事件。
- 鼠标点击：`MouseButtonEvent` 加 `doubleClick` 参数；保留左、右键、双击和点击区域判断。
- 鼠标拖动：`MouseButtonEvent` 加 delta；保留相册笔记选择、图像查看拖动和缩放状态逻辑。
- 滚轮：保持 26.1.2 仍有效的坐标和 delta 参数，不改动原有页码/缩放行为。
- 修饰键：键盘事件使用 `KeyEvent.hasShiftDown()` / `hasControlDown()`；鼠标事件使用 `MouseButtonEvent.hasShiftDown()`。不再依赖已移除的 `Screen.hasShiftDown()` / `hasControlDown()`。

`KeyBinding`、`KeyBindings` 增加事件对象入口，内部继续委托现有 `Key` 匹配器。因此原有快捷键、按下/释放阶段和 handler 消费返回值保持不变。

### 文本框、焦点和文本编辑

`TextBox` 已迁移为事件对象驱动：

- `keyPressed(KeyEvent)`、`charTyped(CharacterEvent)`、`mouseClicked(MouseButtonEvent, boolean)`、`mouseDragged(MouseButtonEvent, double, double)`。
- 方向键、Home/End、Backspace/Delete、Enter、选择、复制/粘贴和 Ctrl/Shift 的原优先级保持不变。
- 文字换行高度检查改为对 `Component.literal(text)` 计算，保留原最大高度和禁止换行规则。
- 相册签名标题的高度校验同样改为 `Component` 输入，没有修改标题字段、长度约束或提交流程。

### Screen、菜单和控件绑定

以下非绘制控制路径已迁移：

- `AlbumScreen`：添加照片状态下的取消、右键清空笔记、TextBox/TextBlock 焦点切换、拖动、释放、Tab/Escape/背包键和页码快捷键分发。
- `AlbumSigningScreen`：Tab、Escape 和标题文本框的输入分发。
- `AlbumViewScreen`、`LecternAlbumScreen`、`ChildPhotographScreen`：相册页交互、书架页码跳转、返回父界面的键鼠行为。
- `PhotographSlotWidget`：主/次操作、滚轮主操作、确认键和 Shift 次操作；确认键改用 `KeyEvent.isSelection()`。
- `LightroomScreen`、`CameraAttachmentsScreen`、`ItemListScreen`、`ItemRenameScreen`：菜单按钮、F1 链接、筛选/镜头/胶卷列表、背包关闭、右键清空命名、确认命名和菜单点击的输入入口。
- `FilmFrameInspectScreen`、`PhotographScreen`、`LightroomFrameInspectScreen`：非绘制的键盘绑定、滚轮和拖动入口。
- 自定义 `CycleButton` 及测试用 `Slider`：点击、右键复位、确认键、方向键、焦点和拖动的事件签名。

所有菜单按钮仍通过原有 `clickMenuButton` / `handleInventoryButtonClick` 与既有 packet 路径发起合法请求；本阶段没有修改 `Packets`、payload、codec、ID、字段顺序或 receiver。

### CameraItem Shift 判断

`CameraItem` 的两处 tooltip 明细 Shift 判断已改为 `Minecrft.get().hasShiftDown()`：

- 相机架上的附件/热切换提示；
- 手持相机附件/热切换提示。

仅迁移公开输入状态读取；未触及 tooltip 绘制、相机状态、EnderMan 逻辑或 Class Tweaker。

## 修改文件

| 文件 | 修改原因 |
| --- | --- |
| `common/src/main/java/io/github/mortuusars/exposure/client/input/KeyBinding.java` | 增加 `KeyEvent` 按下/释放入口，复用原始匹配器。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/input/KeyBindings.java` | 为 Screen 事件分发增加 `KeyEvent` 入口。 |
| `common/src/main/java/io/github/mortuusars/exposure/world/item/camera/CameraItem.java` | 迁移两处 Shift tooltip 状态检查。 |
| `common/src/main/java/io/github/mortuusars/exposure/world/item/InterplanarProjectorItem.java` | 迁移投影仪详情 tooltip 的 Shift 状态检查。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/ClientGUI.java` | 迁移配方详情 tooltip 的 Shift 状态检查。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/component/CycleButton.java` | 迁移点击和键盘轮换输入。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/element/textbox/TextBox.java` | 迁移文本编辑、选择、拖动和文本高度校验。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/element/TextBlock.java` | 迁移文本链接点击事件。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/AlbumScreen.java` | 迁移相册菜单键鼠、焦点及笔记交互。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/AlbumSigningScreen.java` | 迁移标题编辑与 Escape/Tab 控制。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/AlbumViewScreen.java` | 迁移只读相册点击和快捷键。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/ChildPhotographScreen.java` | 迁移返回/右键关闭输入。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/LecternAlbumScreen.java` | 迁移讲台相册页码点击。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/PhotographSlotWidget.java` | 迁移照片槽点击、确认键和滚轮操作。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/camera/CameraAttachmentsScreen.java` | 迁移 F1 与附件区域点击。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/FilmFrameInspectScreen.java` | 迁移非绘制按键和拖动入口。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/ItemListScreen.java` | 迁移背包键关闭及外部点击关闭。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/ItemRenameScreen.java` | 迁移命名输入、确认和右键清空。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/LightroomFrameInspectScreen.java` | 迁移右键缩放退出入口。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/LightroomScreen.java` | 迁移按键绑定、打印 Shift 状态及中心区域点击。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/PhotographScreen.java` | 迁移非绘制按键绑定和拖动入口。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/test/Slider.java` | 迁移测试控件的焦点、点击、拖动与键盘操作。 |

## 明确未处理的范围

以下错误被记录但未修改，留给后续专门阶段：

- `GuiGraphicsExtractor` 的 `extractRenderState` / `extractWidgetRenderState` / `extractContents`、`blit`、`drawString`、tooltip 绘制、pose/scissor 与二维渲染状态。
- `RenderType`、`RenderStateShard`、`RenderPipeline`、顶点、模型、`BakedModel`、实体渲染器和 item model resolver。
- Shader、`GameRenderer`、`RenderTarget`、截图捕获、取景器、照片图像绘制和图片渲染器。
- 网络目录中遗留的两条 Shader handler 错误；网络 payload、codec、注册、发送和 receiver 均未触及。
- ExtraData、SavedData、NBT、照片路径、图像编码、Frame 结构和相机拍摄算法。
- 既有 access widener 的 11 条失效旧目标：`RenderType`、`RenderStateShard`、`GameRenderer`、`ItemProperties` 与 `EnderMan`；未改动文件或条目。
- `TestImageScreen` 的旧拖动入口仍与截图/图像测试路径耦合，按本阶段限制未修改。

## 验证

已执行：

```text
./gradlew :common:compileJava
./gradlew :fabric:compileJava
./gradlew clean build --continue
```

结果：

- `:common:compileJava` 失败，准确错误数为 673；错误数未再触及 javac 1000 条上限。
- `:fabric:compileJava` 只执行到依赖任务 `:common:compileJava`，因此 Fabric javac 尚未开始；没有新增 Fabric 实现层错误证据。
- `clean build --continue` 重现 `:common:compileJava` 的 673 条源码错误，并重现 `:common:validateAccessWidener` 与 `:fabric:validateAccessWidener` 的既有验证失败。
- 静态搜索确认：已参与本阶段 UI 的 Screen/Widget 输入 override 不再保留旧的散列参数签名；保留的旧签名仅在内部 `KeyBinding` 适配层，以及被明确冻结的 `viewfinder` 和 `TestImageScreen` 路径。
- 本阶段 UI 范围内未新增 GLFW 直接轮询、空 payload、固定事件返回、no-op 或 stub；现有 `return null` 位于原有的可空样式/渲染提供方，未修改。

当前 673 条总错误中，日志中可定位的首层诊断仍以 UI 绘制抽取接口（约 170 条首层定位）、模型（约 29 条）、Shader/截图链路（约 26 条）、渲染与客户端 Mixin 为主；网络仅保留 2 条 Shader 相关错误。该分类为首层文件定位，javac 的连锁诊断会使其不能与总数直接相加。

## 风险评估

- 输入消费风险：低。自定义处理仍先于或按原顺序调用 `super`；所有原来返回 `true` 的已处理分支保持原语义。
- 焦点和文本编辑风险：低到中。文本框的 Ctrl/Shift 现在使用同一事件的 modifiers，避免了全局状态与事件不一致；仍需在后续可启动客户端后手测多行选择、双击选词和剪贴板。
- 菜单/合法 packet 请求风险：低。本阶段只保留现有菜单调用和第九阶段的网络边界，没有改 packet 内容或服务端校验。
- 客户端类加载风险：无新增证据。修改集中在既有 client GUI 路径；没有把 client receiver 或 UI 初始化接入 dedicated server 路径。
- 核心业务误伤：未发现。没有更改照片存储、图像编码、网络协议、Frame、相机拍摄、菜单槽位限制或渲染结果。

## 下一阶段建议

下一阶段应专门迁移 **GuiGraphics / Screen 绘制基础**：先处理 26.1.2 的 `extractRenderState`、`extractWidgetRenderState`、二维 pose/collector 以及基础 blit/文本/tooltip 抽取式渲染。应继续把模型、RenderType/RenderPipeline、Shader、截图和取景器保持为独立后续阶段，避免与 UI 绘制基础混合。access widener 的 11 条旧渲染目标也应在对应渲染阶段逐条根据 26.1.2 实际成员重新核对。
