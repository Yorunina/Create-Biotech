# 01. 构建、依赖、映射与发布审计

## 结论摘要

项目当前可以成功编译并产出可重映射 JAR，Mixin annotation processor 和 refmap 打包链路正常。主要风险集中在发布正确性与可追溯性：二进制产物缺少必须保留的许可证材料，发布脚本会把上传失败报告成成功，命令行版本类型解析存在静默误发布风险，JAR 无法复现，依赖版本范围又明显宽于实际兼容窗口。

建议先修发布与许可问题，再补生成资源、版本约束、工具链和 CI。不要在这些问题解决前把“本地 `build` 成功”等同于“可安全发布”。

## 1. 实际构建结果

在基准提交 `511533e2` 上执行了以下不启动客户端的验证：

| 命令 | 结果 | 观察 |
| --- | --- | --- |
| `gradlew.bat compileJava --rerun-tasks --warning-mode all` | 成功，约 17 秒 | annotation processor 写出 `build/mixin/create_biotech.refmap.json` |
| `gradlew.bat build --rerun-tasks --warning-mode all` | 成功，约 22 秒 | `test NO-SOURCE`，`jarJar NO-SOURCE` |
| 再次执行 `gradlew.bat jar` | 成功，约 10 秒 | 即使类和资源均为 up-to-date，`jar` 与 `reobfJar` 仍重新执行 |

编译器报告两类待清理警告：使用或覆盖已弃用 API、未经检查或不安全的泛型操作。当前构建没有启用足以定位到具体文件的 `-Xlint:deprecation` 和 `-Xlint:unchecked`。

refmap 约 20 KiB，最终 JAR 根目录包含：

- `create_biotech.refmap.json`
- `create_biotech.mixins.json`
- `create_biotech_allay.mixins.json`

因此当前 annotation processor、重映射和 refmap 装包链路是有效的。后续调整 Mixin 时应保留这一链路，并继续以生成 refmap 为验证证据。

## 2. 发现与建议

### BUILD-01：发布 JAR 缺少许可证与第三方声明

- 优先级：P0
- 类别：发布合规

证据：

- `LICENSE.md` 说明原创代码为 MIT、原创资源通常为 All Rights Reserved，整体并非单一 MIT 许可证。
- `THIRD_PARTY_NOTICES.md` 要求保留 createbuttercat 的 MIT 声明、Create Phantom 的 BSD-3-Clause 声明以及 Create Mobile Packages 的 MIT 声明。
- `mods.toml` 当前把模组整体声明为 `MIT`。
- 已构建的 `create_biotech-1.20.1-1.1.0.jar` 中没有任何 `LICENSE`、`NOTICE` 或 `THIRD_PARTY_NOTICES` 文件。
- BSD-3-Clause 明确要求二进制再分发在文档或其他随附材料中重现版权声明、条件和免责声明。

建议：

1. 在 `jar` 与 `sourcesJar` 中显式加入 `LICENSE.md` 和 `THIRD_PARTY_NOTICES.md`，使用稳定路径，例如 `META-INF/LICENSE_CREATE_BIOTECH.md` 与 `META-INF/THIRD_PARTY_NOTICES.md`。
2. 重新定义 `mods.toml` 的 `license` 文本，使其准确表达“MIT 代码 + ARR 原创资产 + 第三方许可”，或使用 `Custom` 并指向随包许可文件；不要继续把整个二进制描述为纯 MIT。
3. 在 Modrinth、CurseForge 和 README 的发布说明中保持同一许可证口径。
4. 修正 `THIRD_PARTY_NOTICES.md` 中已失效的本地参考路径，改为 Create Phantom 的线上代码仓库 `https://github.com/yision1/CreatePhantom`，并明确本项目仅参考其代码。

验证：构建后列出 JAR 内容，确认两个许可文件存在；再人工比对平台页面、`mods.toml` 和仓库文档的许可证描述。

### BUILD-02：上传失败仍会被报告为发布成功

- 优先级：P0
- 类别：发布正确性

证据：`upload_modrinth()` 和 `upload_curseforge()` 会返回布尔值，但 `main()` 忽略返回值；无论一个还是两个平台失败，脚本最后都会打印 `发布完成!` 并以成功状态退出。

