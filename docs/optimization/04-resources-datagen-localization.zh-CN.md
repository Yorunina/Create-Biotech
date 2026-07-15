# 04. 资源、数据生成与本地化审计

## 结论摘要

资源树整体可读性尚可：扫描到的 460 个 JSON 均能解析且没有重复键，154 个 PNG 均可解码，28 个 advancement 的本地 parent 全部存在，84 个配方、48 个方块战利品表和 28 个 advancement 也没有重复资源 ID。当前客户端日志中与本模组直接相关的资源加载错误只有一个，但它是可稳定复现的真实缺陷：薛定谔的猫方块模型继承了不存在的 `small_cardboard_box` 模型。

资源维护边界则比较模糊：数据运行任务指向一个未接入主资源集的空 `src/generated/resources`，仓库没有 Forge datagen provider；The Ponderer 的生成 Java、NBT 和语言键直接提交到主源码/资源树，但缺少可复现的导出版本与流程说明；`en_us` 中还有 28 个中文 Ponder 文案。项目同时在 `assets/create` 下携带 23 个 Create packager 资源，其中 21 个与当前精确匹配的 Create 6.0.8 参考文件逐字节相同，会扩大未来升级时的覆盖风险。

## 1. 扫描结果

| 检查项 | 结果 |
| --- | --- |
| JSON 语法 | 460/460 可解析 |
| JSON 重复键 | 未发现 |
| PNG 解码 | 154/154 成功 |
| `en_us` / `zh_cn` key | 各 436 个，集合完全一致 |
| `en_ud` | 10 个 Butter Cat key |
| `zh_tw` | 35 个 Butter Cat/Allay key |
| 本地 advancement parent | 28 个 advancement，0 个缺失 parent |
| 资源 ID 重复 | 84 个配方、48 个 loot table、28 个 advancement 均无重复 |
| 精确模型/纹理引用 | 发现 1 个本模组缺失模型 parent |

这些静态检查只能证明文件可解析和部分引用可达，不能代替 Minecraft/Forge 的完整资源重载、配方 serializer 或 Ponder 场景运行验证。最近一次客户端日志还包含 `cmpackagecouriers` 与 AE2 的资源告警，它们不属于本仓库问题，本章没有归因到 Create: Biotech。

## 2. 已确认的资源缺陷

### RESOURCE-01：薛定谔的猫模型继承了不存在的 parent

- 优先级：P0/P1
- 文件：`src/main/resources/assets/create_biotech/models/block/schrodingers_cat.json`
- 运行证据：`.minecraft/logs/latest.log:78`

模型声明：

```json
"parent": "create_biotech:block/small_cardboard_box"
```

但仓库中不存在 `assets/create_biotech/models/block/small_cardboard_box.json`。最近一次客户端资源加载已经打印：

```text
Unable to load model: 'create_biotech:block/small_cardboard_box' ... FileNotFoundException
```

这会让对应 blockstate 使用缺失模型，不能当作静态扫描误报。建议先确认它本应继承普通纸箱模型、Create 的 package 模型，还是一个遗漏的独立模型；随后补齐 parent 或改成真实存在的模型路径，并执行资源重载/客户端模型检查。修复后应把“扫描所有本模组 model parent 和 texture 引用”的脚本加入 CI，避免只靠肉眼发现。

### RESOURCE-02：`en_us` 中有 28 个中文 Ponder 文案

- 优先级：P1
- 文件：`src/main/resources/assets/create_biotech/lang/en_us.json`

28 个值集中在恶魂热气球装配站/恶魂驾驶台两组生成场景，例如 `create_biotech.ponder.ghast_helm_s_29671.*` 和 `create_biotech.ponder.ghast_hot_air_balloon_assembly_station_scene_1.*`。`en_us` 与 `zh_cn` 虽然都有 436 个 key，但有 29 个值完全相同，其中 28 个包含中文字符。

这意味着英语环境会直接显示中文，而 key 集合对齐检查无法发现问题。建议：

1. 在 Ponderer 的场景源或导出前翻译这些文本，不要只手改生成文件；否则下一次导出会覆盖修复。
2. 增加语言 lint：`en_us` 禁止 CJK 字符（允许名单除外），并检查 `%s`、`%d`、位置参数等占位符在主要语言间一致。
3. 将生成文案与人工维护文案分区或加清晰前缀，便于审查生成 diff。

当前 `en_us` 与 `zh_cn` 的格式化占位符未发现不一致。

## 3. 数据生成边界

### RESOURCE-03：配置了 data run，但仓库没有可执行的 datagen provider

- 优先级：P1
- 文件：`build.gradle`

