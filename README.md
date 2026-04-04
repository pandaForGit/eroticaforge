# EroticaForge

本地 NSFW 角色扮演小说生成工具 — Java 后端（Spring Boot 3 + LangChain4j）。

设计文档见 [`docs/`](docs/)。

## 环境要求

- JDK 21、Maven 3.9+
- PostgreSQL（含 [pgvector](https://github.com/pgvector/pgvector)）
- 本机：[llama.cpp](https://github.com/ggerganov/llama.cpp) `llama-server`（OpenAI 兼容 API）
- 本机：[Ollama](https://ollama.com)（嵌入模型 `bge-m3`）

## 数据库初始化

在云库 `vectordb` 执行：

```bash
psql "postgresql://USER:HOST:5432/vectordb" -f sql/001_init_pgvector.sql
```

若 `vector` 扩展安装失败，需在服务器上先安装 pgvector 包（随发行版而异）。

## 运行（勿把密码写进 Git）

在 PowerShell 中设置环境变量后启动：

```powershell
$env:DB_URL="jdbc:postgresql://HOST:5432/vectordb"
$env:DB_USERNAME="你的用户"
$env:DB_PASSWORD="你的密码"
$env:LLM_BASE_URL="http://localhost:8080/v1"
mvn spring-boot:run
```

默认 **应用端口 `8090`**，避免与本机 **llama-server 占用 8080** 冲突。可用 `SERVER_PORT` 覆盖。

健康检查：<http://localhost:8090/api/health>

## 阶段 1（已完成于代码库）

- `com.eroticaforge.config.LangChainConfig`：`ChatLanguageModel`、`StreamingChatLanguageModel`（llama-server）、`EmbeddingModel`（Ollama bge-m3）、`EmbeddingStore`（PgVector，表名默认 `erotica_lc4j_embeddings`）。
- 可选本地覆盖：在 `src/main/resources/` 放置 `application-local.yml`（已被 `.gitignore` 忽略），或通过 `spring.config.import` 已支持 **optional** 加载。
- 本机编译：`mvn -q compile`（需 **JDK 21**，非 JRE）。
- 可选联调测试（需 DB + Ollama + llama-server）：

```powershell
$env:EROTICA_RUN_LLM_SMOKE="true"
mvn -Dtest=LangChainSmokeIT test
```

## 与 GitHub 同步

远程仓库：<https://github.com/pandaForGit/eroticaforge>  

首次推送前请确认 **未** 将 `application-local.yml`、`.env` 或真实口令提交进仓库。  
**后续推送请在你明确要求后再执行**。
