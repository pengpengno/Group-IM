package com.github.im.group.ui.workbench.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WorkbenchCardTest {

    @Test
    fun validTaskCardParsesWithCanonicalTarget() {
        val result = parseWorkbenchCard(
            """
            {
              "version": 1,
              "eventId": "123e4567-e89b-12d3-a456-426614174000",
              "category": "TASK",
              "action": "ASSIGNED",
              "resourceId": "901",
              "companyId": 42,
              "title": "处理上线检查",
              "summary": "请确认发布前检查项",
              "fallbackText": "有一条新的工作台任务",
              "status": "TODO",
              "occurredAt": "2026-08-24T02:00:00Z",
              "deepLink": "group://workbench/task/901?companyId=42"
            }
            """.trimIndent(),
        )

        val valid = assertIs<WorkbenchCardParseResult.Valid>(result)
        assertEquals(WorkbenchCardCategory.TASK, valid.target.category)
        assertEquals("901", valid.target.resourceId)
        assertEquals(42L, valid.target.companyId)
    }

    @Test
    fun malformedPayloadFallsBack() {
        val result = parseWorkbenchCard("{not-json")
        val fallback = assertIs<WorkbenchCardParseResult.Fallback>(result)
        assertEquals(WorkbenchCardFallbackReason.MALFORMED, fallback.reason)
    }

    @Test
    fun unknownActionFallsBackWithoutNavigation() {
        val result = parseWorkbenchCard(
            """
            {
              "version": 1,
              "eventId": "123e4567-e89b-12d3-a456-426614174000",
              "category": "TASK",
              "action": "DELETE_EVERYTHING",
              "resourceId": "901",
              "companyId": 42,
              "title": "任务",
              "fallbackText": "工作台消息",
              "occurredAt": "2026-08-24T02:00:00Z",
              "deepLink": "group://workbench/task/901?companyId=42"
            }
            """.trimIndent(),
        )
        val fallback = assertIs<WorkbenchCardParseResult.Fallback>(result)
        assertEquals(WorkbenchCardFallbackReason.UNKNOWN_ACTION, fallback.reason)
    }

    @Test
    fun forgedDeepLinkCompanyIsRejected() {
        val result = parseWorkbenchCard(
            """
            {
              "version": 1,
              "eventId": "123e4567-e89b-12d3-a456-426614174000",
              "category": "TASK",
              "action": "ASSIGNED",
              "resourceId": "901",
              "companyId": 42,
              "title": "任务",
              "fallbackText": "工作台消息",
              "occurredAt": "2026-08-24T02:00:00Z",
              "deepLink": "group://workbench/task/901?companyId=99"
            }
            """.trimIndent(),
        )
        val fallback = assertIs<WorkbenchCardParseResult.Fallback>(result)
        assertEquals(WorkbenchCardFallbackReason.INVALID_CONTRACT, fallback.reason)
    }

    @Test
    fun percentEncodedResourceMustBeCanonical() {
        val target = parseWorkbenchDeepLink("group://workbench/report/weekly%20report?companyId=7")
        requireNotNull(target)
        assertEquals("weekly report", target.resourceId)
        assertEquals("group://workbench/report/weekly%20report?companyId=7", target.deepLink)
    }
}
