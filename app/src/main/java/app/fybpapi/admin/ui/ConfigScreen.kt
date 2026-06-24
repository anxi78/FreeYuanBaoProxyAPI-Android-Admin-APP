package app.fybpapi.admin.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fybpapi.admin.api.ApiService
import app.fybpapi.admin.api.ConfigData

@Composable
fun ConfigScreen() {
    val context = LocalContext.current

    var config by remember { mutableStateOf<ConfigData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var restarting by remember { mutableStateOf(false) }

    // 加载配置
    LaunchedEffect(Unit) {
        loading = true
        Thread {
            config = ApiService.getConfig()
            loading = false
        }.start()
    }

    // 输入框状态（用反射/直接映射稍麻烦，最简单：复制一份 ConfigData 用 var 字段）
    // 直接在 Composable 里复用 ConfigData 的 var 字段

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "配置管理",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (config == null) {
            Text(
                text = "无法加载配置：未授权或连接失败",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            val c = config!!

            ConfigField("APP_ID", c.APP_ID) { c.APP_ID = it }
            ConfigField("APP_SECRET", c.APP_SECRET) { c.APP_SECRET = it }
            ConfigField("GROUP_CODE", c.GROUP_CODE) { c.GROUP_CODE = it }
            ConfigField("YUANBAO_USER_ID", c.YUANBAO_USER_ID) { c.YUANBAO_USER_ID = it }
            ConfigField("YUANBAO_NICK", c.YUANBAO_NICK) { c.YUANBAO_NICK = it }
            ConfigField("PORT", c.PORT.toString()) { c.PORT = it.toIntOrNull() ?: 35500 }
            ConfigField("API_KEY", c.API_KEY) { c.API_KEY = it }
            ConfigField("ADMIN_USERNAME", c.ADMIN_USERNAME) { c.ADMIN_USERNAME = it }
            ConfigField("ADMIN_PASSWORD", c.ADMIN_PASSWORD) { c.ADMIN_PASSWORD = it }
            ConfigField("API_DOMAIN", c.API_DOMAIN) { c.API_DOMAIN = it }

            Spacer(Modifier.height(24.dp))

            // ── 保存按钮 ──
            Button(
                onClick = {
                    saving = true
                    Thread {
                        val err = ApiService.saveConfig(config!!)
                        saving = false
                        // 返回主线程弹 Toast
                        android.os.Handler(context.mainLooper).post {
                            if (err == null) {
                                Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "保存失败：$err", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.start()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !saving && !restarting
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("保存配置", fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 重启按钮 ──
            OutlinedButton(
                onClick = {
                    restarting = true
                    Thread {
                        val err = ApiService.restart()
                        restarting = false
                        android.os.Handler(context.mainLooper).post {
                            if (err == null) {
                                Toast.makeText(context, "服务已重启", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "重启失败：$err", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.start()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !saving && !restarting,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                if (restarting) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("重启服务", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ConfigField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}