`legacyForge.runs.data` 把输出指向 `src/generated/resources/`，但 Java 源码中没有 `GatherDataEvent`、`RecipeProvider`、`LootTableProvider`、`BlockStateProvider`、`TagProvider` 等注册；当前 `src/generated/resources` 也为空。因此这个 run 配置表面存在，实际不能重建仓库中的 162 个 `data/create_biotech` JSON 或客户端模型/语言资源。

另外，`src/generated/resources` 没有加入 `sourceSets.main.resources`，即使未来 provider 开始产出内容，生成结果也不会自动进入发布 JAR。该打包问题也记录在 `01-build-dependencies-release.zh-CN.md`。

建议二选一明确治理方式：

- 若采用 datagen：实现 provider，把生成目录接入主资源集，CI 中运行 datagen 后检查 `git diff --exit-code`，保证提交结果可重建。
- 若继续手工维护：删除或注释无效 data run，避免贡献者误以为资源可以自动生成，并提供专用的 JSON/引用 lint。

不要让一部分资源手写、一部分资源“理论上生成”但没有单一来源；这会让升级和大规模重命名最容易产生静默遗漏。

### RESOURCE-04：Ponder 生成物缺少可复现导出说明

- 优先级：P2
- 路径：
  - `src/main/java/com/nobodiiiii/createbiotech/ponder/generated/`
  - `src/main/resources/assets/create_biotech/ponder/generated/`

仓库提交了 The Ponderer 自动生成的 Java 场景、支持类和约 28 个生成 NBT；文件头明确写着“下次导出时会被覆盖”。`GeneratedPonderForgeClient` 会通过 MOD event bus 自动注册生成插件，所以这些文件不是死代码。仓库也跟踪了 `.ponderer-export/manifest.json` 与 `last-report.json`，但它们没有记录准确的 exporter 版本/命令，并且已经与实际输出漂移：manifest 声明的 `ghast.nbt`、`rope.nbt` 不存在，135 个 manifest `en_us` key 中有 3 个在实际文件缺失、102 个值不同。仓库仍缺少：

- The Ponderer 的准确版本；
- 场景源文件/编辑工程在哪里；
- 导出命令与选项；
- 导出后需要执行的格式化、语言合并和验证步骤；
- 如何判断旧的 hash 后缀 Java/NBT 已经失效并应删除。

建议增加 `docs/development/ponder-export.md`，固定工具版本和导出清单；先重建/同步 manifest，再把生成目录视为不可手改区域，并用一个校验脚本检查 manifest owned files、`GeneratedPonderIndex` 引用的类、Java 中的场景 NBT 和语言 key 是否齐全。人工扩展继续放在 `foundation/ponder`、`infrastructure/ponder` 或 `PonderSupportExt`，不要混入生成目录。

### RESOURCE-05：手写资源缺少注册项完整性门禁

- 优先级：P2

当前静态扫描证明 JSON 可解析、advancement parent 存在，但不能自动回答以下问题：

- 每个应有物品形态的注册方块是否有 blockstate、block model、item model、loot table 和语言 key；
- 每个 recipe serializer/type 是否能被 Forge 实际解码；
- 标签中的本模组 ID 是否真实注册；
- Ponder NBT 是否能加载对应方块实体；
- 自定义模型 loader、OBJ/MTL 和 atlas sprite 是否在资源重载时可用。

建议建立分层门禁：纯 Python/Java lint 做语法、重复键、引用和语言检查；Forge GameTest 或最小资源加载测试做 serializer/registry 验证；发布前再做一次人工资源重载与 JEI/Ponder 抽样。不要把完整客户端启动作为每个普通 Java 改动的默认门禁。

## 4. 命名空间与覆盖风险

### RESOURCE-06：`assets/create` 中有 21 个逐字节相同的 Create 资源覆盖

- 优先级：P1/P2
- 路径：`src/main/resources/assets/create/`

项目在 Create 命名空间下提交了 23 个 packager 相关文件。与精确匹配当前依赖版本的 `ref/Create/src/main/resources` 比较后：

- 10 个 `models/block/packager/*.json` 与 Create 6.0.8 完全相同；
- 11 个 `textures/block/packager_*.png` 与 Create 6.0.8 完全相同；
- `blockstates/packager.json` 和 `models/item/packager.json` 在参考源码主资源目录中没有同路径文件，Create 很可能在自己的 datagen 输出中提供它们，因此不能仅据此判定为项目定制。

同命名空间资源会参与资源包覆盖。即使当前内容相同，未来放宽 Create 版本、升级依赖或改变 mod pack 资源优先级时，这些旧副本也可能覆盖 Create 新资源，造成模型与代码不匹配。

