package com.github.im.group.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.im.group.viewmodel.ChatRoomViewModel
import com.github.im.group.api.AiBotApi
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private val botCardJson = Json { ignoreUnknownKeys = true }

@Composable
fun BotCardMessage(
    rawText: String,
    isOwnMessage: Boolean,
    isStructuredCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val card = remember(rawText, isStructuredCard) { parseBotCard(rawText, isStructuredCard) }
    if (card == null) {
        RichTextMessage(rawText = rawText, isOwnMessage = isOwnMessage, modifier = modifier)
        return
    }

    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var pending by remember(rawText) { mutableStateOf(false) }
    var decision by remember(rawText) { mutableStateOf<String?>(null) }
    val approvalId = card.approvalId ?: card.actions.firstOrNull { it.approvalId.isNotBlank() }?.approvalId
    if (!approvalId.isNullOrBlank()) {
        if (decision != null) {
            Text(if (decision == "APPROVED") "已批准，等待安全执行器处理。" else "已拒绝本次操作。", modifier = modifier.padding(12.dp))
        } else {
            AutomationProposalCard(
                proposal = AutomationProposalUi(card.title.ifBlank { "需确认的自动化操作" }, card.summary),
                pending = pending,
                onApprove = {
                    scope.launch {
                        pending = true
                        runCatching { AiBotApi.decideApproval(approvalId, true) }
                            .onSuccess { decision = it.status }
                        pending = false
                    }
                },
                onDecline = {
                    scope.launch {
                        pending = true
                        runCatching { AiBotApi.decideApproval(approvalId, false) }
                            .onSuccess { decision = it.status }
                        pending = false
                    }
                },
                modifier = modifier
            )
        }
        return
    }
    val containerColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color(0xFFF8FAFC)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (card.title.isNotBlank()) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (card.summary.isNotBlank()) {
                Text(
                    text = card.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            card.sections.forEach { section ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = if (isOwnMessage) 0.3f else 0.85f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (section.title.isNotBlank()) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (section.text.isNotBlank()) {
                        Text(
                            text = section.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (card.actions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    card.actions.forEach { action ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.clickable {
                                when {
                                    action.url.isNotBlank() -> uriHandler.openUri(action.url)
                                    action.value.isNotBlank() -> clipboardManager.setText(AnnotatedString(action.value))
                                }
                            }
                        ) {
                            Text(
                                text = action.label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseBotCard(rawText: String, isStructuredCard: Boolean): BotCardModel? {
    if (!isStructuredCard && !rawText.startsWith(ChatRoomViewModel.BOT_CARD_PREFIX)) return null
    val jsonText = if (isStructuredCard) rawText.trim() else rawText.removePrefix(ChatRoomViewModel.BOT_CARD_PREFIX).trim()
    val json = runCatching { botCardJson.parseToJsonElement(jsonText) }.getOrNull() as? JsonObject ?: return null

    return BotCardModel(
        title = json["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        summary = json["summary"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        approvalId = json["approvalId"]?.jsonPrimitive?.contentOrNull,
        sections = (json["sections"] as? JsonArray).orEmpty().mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            BotCardSection(
                title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                text = obj["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
        },
        actions = (json["actions"] as? JsonArray).orEmpty().mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            BotCardAction(
                label = obj["label"]?.jsonPrimitive?.contentOrNull ?: "操作",
                url = obj["url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                value = obj["value"]?.jsonPrimitive?.contentOrNull.orEmpty()
                , approvalId = obj["approvalId"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
        }
    )
}

private data class BotCardModel(
    val title: String,
    val summary: String,
    val approvalId: String?,
    val sections: List<BotCardSection>,
    val actions: List<BotCardAction>
)

private data class BotCardSection(
    val title: String,
    val text: String
)

private data class BotCardAction(
    val label: String,
    val url: String,
    val value: String,
    val approvalId: String
)
