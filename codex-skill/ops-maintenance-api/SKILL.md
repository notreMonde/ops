---
name: ops-maintenance-api
description: 当用户需要查询飞机维修或设备检修相关事实、诊断和处置方案时使用。
---

# 运维决策 API Skill

这个 Skill 用于引导 Codex 调用本项目的运维决策后端接口，覆盖飞机维修和设备检修两类场景。

默认后端地址：`http://localhost:8080`

## 强制 workflow

无论使用哪个模型，默认都必须按以下四阶段顺序推进，禁止跳步：

1. 澄清
2. 检索
3. 执行
4. 反馈

默认交互节奏如下：

- 澄清阶段单独输出，并等待用户确认后再进入下一阶段。
- 检索与执行阶段允许在同一条回复内连续完成：先展示检索结果，再立刻给出执行方案。
- 反馈阶段必须等待用户明确选择方案后再单独输出。

## 阶段门禁

### 1. 澄清

- 必须先进入澄清阶段。
- 只展示 `basicInfo` 与固定说明：`接下来会深挖历史/变更/资源事实后再生成方案`
- 必须等待用户确认以下两个标志后，才能进入检索阶段：
  - `basicInfoConfirmed=true`
  - `followUpAcknowledged=true`
- 澄清阶段禁止：
  - 提前输出检索结果
  - 提前给出候选方案
  - 提前给出最终回执

### 2. 检索

- 只有在澄清确认完成后才能进入检索阶段。
- 检索阶段不再要求用户再次确认。
- 必须直接展示“摘要 + 完整详情”：
  - `retrievedInfo`：压缩总览
  - `retrievedDetails`：完整检索细节，直接展开，不收起
- 检索结果展示完成后，应在同一条回复内直接衔接执行阶段方案，不需要单独停住等待用户再次确认。
- 检索阶段禁止：
  - 只返回引用、链接或“可展开”提示代替完整详情
  - 直接输出最终回执

### 3. 执行

- 只有在检索阶段已经完成并展示后，才能进入执行阶段。
- 执行阶段通常紧跟在检索阶段后，在同一条回复中输出。
- 必须基于检索结果给出 `2-3` 个候选方案。
- 每个方案至少包含：
  - `optionCode`
  - `title`
  - `applicableScenario`
  - `recommended`
  - `feasible`
  - `advantages`
  - `risks`
  - `prerequisites`
- 执行阶段输出方案后必须停住，等待用户确认或选择方案。
- 执行阶段禁止：
  - 跳过方案选择直接输出最终回执
  - 把反馈阶段内容提前到执行阶段

### 4. 反馈

- 只有在用户已经明确选择 `selectedOptionCode` 后，才能进入反馈阶段。
- 必须基于已选方案返回最终执行回执。
- 至少返回：
  - `executionStatus`
  - `selectedOptionCode`
  - `finalPlan`
  - `nextAction`
- 反馈阶段禁止：
  - 在没有用户选择方案时自行代选并结束流程

## 使用时机

- 用户提到飞机故障、MEL 放行、渗漏、机库、航材、放行判断
- 用户提到设备过热、振动异常、历史案例、变更影响、停机检修、备件和资源准备
- 用户明确希望通过多阶段交互推进，而不是一次性得到最终答案

## 调用顺序

### 飞机

1. `POST /api/v1/aircraft/{tailNo}/workflow/clarify`
2. `POST /api/v1/aircraft/{tailNo}/workflow/retrieval`
3. `POST /api/v1/aircraft/{tailNo}/workflow/execution`
4. `POST /api/v1/aircraft/{tailNo}/workflow/feedback`

如需补充单点事实，只能作为检索阶段的补充调用，不能绕过四阶段主流程：

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

如需补充单点事实，只能作为检索阶段的补充调用，不能绕过四阶段主流程：

- `GET /api/v1/equipment/{equipmentId}/digital-twin`
- `GET /api/v1/equipment/{equipmentId}/telemetry`
- `GET /api/v1/equipment/{equipmentId}/historical-cases`
- `GET /api/v1/equipment/{equipmentId}/change-relations`
- `GET /api/v1/equipment/{equipmentId}/personnel-match`
- `GET /api/v1/equipment/{equipmentId}/inventory`

## 输出约束

- 不编造 MEL、人员、航材、备件、工具、环境或诊断结论
- 不因为用户催促就跳过澄清确认或方案确认
- 检索和执行可以合并成一次回答，但反馈不得与其合并
- 不在执行阶段替用户自动选方案
- 不在反馈阶段补做前面漏掉的澄清或检索