影响：自动化或操作者可能认为两个平台均已发布，实际只上传了一个平台或完全没有上传；后续重复发布又可能遇到版本号冲突。

建议：收集每个平台/加载器的上传结果；任一请求失败时以非零状态退出，并打印成功、失败和未尝试项目的汇总。多平台发布不可能真正事务化，因此应明确支持“失败后安全重试”，并在上传前查询同版本是否已经存在。

验证：用 mock HTTP 客户端分别模拟 Modrinth 500、CurseForge 500 和单边成功，断言进程退出码与汇总状态正确。

### BUILD-03：`--version-type` 的文档写法会静默选择错误类型

- 优先级：P0
- 类别：发布正确性

证据：脚本头部写法是 `--version-type release|beta|alpha`，但解析逻辑只处理带等号的 `--version-type=beta`。如果使用文档中的 `--version-type beta`：

1. `--version-type` 参数本身不含 `=`，所以变量仍为默认 `release`。
2. 后续代码发现命令行中出现过 `--version-type`，不会再弹出交互选择。
3. 最终可能把 beta/alpha 静默发布成 release。

建议：使用 `argparse` 定义 `choices=(release, beta, alpha)`，同时支持标准空格写法；非法值必须立即退出。也应让 `--only-modrinth` 与 `--only-curseforge` 互斥，避免两者同时出现后实际不上传任何平台。

验证：为所有参数组合添加纯 Python 单元测试，重点覆盖文档示例、非法类型和互斥选项。

### BUILD-04：发布 JAR 不可复现

- 优先级：P1
- 类别：供应链与调试

证据：`build.gradle` 每次配置时把当前时间写入 `Implementation-Timestamp`。在源码未变化的情况下，两次产物 SHA-256 分别为：

- `A7CADB5E7C1B832726E689CF5AE45A6968DC1B1097A6B803E4DFA02B9D596287`
- `73C473074E94FEA945DBA5AEB689B4025CB7E256B49BC02437C8B3A6589BFA14`

第二次构建中类与资源均为 up-to-date，但 `jar` 和 `reobfJar` 仍重新执行。时间戳既改变内容哈希，也破坏 Gradle 的任务缓存价值。

建议：

- 删除构建实时时间戳，或改用可复现来源，例如 `SOURCE_DATE_EPOCH`、提交时间或发布时显式注入的固定值。
- 对所有归档任务设置 `preserveFileTimestamps = false` 和 `reproducibleFileOrder = true`。
- 在发布前输出 SHA-256，并把校验值保存到发布记录或 CI artifact attestation 中。

验证：同一提交连续执行两次干净构建，JAR SHA-256 必须一致。

### BUILD-05：数据生成输出未加入主资源 source set

- 优先级：P1
- 类别：构建完整性

证据：`runData` 把结果写到 `src/generated/resources/`，但项目没有 `sourceSets.main.resources.srcDir(...)` 配置。当前该目录除 `.cache` 外为空，因此问题暂未表现为漏包。精确匹配的 `ref/Create/build.gradle` 在 `sourceSets.main.resources` 中明确加入 `src/generated/resources` 并排除 `.cache/`。

影响：未来运行数据生成后，文件会出现在仓库中但不会自然进入 `processResources` 和发布 JAR，容易产生“开发目录里存在、游戏里缺失”的隐蔽错误。

建议：按 `ref/Create/build.gradle` 的模式加入生成资源目录并排除 `.cache/`；同时决定生成产物是提交到 Git 还是只由 CI 生成，不能保持模糊状态。

验证：放置或生成一个仅存在于 `src/generated/resources` 的测试资源，执行 `processResources` 和 `jar` 后确认其进入输出；再删除测试资源。

### BUILD-06：依赖版本范围宽于实际兼容窗口

- 优先级：P1
- 类别：运行时兼容

当前元数据范围：

| 依赖 | 当前范围 | 风险 |
| --- | --- | --- |
| Forge loader | `[47,)` | 允许未来加载器大版本 |
| Forge | `[47.1.33,)` | 没有 48 上界 |
| Minecraft | `[1.20.1,1.21)` | 声称支持 1.20.2–1.20.6，但项目只按 1.20.1 编译和映射 |
| Create | `[6.0.0,)` | 允许未来 6.x/7.x，而项目有大量内部类 Mixin |
| JEI | `[15.20.0,)` | 客户端 Mixin 直接指向 JEI library 内部类，无主版本上界 |
| Jade | `[11.0,)` | 无主版本上界 |

