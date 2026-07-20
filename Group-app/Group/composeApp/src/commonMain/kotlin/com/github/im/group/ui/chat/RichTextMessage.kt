package com.github.im.group.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

private const val URL_TAG = "url"

private enum class RichBlockType {
    PARAGRAPH,
    BULLET,
    NUMBERED,
    QUOTE,
    CODE,
    HEADING
}

private data class RichBlock(
    val type: RichBlockType,
    val text: String,
    val prefix: String = ""
)

@Composable
fun RichTextMessage(
    rawText: String,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val blocks = remember(rawText) { parseRichBlocks(rawText) }
    val textColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEach { block ->
            when (block.type) {
                RichBlockType.CODE -> {
                    val annotated = remember(block.text, textColor) {
                        buildAnnotatedString {
                            pushStyle(
                                SpanStyle(
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            append(block.text)
                            pop()
                        }
                    }
                    ClickableText(
                        text = annotated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isOwnMessage) Color.White.copy(alpha = 0.16f) else Color(0xFFF4F4F5),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                        onClick = { offset ->
                            annotated.getStringAnnotations(URL_TAG, offset, offset)
                                .firstOrNull()
                                ?.let { uriHandler.openUri(it.item) }
                        }
                    )
                }

                RichBlockType.BULLET,
                RichBlockType.NUMBERED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = block.prefix,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        RichTextLine(
                            text = block.text,
                            textColor = textColor,
                            onUrlClick = uriHandler::openUri,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                RichBlockType.QUOTE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "|",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        RichTextLine(
                            text = block.text,
                            textColor = textColor.copy(alpha = 0.9f),
                            onUrlClick = uriHandler::openUri,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                RichBlockType.HEADING -> {
                    RichTextLine(
                        text = block.text,
                        textColor = textColor,
                        onUrlClick = uriHandler::openUri,
                        modifier = Modifier,
                        isHeading = true
                    )
                }

                RichBlockType.PARAGRAPH -> {
                    RichTextLine(
                        text = block.text,
                        textColor = textColor,
                        onUrlClick = uriHandler::openUri,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun RichTextLine(
    text: String,
    textColor: Color,
    onUrlClick: (String) -> Unit,
    modifier: Modifier,
    isHeading: Boolean = false
) {
    val annotated = remember(text, textColor, isHeading) {
        buildRichAnnotatedString(text, textColor, isHeading)
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = if (isHeading) {
            MaterialTheme.typography.titleSmall.copy(color = textColor, fontWeight = FontWeight.SemiBold)
        } else {
            MaterialTheme.typography.bodyMedium.copy(color = textColor)
        },
        onClick = { offset ->
            annotated.getStringAnnotations(URL_TAG, offset, offset)
                .firstOrNull()
                ?.let { onUrlClick(it.item) }
        }
    )
}

private fun parseRichBlocks(input: String): List<RichBlock> {
    val normalized = htmlToMarkdown(input).replace("\r\n", "\n").trim()
    if (normalized.isBlank()) {
        return listOf(RichBlock(RichBlockType.PARAGRAPH, ""))
    }

    val result = mutableListOf<RichBlock>()
    val lines = normalized.split('\n')
    var index = 0
    var inCodeBlock = false
    val codeBuffer = StringBuilder()

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trimEnd()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                result += RichBlock(RichBlockType.CODE, codeBuffer.toString().trimEnd())
                codeBuffer.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            index++
            continue
        }

        if (inCodeBlock) {
            codeBuffer.appendLine(trimmed)
            index++
            continue
        }

        val text = trimmed.trim()
        if (text.isBlank()) {
            index++
            continue
        }

        when {
            text.startsWith(">") -> result += RichBlock(RichBlockType.QUOTE, text.removePrefix(">").trim())
            text.startsWith("- ") || text.startsWith("* ") -> result += RichBlock(
                RichBlockType.BULLET,
                text.drop(2).trim(),
                prefix = "-"
            )
            Regex("^\\d+\\.\\s+").containsMatchIn(text) -> {
                val prefix = text.substringBefore('.').trim() + "."
                val body = text.substringAfter('.').trim()
                result += RichBlock(RichBlockType.NUMBERED, body, prefix)
            }
            text.startsWith("#") -> result += RichBlock(
                RichBlockType.HEADING,
                text.trimStart('#').trim()
            )
            else -> result += RichBlock(RichBlockType.PARAGRAPH, text)
        }
        index++
    }

    if (codeBuffer.isNotEmpty()) {
        result += RichBlock(RichBlockType.CODE, codeBuffer.toString().trimEnd())
    }

    return if (result.isEmpty()) listOf(RichBlock(RichBlockType.PARAGRAPH, normalized)) else result
}

private fun buildRichAnnotatedString(
    text: String,
    baseColor: Color,
    isHeading: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        val linkRegex = Regex("\\[([^\\]]+)]\\((https?://[^)]+)\\)|https?://[\\w./?=&%#-]+")
        val matches = linkRegex.findAll(text).toList()
        var matchIndex = 0

        while (index < text.length) {
            val match = matches.getOrNull(matchIndex)
            if (match != null && match.range.first == index) {
                if (match.groups[2] != null) {
                    val label = match.groups[1]!!.value
                    val url = match.groups[2]!!.value
                    pushStringAnnotation(URL_TAG, url)
                    pushStyle(
                        SpanStyle(
                            color = Color(0xFF2563EB),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = if (isHeading) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                    append(label)
                    pop()
                    pop()
                } else {
                    val url = match.value
                    pushStringAnnotation(URL_TAG, url)
                    pushStyle(
                        SpanStyle(
                            color = Color(0xFF2563EB),
                            textDecoration = TextDecoration.Underline
                        )
                    )
                    append(url)
                    pop()
                    pop()
                }
                index = match.range.last + 1
                matchIndex++
                continue
            }

            val nextSpecial = listOf(
                text.indexOf("**", index).takeIf { it >= 0 },
                text.indexOf('*', index).takeIf { it >= 0 },
                text.indexOf('`', index).takeIf { it >= 0 }
            ).filterNotNull().minOrNull() ?: text.length

            if (nextSpecial > index) {
                append(text.substring(index, nextSpecial))
                index = nextSpecial
                continue
            }

            if (text.startsWith("**", index)) {
                val end = text.indexOf("**", index + 2)
                if (end > index) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                    continue
                }
            }

            if (text[index] == '*') {
                val end = text.indexOf('*', index + 1)
                if (end > index) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor))
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                    continue
                }
            }

            if (text[index] == '`') {
                val end = text.indexOf('`', index + 1)
                if (end > index) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x22000000),
                            color = baseColor
                        )
                    )
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                    continue
                }
            }

            append(text[index])
            index++
        }
    }
}

