# Minecraft 26.1.2 Fabric 迁移 Phase 25 报告

## 结论

本阶段只处理预发布前的玻璃相框表面预期核对和相册署名 UI 背景。

- `glass_photograph_frame`：不改渲染。固定上游 `9383e0c9237099947a1e6399fcace5772f496c7d` 明确规定：装入任何物品后 `GlassPhotographFrameEntity.isFrameInvisible()` 为 true；共享 renderer 因而只渲染照片，不提交 clear/glass 模型。照片替代正面玻璃 pane/反光是上游语义，不应添加半透明白层、伪造高光或改变照片深度。
- `AlbumSigningScreen`：已修复。26.1.2 port 删除旧 blit 的 z 参数时，将旧的 `z=0,u=298,v=0` 错当成新 API 的 `u=0,v=298`；后者在真实 512x512 atlas 中完全透明。现在从真实署名面板 `(298,0,149,188)` 采样。

未执行 `clean`，未启动 dedicated server，未修改用户全局 Java/Gradle/启动器/环境。工作区不是 Git 仓库；本报告只使用文件清单、哈希和日志，不声称 Git diff、commit 或 branch。

## 输入、基线和只读截图

- 已完整读取 Phase 23、Phase 24 报告，并仅定向查阅 Phase 18 的 GUI/render-state 记录。
- Phase 18 的相关规则确认普通 Screen 已迁到 `GuiGraphicsExtractor` 的 `extractRenderState`/`extractBackground`、`GUI_TEXTURED` 与明确 stratum；本次背景仍遵守该生命周期，只改 atlas source region。
- Java 25.0.3、项目 Gradle Wrapper 9.4.0、`GRADLE_USER_HOME=D:\Codex\Exposure\.gradle-home`（ASCII）。
- Phase 24 输入 JAR 保持不变：`phase24-exposure-fabric-26.1.2-1.9.18.jar`，SHA-256 `166268BE34F8B9ECF97C3280F565B05DD0E0BD7503D2B06BF45C3194C391D96B`。
- 修改前源码/资源清单：`phase25-prechange-source-resource-manifest.csv`（924 entries）。
- 非 clean 基线：`:common:compileJava :fabric:compileJava :common:compileTestJava :common:test --rerun-tasks`，成功；日志 `phase25-baseline-compile-test.log`。

`D:\Codex\Exposure\bugs` 仅只读检查，未移动、改写、打包：

| 文件 | 尺寸 | SHA-256 | 可见异常 |
|---|---:|---|---|
| `带表面玻璃纹理的空白玻璃相框.png` | 2560x1441 | `A4C93F988A0EDBE9E100F65C07B3E9FE57F6CE8CCCA8BBA99FCD5BB471FE3F16` | 空框中央可见原玻璃 pane 的两组高光。 |
| `放置照片后纹理消失的玻璃相框.png` | 2560x1441 | `9B6F8D5DED3CF7BE95CBF0B094F4510F03191C40D161866A9A3E23EB47306787` | 已装片，边框仍可见，但中央高光不再显现。 |
| `只有文本输入、署名与退出交互组件的相册签名界面.png` | 2560x1441 | `4984CEC330D96AD311BD957C45A8A60FAD6B534597FE0217032A952C3B190534` | 署名/取消按钮与输入控件存在，签名面板背景、标签文本不可见。 |

完整机器可读清单：`phase25-bugs-screenshot-inventory.csv`。

## Group 1：玻璃相框

### 固定上游预期

固定上游的共享 `PhotographFrameEntityRenderer` 先渲染照片，再在 `!isFrameInvisible()` 时渲染模型；glass 子类只把模型切换为 clear small/medium/large，并使用 cutout block sheet。上游 `GlassPhotographFrameEntity` 额外返回 `super.isFrameInvisible() || !getItem().isEmpty()`。

因此：

