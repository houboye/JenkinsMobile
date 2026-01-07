package com.by.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.by.android.data.model.Server
import com.by.android.ui.theme.AndroidTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    serverFlow: Flow<Server>,
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val server by serverFlow.collectAsState(initial = Server.Empty)
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Server Config Section
            SectionHeader(title = "服务器配置")
            
            SettingsItem(
                icon = Icons.Default.Language,
                title = "服务器",
                subtitle = server.url.ifEmpty { "-" }
            )
            
            SettingsItem(
                icon = Icons.Default.Person,
                title = "用户名",
                subtitle = server.username.ifEmpty { "-" }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // App Info Section
            SectionHeader(title = "应用信息")
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "版本",
                subtitle = "1.0.0"
            )
            
            SettingsItem(
                icon = Icons.Default.Build,
                title = "构建号",
                subtitle = "1"
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Actions Section
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "退出登录",
                subtitle = null,
                titleColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { showLogoutDialog = true }
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null
) {
    val itemModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }
    
    ListItem(
        modifier = itemModifier,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = titleColor
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    AndroidTheme {
        SettingsScreen(
            serverFlow = MutableStateFlow(Server("https://jenkins.example.com", "admin", "token")) as Flow<Server>,
            onBackClick = {},
            onLogout = {}
        )
    }
}

