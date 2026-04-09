package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.im.group.model.CompanyDTO
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.theme.ThemeTokens

@Composable
fun SideDrawer(
    userInfo: UserInfo?,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onGroupsClick: () -> Unit = {},
    onMeetingsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSwitchWorkspace: (Long) -> Unit = {},
    appVersion: String = "v1.0.0"
) {
    val companies = userInfo?.companies.orEmpty()
    val currentCompany = companies.firstOrNull { it.companyId == userInfo?.currentLoginCompanyId } ?: companies.firstOrNull()
    val joinedCompanies = companies.filterNot { it.companyId == currentCompany?.companyId }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(ThemeTokens.BackgroundDark)
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        WorkspaceHero(
            currentCompany = currentCompany,
            joinedCompanies = joinedCompanies,
            onSwitchWorkspace = onSwitchWorkspace
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.92f),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(username = userInfo?.username ?: "Guest", size = 48)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userInfo?.username ?: "未登录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = userInfo?.email ?: "查看个人资料",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(10.dp))

                DrawerItem(
                    label = "联系人",
                    icon = Icons.Default.People,
                    onClick = onContactsClick,
                    containerColor = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0284C7)
                )
                DrawerItem(
                    label = "音视频会议",
                    icon = Icons.Default.VideoCall,
                    onClick = onMeetingsClick,
                    containerColor = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A)
                )
                DrawerItem(
                    label = "个人资料",
                    icon = Icons.Default.Person,
                    onClick = onProfileClick,
                    containerColor = Color(0xFFFFEDD5),
                    iconTint = Color(0xFFEA580C)
                )
                DrawerItem(
                    label = "系统设置",
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClick,
                    containerColor = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF7C3AED)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        DrawerItem(
            label = "退出登录",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            onClick = onLogout,
            iconTint = Color(0xFFDC2626),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "版本 $appVersion",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun WorkspaceHero(
    currentCompany: CompanyDTO?,
    joinedCompanies: List<CompanyDTO>,
    onSwitchWorkspace: (Long) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "当前工作区",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF3B82F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentCompany?.name?.take(1)?.uppercase() ?: "G",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentCompany?.name ?: "默认工作区",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentCompany?.code ?: "GROUP",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            if (joinedCompanies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "已加入的公司",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height((joinedCompanies.size.coerceAtMost(4) * 56).dp)
                ) {
                    items(joinedCompanies) { company ->
                        WorkspaceRow(company = company, onClick = { onSwitchWorkspace(company.companyId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceRow(company: CompanyDTO, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Apartment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = company.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = company.code,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
            Text(
                text = "切换",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF93C5FD)
            )
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color = Color.Transparent,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier.padding(vertical = 4.dp)
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(containerColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (containerColor == Color.Transparent) Color.White else Color(0xFF111827),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