- 空框：提交对应 size 的 clear 模型；其中央 pane、边缘、1px 厚度及 cutout 高光可见。
- 已装彩色照片或黑白照片：照片替代正面玻璃模型；两种照片、所有 size 和四个 `itemRotation` 走同一 occupied 语义，没有上游的额外玻璃覆盖层。
- size 只选择 small/medium/large 模型并决定照片尺寸；旋转只旋转照片 quad，不改变 glass 资源或创建新的透明 pass。

三个 glass block model 均有中央 pane element（small：`[2,2,15.85]..[14,14,16]`；medium/large 为同构扩大版本），而不是只有框边；该 pane 使用现有 cutout 纹理的 north/south face。small/medium/large 模型、四张关联纹理和 `album.png` 与固定上游逐字节一致；审计见 `phase25-upstream-resource-hash-audit.csv`。玻璃纹理是 cutout 资源（无半透明像素），不是可由任意 alpha 覆盖层替代的素材。

### 当前 26.1.2 链与决定

Phase 24 当前链确为“先提交动态照片，再提交 clear frame 专用 `cutoutMovingBlock` 模型”。`PhotographFrameEntityRenderer` 中照片使用 `entityTranslucent`，模型是不同 RenderType；collector 的实际 pass 排列不以 Java 调用顺序保证最终覆盖关系。照片 z center 为 `0.48 - configuredOffset`，pane 前表面约为 `15.85/16 - 0.5 = 0.490625`，所以不是照片 quad 尺寸、旋转或资源缺失。

Phase 24 为修复“装片后整框消失”而保留了 clear 模型；这与固定上游的 occupied-invisible 语义不同，但本阶段受限于不得回退该已验证的框体可见性修复。截图中中央高光在装片后不显现，不能据此添加新玻璃效果：固定上游本来就让装片照片替代整块正面模型。没有修改 glass renderer、模型、纹理、RenderType、照片 offset，也没有触碰普通 `photograph_frame`。

Phase 24 已有无 Shader 与 Iris+Sodium+BSL 的 glass 结构通过证据。本阶段无 glass 代码或资源改变，故没有重建 Iris 矩阵或启动客户端；用户使用新 JAR 复测实际 shader/mod 组合即可。

## Group 2：AlbumSigningScreen

固定上游界面是 `AlbumSigningScreen`：透明世界背景之上绘制 149x188 的棕色相册署名面板、"enter title" 标题、作者署名、居中输入框、署名按钮、取消按钮及按钮 tooltip/warning。它不是 Signed Album 查看 Screen，也不改签名数据或服务端逻辑。

旧 `GuiGraphics` 调用参数的前两个数为 `z=0,u=298`，接着是 `v=0`。迁移后的 `GuiGraphicsExtractor` 没有该 z 参数，当前旧写法错误取样了 `(u=0,v=298)`：真实 atlas 的 149x188 区域 non-transparent pixels 为 0；正确 `(298,0)` 区域为 27,801。控件仍工作正是因为 widgets 生命周期正常，而 `extractBackground` 提交了透明区域。

修复只将签名背景定义为 `AlbumGUI.SIGNING_BACKGROUND = (298,0,149,188)`，并通过现有 `TextureRegion.blit` 使用完整 destination/source/atlas 语义：destination 149x188、source 149x188、atlas 512x512。背景仍在 `extractBackground`，先于 `super.extractRenderState` 的输入框和按钮；没有增加 stratum、改变 Screen 生命周期、字符限制、Component、Album 数据、菜单或 packet。

静态证据已完全定位为单一 atlas 参数错误，依照本阶段的节省策略没有额外启动客户端。新增回归测试同时断言正确 region 和透明旧 region；用户最小人工复测应打开一次署名界面、签署临时相册、取消一次，并在另一个 GUI scale 重开确认布局。

## 修改文件

1. `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/AlbumGUI.java`
   - 增加明确的 `SIGNING_BACKGROUND` source region。
