---
name: ops-maintenance-api
description: 当用户需要查询飞机维修或设备检修相关事实、诊断和处置方案时使用。
---

# 运维决策 API Skill

这个 Skill 用于引导 Codex 调用本项目的运维决策后端接口，覆盖飞机维修和设备检修两类场景。

## 默认 workflow

默认使用四阶段交互流程：

1. 澄清
2. 检索
3. 执行
4. 反馈

### 各阶段要求

- 澄清：
  - 展示 `basicInfo`
  - 展示固定文案：`接下来会深挖历史/变更/资源事实后再生成方案`
  - 等待用户确认 `basicInfoConfirmed` 和 `followUpAcknowledged`
- 检索：
  - 不再等待确认
  - 自动深挖必要事实
  - 直接返回“摘要 + 完整详情”
  - `retrievedInfo` 作为压缩总览
  - `retrievedDetails` 作为完整事实结果，不收起
- 执行：
  - 展示检索阶段的压缩关键信息
  - 给出 `2-3` 个方案
  - 标出推荐项
- 反馈：
  - 基于用户选择返回最终执行回执

## 使用时机

- 用户提到飞机故障、MEL 放行、渗漏、机库、航材、放行判断
- 用户提到设备过热、振动异常、历史案例、变更影响、停机检修、备件和资源准备
- 用户希望按照“先确认基础信息，再深挖事实，再出方案”的交互方式推进

## 调用顺序

### 飞机

1. `POST /api/v1/aircraft/{tailNo}/workflow/clarify`
2. `POST /api/v1/aircraft/{tailNo}/workflow/retrieval`
3. `POST /api/v1/aircraft/{tailNo}/workflow/execution`
4. `POST /api/v1/aircraft/{tailNo}/workflow/feedback`

如需补充单点事实，可再调用：

- `GET /api/v1/aircraft/{tailNo}/status-history`
- `GET /api/v1/aircraft/{tailNo}/environment`
- `GET /api/v1/aircraft/{tailNo}/mel-release`
- `GET /api/v1/aircraft/{tailNo}/troubleshooting-kb`
- `GET /api/v1/aircraft/{tailNo}/knowledge-graph`
- `GET /api/v1/aircraft/{tailNo}/personnel-match`
- `GET /api/v1/aircraft/{tailNo}/inventory`

### 设备

1. `POST /api/v1/equipment/{equipmentId}/workflow/clarify`
2. `POST /api/v1/equipment/{equipmentId}/workflow/retrieval`
3. `POST /api/v1/equipment/{equipmentId}/workflow/execution`
4. `POST /api/v1/equipment/{equipmentId}/workflow/feedback`

如需补充单点事实，可再调用：

- `GET /api/v1/equipment/{equipmentId}/digital-twin`
- `GET /api/v1/equipment/{equipmentId}/telemetry`
- `GET /api/v1/equipment/{equipmentId}/historical-cases`
- `GET /api/v1/equipment/{equipmentId}/change-relations`
- `GET /api/v1/equipment/{equipmentId}/personnel-match`
- `GET /api/v1/equipment/{equipmentId}/inventory`

## 输出约束

- 不编造 MEL、人员、航材、备件、工具、环境或诊断结论
- 澄清阶段不要给候选动作
- 检索阶段不要再次要求确认
- 检索阶段完整详情直接展示，不要只返回引用或“可展开”提示
- 执行阶段不要跳过方案选择直接输出最终回执
- 反馈阶段必须基于已选方案输出