项目有 58 个 Mixin，并且部分直接修改 Create/JEI 内部实现，实际兼容面显然比这些开放范围窄。

建议以已验证版本线为基础增加上界，例如 Minecraft `[1.20.1,1.20.2)`、Forge `[47.1.33,48)`、Create 至少限定在经验证的 6.0.x 范围、JEI 限定 15.x、Jade 限定 11.x。最终范围应在 Mixin 专项审计和真实兼容测试后确定，而不是机械照抄示例。

验证：解析构建后的 `mods.toml`，为每个范围编写边界测试；分别验证最低支持版本、当前版本和拒绝的下一主版本。

### BUILD-07：最终 JAR 对 Create 的内嵌依赖形成隐式契约

- 优先级：P1
- 类别：依赖打包

完整构建显示 `jarJar NO-SOURCE`，最终 JAR 中嵌套 JAR 数量为 0。项目开发运行时直接声明 Ponder 和 MixinExtras，但发布产物并不内嵌它们。

精确匹配的 `ref/Create/build.gradle` 会将 Registrate、Flywheel、Ponder 和 MixinExtras 通过 `jarJar` 放入 Create 发布产物，因此在“强制安装官方 Create 6.0.8”这一前提下，当前模组可以依赖 Create 提供这些库。这本身可以是合理设计，但目前没有被显式记录，且 `mods.toml` 又允许任意未来 Create 版本。

建议：明确选择并记录一种策略：

- 推荐的当前策略：依赖 Create 提供其内嵌运行库，同时收紧 Create 版本范围，并在发布验证中检查目标 Create JAR 的运行契约。
- 若希望脱离 Create 的打包细节：自行 `jarJar` 必需库，但必须评估与 Create 内嵌副本的去重和版本冲突，不能直接照搬造成双份加载。

### BUILD-08：发布前置校验不足

- 优先级：P1
- 类别：发布流程

`publish.py` 当前不检查：

- Git 工作区是否干净。
- 当前提交是否已推送、是否有对应 tag。
- `gradle.properties`、JAR 文件名、JAR 内 `mods.toml` 和 manifest 版本是否一致。
- JAR 是否包含 refmap、Mixin 配置、许可文件和预期资源。
- `--skip-build` 选择的产物是否确实来自当前提交。
- changelog 是否为空。

同时，`build/libs` 长期保留了多个历史版本产物，增加脚本模糊匹配或人工误选的风险。

建议：增加独立的 `releaseCheck`/preflight 阶段，默认执行干净构建，并把提交哈希写入构建元数据；`--skip-build` 必须验证产物旁的 commit/hash 证明。发布脚本应使用精确文件名，不保留模糊回退作为正常路径。

### BUILD-09：HTTP 与 Python 环境处理不稳定

- 优先级：P1
- 类别：发布可靠性

问题包括：

- `requests.get/post` 没有连接和读取超时，网络异常时可无限等待。
- 上传时直接 `open(jar_path, "rb")`，没有上下文管理器保证文件关闭。
- 缺少 `requests` 时脚本会在发布现场自动执行 `pip install requests`，改变环境且没有锁定版本或哈希。
- 密钥只从项目根目录 `.env` 读取，不优先支持标准环境变量，不利于 CI 和凭据隔离。

建议：使用锁定的开发/发布依赖文件或独立工具环境；所有请求设置显式 timeout，并捕获网络异常；文件使用 `with`；凭据优先从环境变量或安全凭据存储读取，`.env` 只作为本地可选回退。

### BUILD-10：Java 工具链声明没有真正约束编译与快速启动

- 优先级：P1/P2
- 类别：环境一致性

`settings.gradle` 启用了 Foojay toolchain resolver，但 `build.gradle` 只设置 `sourceCompatibility`、`targetCompatibility` 和 `options.release = 17`，没有配置 `java.toolchain.languageVersion = 17`。本次环境的 `JAVA_HOME` 实际为 Java 21；编译能够生成 Java 17 字节码，但编译器实现和本地快速启动 JVM 并未固定为 17。

