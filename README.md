# 运维决策后端服务

这是一个基于 `Java 8 + Spring Boot 2.7 + Maven` 的运维决策后端服务，覆盖飞机维修与设备维修两类业务流程。

## 能力范围

- 飞机维修：飞机状态与履历、MEL 放行判断、环境感知、AMM/知识库、知识图谱、人员画像、航材工具库存、诊断结论、处置决策
- 设备维修：数字孪生、实时数据、历史案例、变更关联、人员画像、备件工具库存、诊断结论、处置决策

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

## 依赖与启动

优先使用项目内的 `mvnw.cmd`。这个脚本会按下面顺序查找 Maven：

1. 环境变量 `OPS_MAVEN_CMD`
2. `D:\maven\apache-maven-3.9.9\bin\mvn.cmd`
3. `MAVEN_HOME\bin\mvn.cmd`
4. `PATH` 中的 `mvn.cmd` 或 `mvn`

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

## 示例接口

### 飞机维修

```powershell
curl "http://localhost:8080/api/v1/aircraft/B-1234/status-history"
curl "http://localhost:8080/api/v1/aircraft/B-1234/mel-release?component=%E5%B7%A6%E4%B8%BB%E8%B5%B7%E8%90%BD%E6%9E%B6%E5%87%8F%E9%9C%87%E6%94%AF%E6%9F%B1&leakAreaCm2=15&continuousDrip=false"
curl "http://localhost:8080/api/v1/aircraft/B-1234/environment"
curl -X POST "http://localhost:8080/api/v1/aircraft/B-1234/diagnosis-conclusion" -H "Content-Type: application/json" -d "{\"leakAreaCm2\":15,\"continuousDrip\":false,\"repairTarget\":\"完全修复\"}"
curl -X POST "http://localhost:8080/api/v1/aircraft/B-1234/disposition-decision" -H "Content-Type: application/json" -d "{\"leakAreaCm2\":15,\"continuousDrip\":false,\"repairTarget\":\"完全修复\"}"
```

### 设备维修

```powershell
curl "http://localhost:8080/api/v1/equipment/MOT-2024-A07/digital-twin"
curl "http://localhost:8080/api/v1/equipment/MOT-2024-A07/telemetry"
curl "http://localhost:8080/api/v1/equipment/MOT-2024-A07/historical-cases"
curl -X POST "http://localhost:8080/api/v1/equipment/MOT-2024-A07/diagnosis-conclusion" -H "Content-Type: application/json" -d "{\"currentTemperatureC\":92,\"vibrationMmPerS\":4.2,\"temperatureRiseRatePerMin\":2.3}"
curl -X POST "http://localhost:8080/api/v1/equipment/MOT-2024-A07/disposition-decision" -H "Content-Type: application/json" -d "{\"immediateShutdown\":true,\"reserveBearing\":true,\"pushWorkOrder\":true}"
```

## Skill 文件

- [codex-skill/ops-maintenance-api/SKILL.md](/C:/Users/13352/Documents/Codex/2026-06-01/files-mentioned-by-the-user-md-3/codex-skill/ops-maintenance-api/SKILL.md)
- [codex-skill/ops-maintenance-api/openapi.yaml](/C:/Users/13352/Documents/Codex/2026-06-01/files-mentioned-by-the-user-md-3/codex-skill/ops-maintenance-api/openapi.yaml)
- [codex-skill/ops-maintenance-api/system-prompt.md](/C:/Users/13352/Documents/Codex/2026-06-01/files-mentioned-by-the-user-md-3/codex-skill/ops-maintenance-api/system-prompt.md)
