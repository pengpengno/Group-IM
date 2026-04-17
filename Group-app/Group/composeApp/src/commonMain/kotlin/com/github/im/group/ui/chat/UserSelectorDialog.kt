package com.github.im.group.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.UserAvatar
import com.github.im.group.viewmodel.UserViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSelectorDialog(
    title: String = "发起会议",
    initialSelectedUsers: List<UserInfo> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (List<UserInfo>) -> Unit
) {
    val userViewModel: UserViewModel = koinViewModel()
    val friends by userViewModel.friends.collectAsState()
    val searchResults by userViewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedUsers by remember { mutableStateOf(initialSelectedUsers.toSet()) }

    LaunchedEffect(Unit) {
        userViewModel.loadFriendsIfNeeded()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                    
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextButton(
                        onClick = { onConfirm(selectedUsers.toList()) },
                        enabled = selectedUsers.isNotEmpty()
                    ) {
                        Text("完成(${selectedUsers.size})", fontWeight = FontWeight.Bold)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        userViewModel.searchUser(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索联系人...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                // User List
                val displayedUsers = if (searchQuery.isNotEmpty()) {
                    searchResults
                } else {
                    friends.mapNotNull { it.friendUserInfo }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(displayedUsers) { user ->
                        val isSelected = selectedUsers.any { it.userId == user.userId }
                        UserSelectionItem(
                            user = user,
                            isSelected = isSelected,
                            onToggle = {
                                selectedUsers = if (isSelected) {
                                    selectedUsers.filter { it.userId != user.userId }.toSet()
                                } else {
                                    selectedUsers + user
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSelectionItem(
    user: UserInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(username = user.username, size = 44)
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (user.email.isNotEmpty()) {
                Text(
                    text = user.email,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
