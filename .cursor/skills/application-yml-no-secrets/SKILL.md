---
name: application-yml-no-secrets
description: >-
  Keeps Spring `application.yml` free of real database hosts, passwords, and
  private LLM endpoints. Use when editing `src/main/resources/application.yml`,
  adding datasource or LangChain config, or before committing configuration
  changes.
---

# application.yml：禁止入库的敏感信息

## 规则

1. **`src/main/resources/application.yml`** 中不得出现真实 **PostgreSQL 主机（尤其公网 IP）、用户名、密码**，也不得把 **内网/Tailscale 可达的 LLM 地址** 作为唯一写死的默认值。
2. **凭证与真实连接串** 只放在以下之一：
   - **`src/main/resources/application-local.yml`**（已由 `.gitignore` 忽略，可复制 `application-local.yml.example`）
   - **环境变量**：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`，以及 `LLM_BASE_URL`、`LLAMA_EMBEDDING_BASE_URL` 等（与 `application.yml` 中 `${VAR:default}` 一致）
3. 修改 `application.yml` 后若涉及默认连接/模型地址，同步更新 **`application-local.yml.example`** 的说明，便于协作者本地配置。

## 仓库内允许的默认值

- 数据源：`jdbc:postgresql://127.0.0.1:5432/vectordb` 与非敏感占位用户名；密码通过变量注入，默认可为空并由本地文件覆盖。
- LLM / 嵌入：`http://127.0.0.1:8080/v1` 与 `http://127.0.0.1:8081/v1` 作为本地 llama-server 类默认。

## 提交前自检

- `git diff src/main/resources/application.yml` 中不出现疑似真实密码、公网 DB IP、或团队私有网络地址。
- 若误提交过密钥，除改文件外应轮换数据库密码并视情况清理 Git 历史。