`test.py` 会直接优先使用 `JAVA_HOME/bin/java.exe`，因此当前快速启动也会使用 Java 21，而不是项目声明的 Java 17。

建议：

- 在 Gradle 中真正声明 Java 17 toolchain。
- `test.py` 支持 `--java` 或读取 Gradle toolchain 路径，并在启动前检查主版本。
- 若明确支持 Java 21 运行，也应把它变成经过验证的支持矩阵，而不是环境偶然结果。

### BUILD-11：快速客户端脚本缺少可控退出和诊断

- 优先级：P1
- 类别：运行验证

项目规则要求长期客户端验证具备 timeout、退出路径和清理行为。当前 `test.py` 启动后立即脱离：标准输出和错误输出都丢弃，脚本不等待、不设超时、不记录 PID 文件、不负责结束进程，也不检查客户端是否成功进入世界。

另外，`copy_mod_jar()` 以修改时间选择 `build/libs` 中最新的任意非 sources JAR，而不是根据当前 `mod_version` 选择精确文件；在跳过构建、版本回退或历史产物残留时可能复制错误版本。

建议：增加 `--wait`、`--timeout`、`--log-file`、`--kill-on-timeout` 和稳定的 PID/退出码处理；按 `mod_id + minecraft_version + mod_version` 精确选择 JAR；至少扫描启动日志中的 Mixin apply error、mod loading failure 和成功进入目标世界标志。

### BUILD-12：Gradle 性能与供应链治理仍有空间

- 优先级：P2
- 类别：开发体验与供应链

观察：

- `org.gradle.daemon=false` 使每个命令都创建并停止单次 daemon；本次即使只做 dependency insight 也约需 7–8 秒。
- 未启用 Gradle build cache、依赖锁定或 dependency verification metadata。
- Maven 仓库没有 content filter，所有坐标可能依次探测多个第三方仓库。
- Gradle wrapper 使用 8.12 且启用了 URL 校验，但 CI 尚未验证 wrapper checksum。

建议：本地默认允许 daemon，并在 CI 显式使用 `--no-daemon`；评估 `org.gradle.caching=true`；为专用仓库添加 group content filter；生成 dependency verification metadata，并评估锁定直接依赖。Forge/ModDev 生态对 configuration cache 支持有限，不应未经验证直接开启。

### BUILD-13：构建配置和文档存在失效项

- 优先级：P2/P3
- 类别：维护性

- 应用 `maven-publish` 插件但没有 `publishing` 配置；若不发布 Maven，应删除无效插件，若需要则补齐 publication 和仓库。
- README 的仓库结构列出不存在的 `tools/` 目录。
- 发布脚本会读取 `CHANGELOG.md`，但仓库当前没有该文件，只能回退到 Git 提交文本。
- `mods.toml` 缺少项目主页、问题跟踪地址等可选元数据，用户排错路径不够直接。

## 3. 推荐实施批次

### 第一批：阻止错误或不合规发布

1. BUILD-01：随 JAR 打包许可与第三方声明，修正 license 元数据。
2. BUILD-02/03：重写发布参数解析与失败退出逻辑。
3. BUILD-04：移除非确定性时间戳并验证可复现构建。
4. BUILD-08/09：增加 release preflight、超时、异常处理和精确产物选择。

### 第二批：收紧运行契约

1. BUILD-05：接入生成资源 source set。
2. BUILD-06/07：根据 Mixin 审计结果收紧依赖范围并记录 Create 内嵌依赖契约。
3. BUILD-10/11：固定 Java 17 工具链，完善快速客户端的超时、日志和清理。

### 第三批：提高日常开发效率

1. BUILD-12：daemon、build cache、仓库过滤、依赖验证。
2. BUILD-13：清理无效插件和失效文档，补 changelog/发布元数据。
3. 在 CI 中固定运行 `compileJava`、`processResources`、`build`、产物结构检查和发布脚本单元测试。

## 4. 本阶段未执行的验证

根据仓库规则，本阶段只修改文档，没有修改 Mixin，也没有得到用户要求启动客户端，因此未运行 `quickPlayClient`、`test.py` 或其他客户端启动验证。Mixin 运行时兼容性将在专项章节中基于源码、refmap 和本地参考继续审计。
