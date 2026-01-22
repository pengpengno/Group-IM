package com.github.im.group.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.ui.Login
import com.github.im.group.ui.UserAvatar
import com.github.im.group.viewmodel.UserViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileUI(
    navHostController: NavHostController
) {
    val userViewModel: UserViewModel = koinViewModel()
    val loginStateManager: LoginStateManager = koinInject()
    val currentUser = userViewModel.getCurrentUser()
    
    var showEditOptions by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人资料") },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditOptions = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
            ) {
                // 用户头像和基本信息区域
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 用户头像（大图）
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(80.dp)), // 圆形头像
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatar(
                                username = currentUser?.username ?: "",
                                size = 120
                            )
                            
                            // 编辑图标覆盖在右下角
                            if (showEditOptions) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .align(Alignment.BottomEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "编辑头像",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 用户昵称
                        Text(
                            text = currentUser?.username ?: "未设置昵称",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 用户ID / 手机号 / 邮箱
                        Text(
                            text = currentUser?.email ?: "未绑定邮箱",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 个性签名或状态
                        Text(
                            text = "在线",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // 功能按钮列表
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ProfileSettingItem(
                        icon = Icons.Default.AccountCircle,
                        title = "修改头像",
                        subtitle = "点击更换头像",
                        onClick = { /* TODO: 修改头像逻辑 */ }
                    )
                    
                    HorizontalDivider()
                    
                    ProfileSettingItem(
                        icon = Icons.Default.Person,
                        title = "修改昵称",
                        subtitle = currentUser?.username ?: "未设置昵称",
                        onClick = { /* TODO: 修改昵称逻辑 */ }
                    )
                    
                    HorizontalDivider()
                    
                    ProfileSettingItem(
                        icon = Icons.Default.Lock,
                        title = "修改密码",
                        subtitle = "定期更换密码更安全",
                        onClick = { /* TODO: 修改密码逻辑 */ }
                    )
                    
                    HorizontalDivider()
                    
                    ProfileSettingItem(
                        icon = Icons.Default.PrivacyTip,
                        title = "隐私设置",
                        subtitle = "黑名单等隐私设置",
                        onClick = { navHostController.navigate("PrivacySettings") }
                    )
                    
                    HorizontalDivider()
                    
                    // 退出登录按钮
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = {
                            loginStateManager.setLoggedOut()
                            navHostController.navigate(Login) {
                                popUpTo("Profile") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        )
                    ) {
                        Text("退出登录", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    )
}

@Composable
fun ProfileSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(start = 16.dp, end = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}