private fun htmlToMarkdown(input: String): String {
    return input
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p>|</div>|</h\\d>|</blockquote>|</pre>|</ul>|</ol>"), "\n")
        .replace(Regex("(?i)<p>|<div>|<blockquote>"), "")
        .replace(Regex("(?i)<ul[^>]*>|<ol[^>]*>"), "")
        .replace(Regex("(?i)<h\\d[^>]*>"), "# ")
        .replace(Regex("(?i)<(strong|b)>"), "**")
        .replace(Regex("(?i)</(strong|b)>"), "**")
        .replace(Regex("(?i)<(em|i)>"), "*")
        .replace(Regex("(?i)</(em|i)>"), "*")
        .replace(Regex("(?i)<code>"), "`")
        .replace(Regex("(?i)</code>"), "`")
        .replace(Regex("(?i)<pre[^>]*>"), "```\n")
        .replace(Regex("(?i)</pre>"), "\n```")
        .replace(Regex("(?i)<li[^>]*>"), "- ")
        .replace(Regex("(?i)</li>"), "\n")
        .replace(Regex("(?i)<a\\s+[^>]*href=['\"]([^'\"]+)['\"][^>]*>(.*?)</a>")) {
            "[${it.groupValues[2]}](${it.groupValues[1]})"
        }
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
}
