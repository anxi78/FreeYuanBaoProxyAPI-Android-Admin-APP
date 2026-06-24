package app.fybpapi.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.fybpapi.admin.ui.ChatScreen
import app.fybpapi.admin.ui.ConfigScreen
import app.fybpapi.admin.ui.LoginScreen
import app.fybpapi.admin.ui.theme.YbAdminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YbAdminTheme {
                AppRoot()
            }
        }
    }
}

// ── Tab 定义 ──
private sealed class Tab(val label: String, val icon: ImageVector) {
    data object Config : Tab("配置", Icons.Default.Settings)
    data object Chat   : Tab("聊天", Icons.Default.Chat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    var loggedIn by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf<Tab>(Tab.Config) }

    if (!loggedIn) {
        LoginScreen(onLoginSuccess = { loggedIn = true })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("元宝Admin") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        TextButton(onClick = {
                            app.fybpapi.admin.api.ApiService.logout()
                            loggedIn = false
                        }) {
                            Text("退出", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab is Tab.Config,
                        onClick = { selectedTab = Tab.Config },
                        icon = { Icon(Tab.Config.icon, contentDescription = null) },
                        label = { Text(Tab.Config.label) }
                    )
                    NavigationBarItem(
                        selected = selectedTab is Tab.Chat,
                        onClick = { selectedTab = Tab.Chat },
                        icon = { Icon(Tab.Chat.icon, contentDescription = null) },
                        label = { Text(Tab.Chat.label) }
                    )
                }
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    is Tab.Config -> ConfigScreen()
                    is Tab.Chat   -> ChatScreen()
                }
            }
        }
    }
}
