# 06. 测试、诊断、CI 与开发者体验审计

## 结论摘要

当前 `build` 能成功，但验证能力接近空白：没有 `src/test`、JUnit/GameTest、CI workflow 或静态分析任务；Gradle 的 `test` 是 `NO-SOURCE`，`check` 实际只会运行这个空测试套件。因此“构建绿色”只能证明主源码编译、资源复制和 JAR 组装成功，不能证明网络权限、Mixin 注入、存档迁移、配方加载或客户端资源正确。

诊断方面，多个数据加载和兼容 fallback 会静默吞异常，其中 `AirCourierTaskSavedData` 可直接丢弃无法读取的 courier task而不留日志。开发文档也存在与仓库现实不一致的问题：README 宣称 `ref/` 是 bundled、注册走 Registrate、资源是 datagen 混合产出且存在 `tools/`，但 `ref/` 实际完全被 gitignore、主要注册使用 DeferredRegister、没有 datagen provider，`tools/` 目录也不存在。关键 `AGENTS.md`/`CLAUDE.md` 同样未跟踪，新的 clone 无法获得当前项目约定。

## 1. 当前验证基线

### 已存在

- Gradle wrapper 8.12，wrapper JAR SHA-256 为 `2DB75C40782F5E8BA1FC278A5574BAB070ADCCB2D21CA5A6E5ED840888448046`。
- `compileJava --rerun-tasks --warning-mode all` 成功。
- `build --rerun-tasks --warning-mode all` 成功。
- Mixin annotation processor会生成 `build/mixin/create_biotech.refmap.json`，JAR 已包含 refmap。
- `test.py`/`quickPlayClient` 可构建、复制 JAR并启动本地外部客户端。
- 本轮审计已用一次性脚本验证 JSON、PNG、语言 key、advancement parent 和模型引用。

### 缺失

- `src/test/java` 和 `src/test/resources`；
- JUnit/TestNG 依赖；
- Forge GameTest；
- GitHub Actions、GitLab CI 或其他 CI；
- Checkstyle、SpotBugs、Error Prone、NullAway 等静态检查；
- 资源 lint 的版本化脚本；
- dependency lock/Gradle dependency verification metadata；
- 可超时、可判定成功失败、可清理进程的客户端 smoke test。

`gradlew tasks --group verification` 只有 `check` 与 `test`，而前一次完整 `build` 明确显示 `test NO-SOURCE`。

## 2. 测试策略

### TEST-01：先建立纯 JVM 单元测试层

- 优先级：P0/P1

仓库已有不少不依赖完整 Minecraft world 的确定性逻辑，适合先用 JUnit 5建立快速测试：

- `PhantomAddressRules` 的 canonical、blank、模糊/精确匹配；
- `AirCourierFlightMath`、planner、estimate 和 target geometry；
- `ShulkerTeleporterBlockEntity.normalizeCandidateAddresses` 的长度、去重、顺序与最大 64 项；
- Shulker/Phantom target comparator；
- captured entity box 的纯 NBT helper、ingredient stacking id；
- belt 几何、loop position、connector/track 映射；
- recipe serializer 的边界数据；
- `publish.py` 参数解析和上传失败退出码，可用 mock HTTP/进程层测试。

目标不是追求覆盖率数字，而是先保护高风险纯函数和协议边界。测试应固定随机种子并覆盖空值、极值、重复值、负数、超长字符串和恶意 count。

### TEST-02：为网络包建立 decoder 与授权测试

- 优先级：P0

`02-java-architecture-network.zh-CN.md` 已列出 Shulker Packager placement、Smart Glue 删除和 Shulker Teleporter config 等服务端授权问题。建议对每个 C2S packet建立统一测试清单：

- 负数/超大 VarInt count在分配前拒绝；
- UTF/NBT/集合大小上限；
- sender 为 null、菜单已关闭、维度切换、区块未加载；
- 距离、手持工具/物品、placement token、权限与目标 BE 类型；
- 重放、重复包和乱序包；
- 非法数据不得部分修改 world/BE。

`AirCourierHudPacket` 是 S2C，但 decoder同样应限制 count到 `AirCourierHudPayload.MAX_VISIBLE_ENTRIES` 或协议上限，避免损坏/不兼容服务端导致客户端大分配。`MiniPhantomConfirmPacket` 的 `readUtf()` 也应使用与菜单规则一致的显式最大长度。

