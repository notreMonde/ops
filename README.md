# 运维决策后端服务

这是一个基于 `Java 8 + Spring Boot 2.7 + Maven` 的运维决策后端示例项目，覆盖飞机维修和设备检修两类场景。

## 当前交互模式

项目中的飞机和设备 workflow 已统一为四阶段：

1. 澄清
2. 检索
3. 执行
4. 反馈

### 四阶段说明

- 澄清：只展示基础信息，并明确告知“接下来会深挖历史/变更/资源事实后再生成方案”，等待用户确认。
- 检索：自动深挖必要事实，不再向用户发起二次确认；返回“摘要 + 完整详情”，并直接展示。
- 执行：展示检索到的关键信息，并给出 `2-3` 个方案供用户选择。
- 反馈：根据用户选定的方案，返回最终执行回执。

## 项目结构

```text
.
├── pom.xml
├── mvnw
├── mvnw.cmd
├── codex-skill/
│   └── ops-maintenance-api/
└── src/
    ├── main/java/com/demo/ops/
    └── main/resources/datasets/
```

## 启动方式

### Windows

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
chmod +x mvnw
./mvnw clean package
./mvnw spring-boot:run
```

默认端口：`8080`

- 健康检查：[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Swagger UI：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON：[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## 核心接口

### 飞机

```powershell
curl "http://localhost:8080/api/v1/aircraft/B-1234/status-history"
curl "http://localhost:8080/api/v1/aircraft/B-1234/environment"
curl "http://localhost:8080/api/v1/aircraft/B-1234/mel-release?component=%E5%B7%A6%E4%B8%BB%E8%B5%B7%E8%90%BD%E6%9E%B6%E5%87%8F%E9%9C%87%E6%94%AF%E6%9F%B1&leakAreaCm2=8&continuousDrip=false"
```

### 设备

```powershell
curl "http://localhost:8080/api/v1/equipment/MOT-2024-A07/digital-twin"
curl "http://localhost:8080/api/v1/equipment/MOT-2024-A07/telemetry"
curl "http://localhost:8080/api/v1/equipment/MOT-2024-A07/historical-cases"
```

## 四阶段 workflow 示例

### 飞机 workflow

#### 1. 澄清

```powershell
curl -X POST "http://localhost:8080/api/v1/aircraft/B-1234/workflow/clarify" `
  -H "Content-Type: application/json" `
  -d "{}"
```

确认后：

```powershell
curl -X POST "http://localhost:8080/api/v1/aircraft/B-1234/workflow/clarify" `
  -H "Content-Type: application/json" `
  -d "{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true}"
```

#### 2. 检索

```powershell
curl -X POST "http://localhost:8080/api/v1/aircraft/B-1234/workflow/retrieval" `
  -H "Content-Type: application/json" `
  -d "{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true,\"continuousDrip\":false}"
```

检索阶段当前会同时返回：

- `retrievedInfo`：压缩版摘要，便于快速浏览
- `retrievedDetails`：完整详情，直接展开 `statusHistory`、`melAssessment`、`environment`、`troubleshootingKnowledge`、`knowledgeGraph`、`personnelMatch`、`inventory`、`diagnosisConclusion`

#### 3. 执行

```powershell
curl -X POST "http://localhost:8080/api/v1/aircraft/B-1234/workflow/execution" `
  -H "Content-Type: application/json" `
  -d "{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true,\"continuousDrip\":false,\"repairTarget\":\"保留放行\",\"userIntent\":\"尽快恢复运行\"}"
```

#### 4. 反馈

```powershell
curl -X POST "http://localhost:8080/api/v1/aircraft/B-1234/workflow/feedback" `
  -H "Content-Type: application/json" `
  -d "{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true,\"continuousDrip\":false,\"repairTarget\":\"保留放行\",\"userIntent\":\"尽快恢复运行\",\"selectedOptionCode\":\"B\"}"
```

### 设备 workflow

#### 1. 澄清

```powershell
curl -X POST "http://localhost:8080/api/v1/equipment/MOT-2024-A07/workflow/clarify" `
  -H "Content-Type: application/json" `
  -d "{}"
```

确认后：

```powershell
curl -X POST "http://localhost:8080/api/v1/equipment/MOT-2024-A07/workflow/clarify" `
  -H "Content-Type: application/json" `
  -d "{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true}"
```

#### 2. 检索

```powershell
curl -X POST "http://localhost:8080/api/v1/equipment/MOT-2024-A07/workflow/retrieval" `
  -H "Content-Type: application/json" `
  -d "{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true}"
```

检索阶段当前会同时返回：

- `retrievedInfo`：压缩版摘要，便于快速浏览
- `retrievedDetails`：完整详情，直接展开 `digitalTwin`、`telemetry`、`historicalCases`、`changeRelations`、`personnelMatch`、`inventory`、`diagnosisConclusion`

#### 3. 执行

```powershell
curl -X POST "http://localhost:8080/api/v1/equipment/MOT-2024-A07/workflow/execution" `
  -H "Content-Type: application/json" `
  -d "{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true,\"userIntent\":\"尽量不停机\"}"
```

#### 4. 反馈

```powershell
curl -X POST "http://localhost:8080/api/v1/equipment/MOT-2024-A07/workflow/feedback" `
  -H "Content-Type: application/json" `
  -d "{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true,\"selectedOptionCode\":\"A\"}"
```

## Skill 文件

- [SKILL.md](/E:/Users/13352/Desktop/运维系统/files-mentioned-by-the-user-md-3/codex-skill/ops-maintenance-api/SKILL.md)
- [openapi.yaml](/E:/Users/13352/Desktop/运维系统/files-mentioned-by-the-user-md-3/codex-skill/ops-maintenance-api/openapi.yaml)
- [system-prompt.md](/E:/Users/13352/Desktop/运维系统/files-mentioned-by-the-user-md-3/codex-skill/ops-maintenance-api/system-prompt.md)
