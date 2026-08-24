# Workbench 开源组件复用评审

> 评审日期：2026-08-24  
> 原则：优先补齐可靠性与运维能力，不引入第二份 tenant / permission / business truth。

## 结论

| 项目 | 许可证 | 判定 | Group-IM 用法 |
| --- | --- | --- | --- |
| [Spring Modulith](https://github.com/spring-projects/spring-modulith) | Apache-2.0 | **优先做 #55 Spike** | 评估 Event Publication Registry 持久化未完成事件，避免纯 `AFTER_COMMIT` 在进程崩溃时丢通知。必须使用 tenant-aware 存储、幂等 `eventId` 和可重放 handler。 |
| [Microsoft Adaptive Cards](https://github.com/microsoft/AdaptiveCards) | MIT | **只参考，不替换 V1 协议** | 参考 version fallback、host styling 和未知元素降级；不引入其通用 action schema，避免绕过 Group-IM 的 category/action allowlist 和 server re-authorization。 |
| [Gotify](https://github.com/gotify/server) | MIT | **参考，不内嵌 server** | 借鉴 WebSocket 重连、Android 后台收件和优先级语义。直接部署会引入第二套用户/令牌/消息库，与现有 IM ClientEvent/Push 重复。 |
| [UnifiedPush Android Connector](https://github.com/UnifiedPush/android-connector) | Apache-2.0 | **后续可选 transport** | 作为无 Google 环境的 Android push adapter 候选；只应接在 #55 transport SPI 后，不进入 Workbench 业务或授权层。 |
| [Flowable](https://github.com/flowable/flowable-engine) | Apache-2.0 | **Approval V1 不引入** | #56 是固定串行 Nodes，自有状态机更小且更容易保持 tenant/audit 边界。仅在出现用户可配 BPMN、并行网关、定时器补偿等硬需求时再立项。 |

## 对当前路线的影响

1. #50 不引入外部依赖；只完成 Flyway-proven managed-core storage contract。
2. #54 定义 transport kill switch 和 minimum-client matrix，不把某个 push 供应商当成协议。
3. #55 实现前先做 Spring Modulith 持久化事件 Spike，并与自建 transactional outbox 对比；验收是“业务提交后崩溃，重启仍可重放且不重复发卡”。
4. #56 保持轻量领域状态机，不为可能的未来复杂流程提前引入 BPMN 引擎。

## 禁止边界

- 开源卡片 action 不能直接授权 Approve/Complete；
- push payload 只允许低敏 routing hint；
- 事件重放必须 tenant-aware，且以 stable `eventId` 幂等；
- 不允许开源组件建立第二套 Task/Approval 业务真相。
