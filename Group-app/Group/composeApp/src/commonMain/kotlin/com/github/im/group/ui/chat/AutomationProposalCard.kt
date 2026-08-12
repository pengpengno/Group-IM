package com.github.im.group.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AutomationProposalUi(
    val title: String,
    val summary: String,
    val scope: String? = null,
    val affectedCount: Int? = null
)

@Composable
fun AutomationProposalCard(
    proposal: AutomationProposalUi,
    pending: Boolean = false,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("需确认", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(proposal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(proposal.summary, style = MaterialTheme.typography.bodyMedium)
            proposal.scope?.let { Text("影响范围：$it", style = MaterialTheme.typography.bodySmall) }
            proposal.affectedCount?.let { Text("将影响 $it 个对象", style = MaterialTheme.typography.bodySmall) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDecline, enabled = !pending, modifier = Modifier.weight(1f)) { Text("拒绝") }
                Button(onClick = onApprove, enabled = !pending, modifier = Modifier.weight(1f)) { Text(if (pending) "处理中…" else "批准执行") }
            }
        }
    }
}
