# 00. 仓库基线与风险地图

## 结论摘要

仓库已具备完整的模组源码、资源、发布脚本和大量本地依赖参考，且当前工作区干净，适合开展系统性优化。当前最值得优先治理的不是单个微优化，而是建立自动化验证边界、整理 Mixin 风险面、拆分少数超大核心类，以及明确 `createbiotech` 与内置 `phantom` 代码的模块边界。

本章只记录全仓基线和可由仓库结构直接确认的风险信号；具体实现建议将在后续专项审计中进一步核验。

## 1. 项目与依赖基线

来自 `gradle.properties` 和 `build.gradle` 的当前版本：

| 组件 | 当前版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge / LegacyForge | 47.1.33 |
| Java | 17 |
| Create | 6.0.8-291 |
| Registrate | MC1.20-1.3.3 |
| Flywheel | 1.0.5 |
| Vanillin | 1.0.0 |
| Ponder | 1.0.91 |
| JEI | 15.20.0.130 |
| Jade | 11.13.2+forge-1.20.1 |
| MixinExtras | 0.4.1 |

本地 `ref/Create/gradle.properties` 与项目的 Minecraft、Forge、Create、Registrate、Flywheel 和 Ponder 版本一致，可作为 Create 相关行为的精确参考。`ref/jei/gradle.properties` 同属 Minecraft 1.20.1，但其 Forge 基线为 47.3.1，且本地快照未提供可直接确认的 JEI 构建号；因此 JEI 代码只能先视为同版本线的近似参考，涉及二进制签名或具体内部实现时必须额外核验。

`ref/SOURCES.md` 当前不存在，无法统一确认每个参考目录的来源、提交、分支和版本 caveat。

## 2. 仓库规模

基准提交共跟踪 1,119 个文件，主要组成如下：

| 类型 | 数量 |
| --- | ---: |
| Java | 425 |
| JSON | 464 |
| PNG | 157 |
| Ponder NBT | 29 |
| OBJ / MTL | 14 |
| Markdown | 9 |

Java 源码共 425 个文件、约 62,369 个物理行，全部位于 `src/main/java`；没有 `src/test` 测试文件。`src/main/resources` 约有 665 个资源文件，其中包括 226 个模型、147 个纹理、84 个配方、53 个方块状态、48 个战利品表、29 个 Ponder 场景和 28 个进度。

源码大致分为两组：

- `com.nobodiiiii.createbiotech.*`：主体模组，包含内容、Mixin、Ponder、兼容层、客户端、注册与基础设施代码。
- `com.yision.phantom.*`：内置的 Phantom 物流、方块、实体、客户端、Mixin、配置和网络代码。

这两组代码共享同一个构建产物、refmap 和运行时生命周期，但包名、来源与职责明显不同，后续需要专项核对其耦合边界和许可/同步策略。

## 3. 已确认的风险信号

### 3.1 缺少持续集成与自动化测试

仓库未发现 GitHub Actions 或其他 CI 配置，也没有测试源码。当前质量验证主要依赖本地 Gradle 任务、`test.py` 和人工客户端运行。

影响：

- 425 个 Java 文件和 464 个 JSON 文件的改动无法在提交或拉取请求阶段自动发现基础回归。
- 58 个 Mixin 类依赖目标方法、描述符和加载环境，纯人工验证难以稳定覆盖。
- 发布脚本、资源替换和可选依赖组合缺少可重复的最小验证门槛。

初步建议：P1。先建立不启动客户端的快速流水线，至少运行 Java 编译、资源处理、完整构建和基础静态资源校验；再逐步补充纯逻辑单元测试与最小 Forge 游戏测试。具体任务组合将在构建审计中确认。

### 3.2 少数超大类形成高变更风险区

Java 文件行数分布：

| 区间 | 文件数 |
| --- | ---: |
| 少于 300 行 | 387 |
| 300–499 行 | 23 |
| 500–999 行 | 11 |
| 1,000 行及以上 | 4 |

最大的几个文件包括：

- `CreeperBlastChamberBlockEntity.java`：约 3,165 个物理行。
- `GeneratedPonderSupport.java`：约 2,179 个物理行，属于生成代码，应优先治理生成流程而非人工拆分。
- `SpiderAssemblyTableBlockEntity.java`：约 1,198 个物理行。
- `SlimeBeltInventory.java`：约 1,165 个物理行。

