package com.github.im.group.ui.workbench.notification

import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

private const val GENERIC_FALLBACK = "工作台消息暂无法展示，请进入工作台查看。"
private val EVENT_ID_PATTERN = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
private val ACTION_PATTERN = Regex("^[A-Z][A-Z0-9_]{0,63}$")
private val CATEGORY_PATH = mapOf(
    WorkbenchCardCategory.TASK to "task",
    WorkbenchCardCategory.APPROVAL to "approval",
    WorkbenchCardCategory.ANNOUNCEMENT to "announcement",
    WorkbenchCardCategory.SCHEDULE to "schedule",
    WorkbenchCardCategory.REPORT to "report",
)
private val KNOWN_ACTIONS = mapOf(
    WorkbenchCardCategory.TASK to setOf("ASSIGNED", "COMPLETED", "REOPENED", "DUE_SOON", "STATUS_CHANGED"),
    WorkbenchCardCategory.APPROVAL to setOf("PENDING", "APPROVED", "REJECTED", "RETURNED"),
    WorkbenchCardCategory.ANNOUNCEMENT to setOf("PUBLISHED"),
    WorkbenchCardCategory.SCHEDULE to setOf("CREATED", "UPDATED", "REMINDER"),
    WorkbenchCardCategory.REPORT to setOf("CREATED", "UPDATED", "READY"),
)

private val workbenchJson = Json { ignoreUnknownKeys = true }

enum class WorkbenchCardCategory {
    TASK,
    APPROVAL,
    ANNOUNCEMENT,
    SCHEDULE,
    REPORT,
}

data class WorkbenchCardEnvelopeV1(
    val version: Int,
    val eventId: String,
    val category: WorkbenchCardCategory,
    val action: String,
    val resourceId: String,
    val companyId: Long,
    val title: String,
    val summary: String?,
    val fallbackText: String,
    val status: String?,
    val occurredAt: String,
    val deepLink: String,
)

data class WorkbenchDeepLinkTarget(
    val category: WorkbenchCardCategory,
    val resourceId: String,
    val companyId: Long,
    val deepLink: String,
)

enum class WorkbenchCardFallbackReason {
    MALFORMED,
    UNSUPPORTED_VERSION,
    UNKNOWN_CATEGORY,
    UNKNOWN_ACTION,
    INVALID_CONTRACT,
}

sealed interface WorkbenchCardParseResult {
    data class Valid(
        val card: WorkbenchCardEnvelopeV1,
        val target: WorkbenchDeepLinkTarget,
    ) : WorkbenchCardParseResult

    data class Fallback(
        val fallbackText: String,
        val reason: WorkbenchCardFallbackReason,
    ) : WorkbenchCardParseResult
}

fun parseWorkbenchCard(content: String): WorkbenchCardParseResult {
    val root = runCatching { workbenchJson.parseToJsonElement(content) }.getOrNull()
        ?: return WorkbenchCardParseResult.Fallback(GENERIC_FALLBACK, WorkbenchCardFallbackReason.MALFORMED)
    val record = root as? JsonObject
        ?: return WorkbenchCardParseResult.Fallback(GENERIC_FALLBACK, WorkbenchCardFallbackReason.MALFORMED)
    val fallbackText = record.safeText("fallbackText", 300, required = false)
        ?.takeIf { it.isNotBlank() }
        ?: GENERIC_FALLBACK

    val version = record["version"].primitiveIntOrNull()
    if (version != 1) {
        return WorkbenchCardParseResult.Fallback(fallbackText, WorkbenchCardFallbackReason.UNSUPPORTED_VERSION)
    }

    val category = record["category"]?.primitiveContentOrNull()
        ?.let { raw -> WorkbenchCardCategory.entries.firstOrNull { it.name == raw } }
        ?: return WorkbenchCardParseResult.Fallback(fallbackText, WorkbenchCardFallbackReason.UNKNOWN_CATEGORY)

    val action = record["action"]?.primitiveContentOrNull()
    if (action == null || !ACTION_PATTERN.matches(action) || action !in KNOWN_ACTIONS.getValue(category)) {
        return WorkbenchCardParseResult.Fallback(fallbackText, WorkbenchCardFallbackReason.UNKNOWN_ACTION)
    }

    val eventId = record["eventId"]?.primitiveContentOrNull()
    val resourceId = record.safeText("resourceId", 128)
    val companyId = record["companyId"].primitiveLongOrNull()
    val title = record.safeText("title", 120)
    val explicitFallback = record.safeText("fallbackText", 300)
    val summary = record.safeText("summary", 300, required = false)
    val status = record.safeText("status", 32, required = false)
    val occurredAt = record["occurredAt"]?.primitiveContentOrNull()
    val deepLink = record["deepLink"]?.primitiveContentOrNull()
    val target = deepLink?.let(::parseWorkbenchDeepLink)

    val contractValid =
        eventId != null && EVENT_ID_PATTERN.matches(eventId) &&
            resourceId != null &&
            companyId != null && companyId > 0 &&
            title != null &&
            explicitFallback != null &&
            occurredAt != null && runCatching { Instant.parse(occurredAt) }.isSuccess &&
            target != null &&
            target.category == category &&
            target.companyId == companyId &&
            target.resourceId == resourceId

    if (!contractValid) {
        return WorkbenchCardParseResult.Fallback(fallbackText, WorkbenchCardFallbackReason.INVALID_CONTRACT)
    }

    val card = WorkbenchCardEnvelopeV1(
        version = 1,
        eventId = eventId!!,
        category = category,
        action = action,
        resourceId = resourceId!!,
        companyId = companyId!!,
        title = title!!,
        summary = summary,
        fallbackText = explicitFallback!!,
        status = status,
        occurredAt = occurredAt!!,
        deepLink = deepLink!!,
    )
    return WorkbenchCardParseResult.Valid(card, target!!)
}

