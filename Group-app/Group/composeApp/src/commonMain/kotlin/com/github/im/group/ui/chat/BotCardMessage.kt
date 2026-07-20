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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.im.group.viewmodel.ChatRoomViewModel
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
    modifier: Modifier = Modifier
) {
    val card = remember(rawText) { parseBotCard(rawText) }
    if (card == null) {
        RichTextMessage(rawText = rawText, isOwnMessage = isOwnMessage, modifier = modifier)
        return
    }

    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
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

private fun parseBotCard(rawText: String): BotCardModel? {
    if (!rawText.startsWith(ChatRoomViewModel.BOT_CARD_PREFIX)) return null
    val jsonText = rawText.removePrefix(ChatRoomViewModel.BOT_CARD_PREFIX).trim()
    val json = runCatching { botCardJson.parseToJsonElement(jsonText) }.getOrNull() as? JsonObject ?: return null

    return BotCardModel(
        title = json["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        summary = json["summary"]?.jsonPrimitive?.contentOrNull.orEmpty(),
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
            )
        }
    )
}

private data class BotCardModel(
    val title: String,
    val summary: String,
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
    val value: String
)