### TEST-03：引入 Forge GameTest 覆盖 world 行为和持久化

- 优先级：P1

建议优先覆盖：

- 三段 Shulker Teleporter 的放置、异常中断、拆除、跨维度地址注册和重载；
- Blast Chamber 3/5/上限结构形成、最后一块错误、区块卸载和 pending 操作恢复；
- BioPackager contraption 捕获、disassembly、server restart与箱子不丢失；
- Slime/Magma/Power Belt 的方向反转、connector seam、漏斗/隧道交互；
- SavedData 旧版本、损坏单条记录和未知维度的迁移策略；
- 自定义 recipe serializer、loot table、advancement触发；
- server stopped 后 static manager 清空。

GameTest 需要按功能拆小，不要把整个玩法链写成一个长而脆弱的场景。

### TEST-04：Mixin 必须有独立运行时 smoke 层

- 优先级：P1

编译和 refmap存在不能证明运行时 target仍匹配。建议提供一个有超时的专用 dev client/server profile：

- 启动后检查日志中所有 required Mixin应用成功；
- JEI/Jade 缺失与存在两种矩阵；
- 至少触发 Fluid Tank renderer、belt funnel、Ponder、contraption damage和 legacy NBT read路径；
- 捕获日志并以已知 fatal pattern判定失败；
- 无论成功失败都终止子进程并清理临时 world。

项目约定要求 Mixin 改动在实践可行时运行 `quickPlayClient`；应把现有“人工启动器”升级成可观察、可退出的 smoke harness，而不是让 CI无限等待图形客户端。

### TEST-05：修复被注释掉的 Funnel Mixin 应有回归测试

- 优先级：P1/P2

`FunnelBlockMixin` 保留了一个未注入的 `createBiotech$updateShape` 方法及长 TODO：在已有 funnel 旁放 belt 会因重入/旧 surface 崩溃，所以自动 attach 被禁用。该方法仍参与编译但永远不会执行，容易让读者误判功能状态。

建议把问题变成可复现 GameTest：放置顺序、neighbor update、移除/re-attach、水平/垂直 surface。修复前将禁用代码移入 issue/design 文档或显式 `@Disabled` 测试说明，避免长期保留“看起来像有效 Mixin”的死 handler。

## 3. CI 与构建门禁

### CI-01：建立 Java 17 的基础 CI

- 优先级：P0/P1

最小 pipeline：

1. checkout；
2. 安装 Temurin/Corretto Java 17；
3. 校验 Gradle wrapper；
4. `gradlew --no-daemon compileJava test build --warning-mode all`；
5. 运行资源/语言/引用 lint；
6. 检查 refmap/JAR内容、许可证文件和生成资源；
7. 上传构建产物与问题报告。

建议 Windows 作为必跑环境，因为发布/quickplay脚本明显偏 Windows；再增加 Linux 编译可发现路径大小写、分隔符和 shell 假设。客户端 smoke可做手动/夜间 job，不阻塞每个普通 PR。

### CI-02：把“build 绿色但没有测试”变成显式失败

- 优先级：P1

在测试真正建立后，CI 应检查 test source/test result非空；否则一次误删 `src/test` 会继续绿色。资源 lint同样要作为 Gradle task接入 `check`，而不是只存在于审计人员的一次性命令。

推荐拆分任务：

- `lintResources`：JSON 重复键、模型/纹理、语言、advancement parent；
- `verifyMixinRefmap`：关键 Mixin/selector必须出现在 refmap；
- `verifyJarContents`：LICENSE、THIRD_PARTY_NOTICES、mixins/refmap、generated resources；
- `verifyGenerated`：Ponder/datagen manifest 与提交文件一致；
- `test`：纯 JVM；
- `gameTestServer`：world 行为。

### CI-03：启用有控制的编译 lint

- 优先级：P2

当前 javac只输出汇总：某些文件使用 deprecated API 和 unchecked 操作；Gradle problems report把位置统一归到 `CreateBiotechClient.java`，没有具体调用点。建议先在本地/CI添加：

```gradle
options.compilerArgs += ['-Xlint:deprecation', '-Xlint:unchecked']
```

先生成基线并逐项清理，再决定是否 `-Werror`。Mixin/Forge生成或第三方泛型接口可能产生合理警告，应在最小作用域使用 `@SuppressWarnings` 并说明原因，不要全项目屏蔽。