建议先通过提交历史确认复制目的：

1. 若没有故意修改 Create packager 外观，删除这些副本并依赖 Create 自身资源。
2. 若 Mixin 功能确实需要覆盖，保留最小差异文件，在文档中记录目标 Create 版本、差异原因和升级检查项。
3. 对第三方命名空间文件建立 allowlist；CI 报告新出现的 `assets/create`、`data/create`、`assets/minecraft`、`data/minecraft` 文件，要求审查者确认覆盖语义。

本结论使用的 Create 本地参考与项目的 Minecraft、Forge、Create、Flywheel、Ponder 版本精确匹配，因此 21 个文件的比较可视为当前版本的可靠证据。

### RESOURCE-07：Minecraft blocks atlas 文件是合法的叠加，不是整表替换

- 优先级：已核验，无需修复
- 文件：`src/main/resources/assets/minecraft/atlases/blocks.json`

该文件列出 29 个 `single` sprite source，其中包括传送带滚动纹理、流体纹理、connected texture 和实体纹理。把文件放在 `assets/minecraft/atlases/blocks.json` 是给原版 blocks atlas 增加非默认目录 sprite 的正确方式。

本地 Forge/Minecraft 1.20.1 参考 `ref/Minecraft-1.20.1/net/minecraft/client/renderer/texture/atlas/SpriteResourceLoader.java:70-81` 会遍历 `ResourceManager#getResourceStack`，把每个资源包中的 `sources` 依次 `addAll`；因此这里会与原版、Forge、Create 的 atlas 定义合并，而不是替换整个 atlas。`IClientFluidTypeExtensions` 的本地注释也明确要求对默认搜索路径外的流体纹理添加 atlas entry。

建议保留当前命名空间，但为 29 个 `resource` 增加文件存在性 lint；新增同名 sprite 时还要检查后加载 source 覆盖顺序。

## 5. 本地化策略

### RESOURCE-08：`zh_tw` 是局部翻译，未翻译内容会回退到英语

- 优先级：P2

`zh_tw.json` 只有 35 个 Butter Cat/Allay key，而 `en_us`/`zh_cn` 各有 436 个。Minecraft 不会自动把缺失的繁体中文 key 回退到 `zh_cn`；通常最终显示 `en_us`。如果这是有意的社区局部翻译，应在贡献文档中明确“允许部分覆盖”；如果目标是完整繁中支持，应建立 key parity 门禁并逐步补齐。

`en_ud` 只有 10 个 Butter Cat key更接近趣味/测试语言包，允许部分覆盖是合理的，但也应在语言策略中声明，避免通用 parity 检查错误要求它与 `en_us` 完全一致。

### RESOURCE-09：语言 key 已对齐，但应检查语义而不只检查集合

- 优先级：P2

`en_us` 和 `zh_cn` 的 436 个 key 完全对齐是良好基础；本次发现英语文件混入中文，说明“只比 key 数量”不足。建议语言 lint 同时检查：

- key 集合；
- 格式化占位符；
- `en_us` 的异常 CJK 字符；
- 值为空、等于 key 或明显复制错误；
- 生成场景的成组 header/text 序号是否连续；
- 删除注册项后是否残留孤儿翻译。

## 6. 建议实施顺序

1. 修复 `small_cardboard_box` 缺失 parent，并通过客户端资源重载确认日志消失。
2. 修复 `en_us` 中 28 个中文 Ponder 文案，明确生成源再重新导出。
3. 决定 datagen 是真正启用还是删除空壳配置；若启用，同时修复生成资源 source set。
4. 核对并最小化 `assets/create` 覆盖，给第三方命名空间文件建立 allowlist。
5. 固化 Ponderer 版本、源工程和导出/清理流程。
6. 把本次 JSON、PNG、模型引用、语言和 advancement parent 检查整理成可重复的 CI lint。

## 7. 验证清单

- [ ] 所有本模组 model parent 和 texture 引用可解析。
- [ ] `latest.log` 不再出现 `create_biotech:block/small_cardboard_box`。
- [ ] `en_us` 除允许项外不含 CJK 字符。
- [ ] `en_us`/`zh_cn` key 与格式化占位符一致。
- [ ] 生成资源能够从文档化流程完整重建，或无效 data run 已移除。
- [ ] Ponder 生成 index、Java 类、NBT 和语言 key 交叉引用完整。
- [ ] 第三方命名空间覆盖都有目的、版本边界和升级检查说明。
- [ ] Forge 实际加载所有自定义配方 serializer、loot table 和 advancement，无本模组解析错误。