2. `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/AlbumSigningScreen.java`
   - 用该 region 绘制背景，修正旧 z/UV 语义漂移。
3. `common/src/test/java/io/github/mortuusars/exposure/client/gui/screen/album/AlbumGuiResourceTests.java`
   - 新增 atlas 采样回归测试。

修改前后 SHA-256 清单：`phase25-source-resource-changes.csv`。没有其他生产资源、glass 代码或普通木框代码改动。

## 验证与交付

- 定向编译与新测试：`:common:compileJava :fabric:compileJava :common:compileTestJava :common:test --tests ...AlbumGuiResourceTests` 成功；`phase25-g2-signing-background-test.log`。
- 最终非 clean 完整 build：`gradlew.bat --rerun-tasks build` 成功；common/Fabric compileJava、common compileTestJava、common/Fabric Access Widener、全部 build 均实际执行；日志 `phase25-final-full-gates-build.log`。
- JUnit XML：19 suites，58 tests，0 failures，0 errors，0 skipped（原 57 加 1）。
- 未启动客户端或 dedicated server：仅 GUI source/atlas 抽样修复，且静态证据精确充分；这符合本阶段允许直接编译、测试和交付的最小化条件。没有宣称未执行的运行时截图验证。
- 进程审计：0 个 KnotClient/KnotServer/dev-launch-injector；唯一 Java 进程为 Gradle daemon，见 `phase25-final-process-audit.txt`。
- JAR 内容审计：1,232 entries；没有 bugs 截图、`phase25-`、测试相册/自动操作路径或 `D:\Codex\Exposure` 绝对路径内容。25 个生产 shader entries 是既有合法资源，并非临时 shader。审计文件：`phase25-final-jar-entries.txt`、`phase25-final-jar-prohibited-entry-hits.txt`、`phase25-final-jar-content-hits.txt`。

新 Fabric JAR（未覆盖 Phase 24、未安装到正式实例）：

```text
Path:     D:\Codex\Exposure\phase25-exposure-fabric-26.1.2-1.9.18.jar
Size:     2,058,846 bytes
Modified: 2026-07-19 11:59:28 +08:00
SHA-256: 2866EDA2D2CC721EFEA6D176367C4373CCAF4D1F22C27F633641CDA98A86A421
```

## 未修改边界、失败尝试与交接

未修改 packet ID/方向/字段顺序/StreamCodec，未激活 `query_exposure_data`，未改 menu open-data envelope；未改 SavedData、ExtraData、Frame、ExposureData、CameraId、Album/Signed Album 保存格式、照片/像素/调色板/曝光/胶片参数；未回退 Phase 22 Capture、Phase 23 色彩、Phase 24 相册页面、命令、配方或已恢复的 glass 框体可见性。

没有功能性失败尝试，也没有为视觉效果添加试探性覆盖层。唯一审计脚本的首次内存哈希 API 在本机 PowerShell 不可用，立即改用兼容 `SHA256.Create().ComputeHash` 重跑；未改动任何游戏文件或资源。

状态交接：已完成固定上游语义核对、署名背景最小修复、资源级回归测试、58/58 测试和完整 build，并保存 Phase 25 JAR。关键决定是将玻璃高光消失标记为固定上游 occupied 设计，而不是新 bug；署名问题为已修复的 UV 参数迁移缺陷。遗留风险仅是用户实际 shader/mod 组合的透明排序与最终视觉观感，未声称已在本阶段重建全矩阵。

下一步默认由用户使用 Phase 25 JAR，只复测：

1. 空白和装片 glass frame（彩色、黑白各一；确认木框未受影响）；应接受装片后照片替代正面 glass pane 的上游设计。
2. Album 署名界面：背景、标题、作者文本、输入框、署名/取消按钮；分别检查空文本、普通文本、既有限制边界、签名后查看页、取消返回，以及一个额外 GUI scale。

若这两项通过，即可进入预发布准备。