### CI-04：依赖来源可验证性不足

- 优先级：P2

仓库没有 dependency locking 或 `gradle/verification-metadata.xml`。虽然主要依赖版本固定，Maven 仓库内容被替换或镜像差异仍可能改变构建结果。建议启用 Gradle dependency verification，并对发布构建使用锁定/校验和；与 `01-build-dependencies-release.zh-CN.md` 的可复现 JAR 修复一起实施。

## 4. 日志与故障诊断

### DIAG-01：SavedData 损坏被静默丢弃

- 优先级：P1
- 文件：`AirCourierTaskSavedData.java`

加载每个 task时捕获任意 `Exception` 后空处理。一个格式变化或真实代码 bug会让 courier task消失，玩家的包裹/载具状态可能丢失，而日志没有 task index、UUID、NBT摘要或异常栈。

建议：

- 按 entry捕获预期解析异常；
- warning包含 data name、entry index、可读 task id和异常；
- 保留无法读取的原始 NBT到 quarantine list/备份字段，或至少统计丢弃数；
- 加旧版本 schema/version迁移测试。

`ShulkerTeleporterSavedData` 对非法 dimension id也静默忽略，至少应聚合成一次 warning。不要逐 tick刷屏，但数据丢弃必须可追踪。

### DIAG-02：兼容 fallback 捕获过宽且没有一次性诊断

- 优先级：P1/P2

代表性位置：

- `BioPackagerContraptionTracker#getAllItems()` 捕获 `Throwable` 后返回 null；
- `SlimeClutchBlockEntity` 反射解析/调用失败后静默退回 `RotationPropagator.isConnected`；
- `PonderSupportExt` entity NBT patch失败后忽略；
- Shulker client event和 Fluid Tank renderer也有宽 catch，后者已在 Mixin专项说明。

建议只捕获可预期的反射/链接/运行异常，并使用 once-per-session logger输出目标类、依赖版本和 fallback行为。`LinkageError` 是否降级需逐处决定，不能统一吞掉。主模组目前没有公共 `CreateBiotech.LOGGER`，而 Phantom、remap helper和渲染类各自创建 logger；可建立统一 logger/diagnostic helper实现 rate-limit与一次性告警。

### DIAG-03：`test.py` 完全丢弃客户端输出

- 优先级：P1

`stdout`/`stderr` 都指向 `DEVNULL`，脚本启动进程后立即成功返回，不等待退出、不监控 `latest.log`、无 timeout、无 PID cleanup，也无法区分“进入世界成功”和“启动即崩溃”。该问题在 `01-build-dependencies-release.zh-CN.md` 已记录。

建议每次运行创建带时间戳日志目录，保存 command、JAR hash、Java版本、stdout/stderr和 Minecraft latest.log；提供 `--timeout`、`--wait-for-log-pattern`、`--terminate-on-success` 与 finally cleanup。启动器和自动验证器应分成两个清晰命令，避免用户以为 `test.py` 已经测试过结果。

## 5. 文档与仓库可复现性

### DX-01：README 宣称 bundled 的 `ref/` 实际完全未跟踪

- 优先级：P0/P1

`.gitignore` 忽略整个 `ref/`，`git ls-files ref` 为 0，也没有 git submodule。新 clone不会得到当前用于 Create/JEI/Forge/Minecraft行为核验的任何参考源码；`ref/SOURCES.md` 也不存在。README 的“bundled Create + JEI reference sources”与事实冲突。

建议不要直接提交大体积上游 checkout，而是提交 `ref/SOURCES.md` 或机器可读 manifest，记录 repository URL、commit/tag、目标版本、license和获取脚本；可选用 submodule/worktree/bootstrap task。脚本应验证 checkout commit与 `gradle.properties` 版本，并明确 JEI 等近似参考 caveat。

### DX-02：关键项目规则被 `.gitignore` 排除

- 优先级：P1

`AGENTS.md` 和 `CLAUDE.md` 存在于本地但不在 Git 中，其中包含“先查 ref”“Mixin remap约定”“非 Mixin 不启动客户端”等重要安全规则。团队成员、CI和新的 AI task从 clone中无法获得这些约定。

