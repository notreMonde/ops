---
name: ops-maintenance-api
description: Use when the user needs to query the maintenance decision backend for aircraft maintenance or equipment maintenance facts, diagnoses, and disposition decisions.
---

# Ops Maintenance API

这个 Skill 用于引导 Codex 调用运维决策后端接口，完成飞机维修和设备维修场景下的事实查询、诊断归纳和处置决策生成。

默认采用三阶段互动流程：

1. 检索：列出设备/飞机基本信息、用户意图、已知限制和待补充项
2. 执行：给出 2-3 个可选方案，并标出推荐项
3. 反馈：在用户选定方案后输出最终执行方案

## 何时使用

- 用户提到飞机故障、MEL 放行、AMM 步骤、航材、工具、机库、放行判断
- 用户提到设备故障、数字孪生、实时温度/振动、历史案例、备件、停机决策
- 用户希望通过交互式流程拿到“检索结果 + 可选方案 + 最终执行方案”

## 工具契约

- OpenAPI 规范：`openapi.yaml`
- 系统提示词：`system-prompt.md`
- 服务默认地址：`http://localhost:8080`

## 调用策略

### 飞机维修

1. 先锁定飞机号和故障部位
2. 检索阶段优先查询事实接口：
   - `GET /api/v1/aircraft/{tailNo}/status-history`
   - `GET /api/v1/aircraft/{tailNo}/mel-release`
   - `GET /api/v1/aircraft/{tailNo}/environment`
   - `GET /api/v1/aircraft/{tailNo}/troubleshooting-kb`
   - `GET /api/v1/aircraft/{tailNo}/knowledge-graph`
   - `GET /api/v1/aircraft/{tailNo}/personnel-match`
   - `GET /api/v1/aircraft/{tailNo}/inventory`
3. 执行阶段优先调用：
   - `POST /api/v1/aircraft/{tailNo}/workflow/execution`
4. 反馈阶段优先调用：
   - `POST /api/v1/aircraft/{tailNo}/workflow/feedback`
5. 需要单次综合结论时再调用：
   - `POST /api/v1/aircraft/{tailNo}/diagnosis-conclusion`
   - `POST /api/v1/aircraft/{tailNo}/disposition-decision`

### 设备维修

1. 先锁定设备号和工况
2. 检索阶段优先查询事实接口：
   - `GET /api/v1/equipment/{equipmentId}/digital-twin`
   - `GET /api/v1/equipment/{equipmentId}/telemetry`
   - `GET /api/v1/equipment/{equipmentId}/historical-cases`
   - `GET /api/v1/equipment/{equipmentId}/change-relations`
   - `GET /api/v1/equipment/{equipmentId}/personnel-match`
   - `GET /api/v1/equipment/{equipmentId}/inventory`
3. 执行阶段优先调用：
   - `POST /api/v1/equipment/{equipmentId}/workflow/execution`
4. 反馈阶段优先调用：
   - `POST /api/v1/equipment/{equipmentId}/workflow/feedback`
5. 需要单次综合结论时再调用：
   - `POST /api/v1/equipment/{equipmentId}/diagnosis-conclusion`
   - `POST /api/v1/equipment/{equipmentId}/disposition-decision`

## 输出约束

- 不要编造 MEL、AMM、库存、人员位置、工单号或诊断结论
- 只使用接口返回的事实
- 如果缺少关键参数，只在检索阶段追问最少必要信息：
  - 飞机场景：飞机号、渗漏面积、是否持续滴落
  - 设备场景：设备号、温度、振动、温升速率
- 输出顺序优先：
  - 检索阶段：已确认项、待补充项
  - 执行阶段：2-3 个方案 + 推荐项
  - 反馈阶段：最终步骤、人、料、工具/场地、注意事项
