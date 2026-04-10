package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    val currentCompany = companies.find { it.companyId == userInfo?.currentLoginCompanyId } ?: companies.firstOrNull()
    var isWorkspaceExpanded by androidx.compose.runtime.remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(ThemeTokens.BackgroundDark)
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        // App Title
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("G", color = ThemeTokens.BackgroundDark, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Group IM",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workspace Switcher (Web-like)
        WorkspaceSwitcher(
            currentCompany = currentCompany,
            allCompanies = companies,
            isExpanded = isWorkspaceExpanded,
            onToggle = { isWorkspaceExpanded = !isWorkspaceExpanded },
            onSwitch = { id ->
                isWorkspaceExpanded = false
                onSwitchWorkspace(id)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Actions Surface
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.08f), // Premium dark glass
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                DrawerActionItem(
                    label = "联系人",
                    icon = Icons.Default.People,
                    onClick = onContactsClick,
                    accentColor = Color(0xFF3B82F6)
                )
                DrawerActionItem(
                    label = "音视频会议",
                    icon = Icons.Default.VideoCall,
                    onClick = onMeetingsClick,
                    accentColor = Color(0xFF10B981)
                )
                DrawerActionItem(
                    label = "个人资料",
                    icon = Icons.Default.Person,
                    onClick = onProfileClick,
                    accentColor = Color(0xFFF59E0B)
                )
                DrawerActionItem(
                    label = "系统设置",
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClick,
                    accentColor = Color(0xFF8B5CF6)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Current User Profile
        Surface(
            onClick = onProfileClick,
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(username = userInfo?.username ?: "Guest", size = 44)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        userInfo?.username ?: "未登录",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        userInfo?.email ?: "点击管理账号",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.Start)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color(0xFFF87171))
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Group IM $appVersion",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun WorkspaceSwitcher(
    currentCompany: CompanyDTO?,
    allCompanies: List<CompanyDTO>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSwitch: (Long) -> Unit
) {
    Column {
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isExpanded) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ThemeTokens.PrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        currentCompany?.name?.take(1)?.uppercase() ?: "G",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currentCompany?.name ?: "Personal Hub",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${allCompanies.size} 个工作空间",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp).let {
                        if (!isExpanded) it else it // Add rotation if needed
                    }
                )
            }
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "切换工作空间",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp)
                    )
                    allCompanies.forEach { company ->
                        val isActive = company.companyId == currentCompany?.companyId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isActive) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { if (!isActive) onSwitch(company.companyId) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) ThemeTokens.PrimaryBlue else Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    company.name.take(1).uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                company.name,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Add workspace logic */ }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "添加新工作空间",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerActionItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accentColor: Color
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