若这些确实是项目级规则，应提交至少一个规范来源，例如 `CONTRIBUTING.md`/`docs/development/`，再让本地 agent文件引用它；个人专用增补可以继续 ignored。不要让关键映射规则只存在于单台机器。

### DX-03：README 多处与当前实现不一致

- 优先级：P1/P2

已确认：

- “Registration goes through Registrate”不符合 `CBItems`、`CBBlocks` 等主要 DeferredRegister实现；Registrate只在部分模块使用。
- `runData` 被描述为“regenerate datagen output”，但没有 provider，生成目录为空且未接入 source set。
- `data/` 被描述为“hand-written + datagen”，当前没有可重建 datagen链。
- 仓库布局列出 `tools/`，实际不存在。
- Mixin 表只链接 `create_biotech.mixins.json`，没有提 `create_biotech_phantom.mixins.json`。
- `ref/` 被描述为 bundled，实际 ignored。

建议 README 只描述可从 clean clone执行的流程；实验/本地私有环境放到单独文档并标注 prerequisites。增加 `CONTRIBUTING.md`、`DEVELOPMENT.md`、`RELEASING.md` 和 `CHANGELOG.md`，避免 README继续承载所有约定。

### DX-04：Ponder export manifest 与实际生成物已经漂移

- 优先级：P1

仓库确实跟踪 `.ponderer-export/manifest.json` 和 `last-report.json`，这是有价值的生成元数据；但当前 manifest并非可用的真实基线：

- manifest声明的 `ghast.nbt`、`rope.nbt` 两个唯一资源文件不存在（在两个 scene中共出现 4 次）；
- manifest收集到的 135 个 `en_us` key中，3 个在实际语言文件缺失，102 个值与实际文件不同；
- 最近生成的 Ghast scene Java实际引用 `ghast_hot_air_balloon_assembly_station_base.nbt` 和 `_rope.nbt`，与 manifest owned files不一致；
- `last-report` 只覆盖最近两个 scene，不能证明全部生成物一致。

这说明导出后发生了重命名/人工翻译，manifest没有同步。下一次 exporter依据旧 ownership清理或覆盖时存在丢文件/回退翻译风险。建议把 export命令固定为版本化脚本，导出后自动校验 manifest owned files、Java resource引用和语言值；人工翻译要回写场景源或进入明确的 post-export merge，而不是让 manifest长期陈旧。

### DX-05：缺少贡献、变更和安全入口

- 优先级：P2

仓库没有 CONTRIBUTING、CHANGELOG、SECURITY、release checklist或 issue/PR模板。对一个含网络包、Mixin、第三方资产和发布脚本的模组，这会增加版本回归与许可证遗漏概率。建议至少补：

- 支持的 Minecraft/Create/Forge矩阵；
- bug报告所需 latest.log、crash report、mod list；
- Mixin/网络/资源改动的验证要求；
- 版本号和 changelog规则；
- 发布前 JAR内容、许可证、hash、平台上传结果检查。

## 6. 推荐落地顺序

1. 建基础 CI，明确 `test NO-SOURCE` 不能代表验证完成。
2. 增加网络 decoder/authorization与纯函数 JUnit测试。
3. 建资源/refmap/JAR内容 lint并接入 `check`。
4. 为 SavedData、server lifecycle、Blast Chamber、belt建立 GameTest。
5. 把 quickplay升级为有日志、超时和退出判定的 smoke harness。
6. 修正文档、提交可共享的项目规则和 ref provenance manifest。
7. 同步/重建 Ponder export manifest，避免生成物 ownership继续漂移。

## 7. 验证清单

- [ ] clean clone在没有本地隐藏文件的情况下可以按文档完成 build/test。
- [ ] CI 使用 Java 17，并在 Windows 至少执行 compile/test/build/resource lint。
- [ ] `test` 有实际测试结果且关键网络边界有恶意输入用例。
- [ ] Forge GameTest覆盖 SavedData、结构、belt和 server stop lifecycle。
- [ ] Mixin smoke有 timeout、日志、成功条件和进程 cleanup。
- [ ] 任何 SavedData entry被丢弃都有可定位 warning。
- [ ] README 不再声称不存在的 bundled ref、tools或 datagen能力。
- [ ] `ref/SOURCES.md`/manifest记录全部本地参考的精确来源与版本 caveat。
- [ ] Ponder manifest owned files、Java引用、NBT和语言值一致。