影响：核心状态机、同步、配方处理、渲染或物品传输逻辑更容易在同一类中互相干扰，代码审查和回归定位成本较高。

初步建议：P1/P2。先为手写超大类绘制职责和状态转换图，再按“持久化/同步、配方匹配、运行状态机、输入输出、表现层数据”提取边界；生成文件则补齐来源、生成命令和禁止手改标记。

### 3.3 Mixin 表面积较大

两个 Mixin 配置共声明 58 个 Mixin 类，其中包含服务端通用逻辑、客户端渲染、访问器以及对 Create 和 Phantom 行为的修改；相关源码中可检索到约 187 处 Mixin 注解或目标声明。

影响：Create 小版本升级、映射变化、可选模组缺失、客户端/服务端类加载边界和注入顺序都可能造成启动失败或静默行为变化。

初步建议：P1。建立 Mixin 清单，逐项记录目标类、目标成员、remap 决策、客户端边界、失败策略和对应功能；随后对照 `ref/Create/`、`ref/jei/`、Forge 1.20.1 参考源码与生成 refmap 核验。

### 3.4 本地参考源缺少统一来源清单

`ref/` 包含 Create、JEI、Catnip、Create Mobile Packages、Create Enchantment Industry、Create Phantom、Forge、Minecraft 等参考目录，但 `ref/SOURCES.md` 缺失，且只有部分目录保留可读取的 Git 元数据。

影响：未来维护者难以判断某段参考代码是精确版本、邻近版本还是临时快照，容易把内部实现误当成当前依赖的稳定 API。

建议：P1。新增 `ref/SOURCES.md`，至少记录目录、上游 URL、分支/标签、提交、对应 Minecraft/加载器/模组版本、获取日期以及是否允许作为精确实现依据。该文件本身不需要提交 `ref/` 内容，只用于固定审计语境。

### 3.5 生成资产与手写资产的边界不清晰

项目配置了 `src/generated/resources` 作为数据生成输出目录，但当前没有被跟踪的生成资源；大量配方、模型、战利品表、进度和 Ponder NBT 位于 `src/main/resources`。同时源码中存在约 2,034 行的 `ponder/generated/GeneratedPonderSupport.java` 和多个生成场景类。

影响：无法仅从目录判断哪些文件应手工维护、哪些文件可重建；重新运行生成器可能导致覆盖、漂移或不可复现的差异。

建议：P2。为每类生成资产记录生成入口、输入源、输出目录和提交策略；对生成 Java 增加明确头注释，并提供可重复的校验命令。

### 3.6 模块来源与维护边界需要显式化

`com.yision.phantom.*` 与主体代码共同打包，另有独立的 `create_biotech_phantom.mixins.json`，仓库文档中也保留 Create Phantom 授权材料。这说明项目已经注意来源许可，但技术层面的边界仍需进一步固化。

建议：P2。记录 Phantom 子系统的上游基线、允许修改范围、与主体注册/网络/配置的连接点，以及未来同步上游时的冲突处理流程。若它必须长期共同发布，可考虑通过 Gradle source set、独立内部模块或至少明确的 facade 降低交叉依赖。

## 4. 首轮实施顺序

1. P1：补齐参考源清单并冻结依赖/参考版本语境。
2. P1：建立最小 CI 与无客户端构建验证。
3. P1：完成 58 个 Mixin 的兼容性和映射审计。
4. P1/P2：拆解三个手写千行核心类的职责与状态机。
5. P2：明确生成资源、Ponder 生成代码和手写资源的工作流。
6. P2：固化 Phantom 子系统的来源、许可和技术边界。

## 5. 后续核验项

- 执行干净环境下的 `compileJava`、`processResources` 与 `build`，记录耗时、警告和可复现性。
- 检查依赖声明、运行时打包方式、版本范围、Mixin annotation processor 和生成 refmap。
- 扫描核心方块实体、传送带、配方与网络代码的 tick 热点、分配热点和同步粒度。
- 校验 JSON、语言键、模型引用、配方/标签以及客户端专属类加载边界。
- 审计 `publish.py`、`test.py`、README 与发布元数据之间的一致性。
