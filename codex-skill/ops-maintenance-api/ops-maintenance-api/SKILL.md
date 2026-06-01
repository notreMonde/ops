---
name: ops-maintenance-api
description: Use when the user needs to query the maintenance decision backend for aircraft maintenance or equipment maintenance facts, diagnoses, and disposition decisions.
---

# Ops Maintenance API

这个 Skill 用于引导 Codex 调用运维决策后端接口，完成飞机维修和设备维修场景下的事实查询、诊断归纳和处置决策生成。

## 何时使用

- 用户提到飞机故障、MEL 放行、AMM 步骤、航材、工具、机库、放行判断
- 用户提到设备故障、数字孪生、实时温度/振动、历史案例、备件、工具、停机决策
- 用户希望从自然语言直接得到“结论 + 证据 + 人员 + 物料 + 场地/位置”

## 工具契约

- OpenAPI 规范：`openapi.yaml`
- 系统提示词：`system-prompt.md`
- 服务默认地址：`http://localhost:8080`

## 调用策略

### 飞机维修

1. 先锁定飞机号和故障部位
2. 先查询事实接口：
   - `GET /api/v1/aircraft/{tailNo}/status-history`
   - `GET /api/v1/aircraft/{tailNo}/mel-release`
   - `GET /api/v1/aircraft/{tailNo}/environment`
   - `GET /api/v1/aircraft/{tailNo}/troubleshooting-kb`
   - `GET /api/v1/aircraft/{tailNo}/knowledge-graph`
   - `GET /api/v1/aircraft/{tailNo}/personnel-match`
   - `GET /api/v1/aircraft/{tailNo}/inventory`
3. 需要综合结论时再调用：
   - `POST /api/v1/aircraft/{tailNo}/diagnosis-conclusion`
   - `POST /api/v1/aircraft/{tailNo}/disposition-decision`
4. 回答顺序优先：
   - 是否可放行
   - 依据是什么
   - 建议怎么修
   - 由谁修、在哪里修、用什么料和工具

### 设备维修

1. 先锁定设备号和工况
2. 先查询事实接口：
   - `GET /api/v1/equipment/{equipmentId}/digital-twin`
   - `GET /api/v1/equipment/{equipmentId}/telemetry`
   - `GET /api/v1/equipment/{equipmentId}/historical-cases`
   - `GET /api/v1/equipment/{equipmentId}/change-relations`
   - `GET /api/v1/equipment/{equipmentId}/personnel-match`
   - `GET /api/v1/equipment/{equipmentId}/inventory`
3. 需要综合结论时再调用：
   - `POST /api/v1/equipment/{equipmentId}/diagnosis-conclusion`
   - `POST /api/v1/equipment/{equipmentId}/disposition-decision`
4. 回答顺序优先：
   - 是否应立即停机
   - 根因和证据链
   - 建议处置步骤
   - 推荐人员、备件和工具位置

## 约束

- 不要编造 MEL、AMM、库存、人员位置、工单号或诊断结论
- 只使用接口返回的事实
- 如果缺少关键参数，只追问最少必要信息：
  - 飞机场景：飞机号、渗漏面积、是否持续滴落
  - 设备场景：设备号、温度、振动、温升速度
