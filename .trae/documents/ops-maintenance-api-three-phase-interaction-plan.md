# 三阶段分步交互方案：ops-maintenance-api 改造计划

## 一、现状分析

### 当前问题
当前 `ops-maintenance-api` skill 是**纯提示词驱动**的，虽然 system-prompt.md 中提到了"等待用户反馈"的规则，但在实际执行中容易出现以下问题：

1. **一次性全量查询**：AI 会并行查询所有事实接口（status-history / mel-release / environment / troubleshooting-kb / knowledge-graph / personnel-match / inventory），全部拿到结果后一次性呈现给用户
2. **无阶段性确认**：缺乏明确的"锁定对象→用户确认→继续查询→用户选择→最终执行"的流程控制
3. **依赖 AI 自觉**：交互节奏完全依赖 AI 对提示词的理解，不同轮次行为可能不一致
4. **无状态管理**：无法在多次对话之间维护查询状态、已选方案等上下文

### 可参考模式：opspath
`opspath` skill 提供了成熟的多轮状态机方案：

| 阶段 | 状态值 | 动作 |
|---|---|---|
| 步骤 1 | `awaiting_lock_confirm` | 锁定对象 → 展示给用户确认 |
|