fun parseWorkbenchDeepLink(value: String): WorkbenchDeepLinkTarget? {
    val prefix = "group://workbench/"
    if (!value.startsWith(prefix)) return null
    if (value.count { it == '?' } != 1) return null

    val pathAndQuery = value.removePrefix(prefix)
    val path = pathAndQuery.substringBefore('?')
    val query = pathAndQuery.substringAfter('?', missingDelimiterValue = "")
    if (!query.startsWith("companyId=") || '&' in query) return null

    val companyRaw = query.removePrefix("companyId=")
    if (companyRaw.isBlank() || companyRaw.any { !it.isDigit() }) return null
    val companyId = companyRaw.toLongOrNull()?.takeIf { it > 0 } ?: return null

    val parts = path.split('/')
    if (parts.size != 2 || parts.any { it.isBlank() }) return null
    val category = CATEGORY_PATH.entries.firstOrNull { it.value == parts[0] }?.key ?: return null
    val resourceId = percentDecode(parts[1]) ?: return null
    if (resourceId.isBlank() || resourceId.length > 128) return null

    val canonical = "group://workbench/${CATEGORY_PATH.getValue(category)}/${percentEncode(resourceId)}?companyId=$companyId"
    if (canonical != value) return null

    return WorkbenchDeepLinkTarget(category, resourceId, companyId, canonical)
}

private fun JsonObject.safeText(key: String, maxLength: Int, required: Boolean = true): String? {
    val element = this[key] ?: return if (required) null else null
    if (element === kotlinx.serialization.json.JsonNull) return if (required) null else null
    val value = element.primitiveContentOrNull() ?: return null
    if (value.length > maxLength) return null
    if (required && value.isBlank()) return null
    return value
}

private fun JsonElement?.primitiveContentOrNull(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive.isString) return primitive.contentOrNull
    return null
}

private fun JsonElement?.primitiveLongOrNull(): Long? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive.booleanOrNull != null) return null
    return primitive.longOrNull
}

private fun JsonElement?.primitiveIntOrNull(): Int? {
    val value = primitiveLongOrNull() ?: return null
    return value.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()
}

private fun percentEncode(value: String): String {
    val hex = "0123456789ABCDEF"
    return buildString {
        value.encodeToByteArray().forEach { byte ->
            val unsigned = byte.toInt() and 0xFF
            val safe =
                unsigned in 'a'.code..'z'.code ||
                    unsigned in 'A'.code..'Z'.code ||
                    unsigned in '0'.code..'9'.code ||
                    unsigned == '-'.code || unsigned == '.'.code || unsigned == '_'.code || unsigned == '~'.code
            if (safe) {
                append(unsigned.toChar())
            } else {
                append('%')
                append(hex[unsigned ushr 4])
                append(hex[unsigned and 0x0F])
            }
        }
    }
}

private fun percentDecode(value: String): String? {
    val bytes = mutableListOf<Byte>()
    var index = 0
    while (index < value.length) {
        val ch = value[index]
        when {
            ch == '%' -> {
                if (index + 2 >= value.length) return null
                val high = value[index + 1].hexValue() ?: return null
                val low = value[index + 2].hexValue() ?: return null
                bytes += ((high shl 4) or low).toByte()
                index += 3
            }
            ch.code <= 0x7F -> {
                bytes += ch.code.toByte()
                index++
            }
            else -> return null
        }
    }
    return runCatching { bytes.toByteArray().decodeToString() }.getOrNull()
}

private fun Char.hexValue(): Int? = when (this) {
    in '0'..'9' -> code - '0'.code
    in 'a'..'f' -> code - 'a'.code + 10
    in 'A'..'F' -> code - 'A'.code + 10
    else -> null
}
