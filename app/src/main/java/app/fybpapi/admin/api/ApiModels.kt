package app.fybpapi.admin.api

import com.google.gson.annotations.SerializedName

// ── 请求体 ──

data class LoginRequest(
    val username: String,
    val password: String
)

data class ChatRequest(
    val message: String
)

// ── 配置 ──

data class ConfigData(
    @SerializedName("APP_ID") var APP_ID: String = "",
    @SerializedName("APP_SECRET") var APP_SECRET: String = "",
    @SerializedName("GROUP_CODE") var GROUP_CODE: String = "",
    @SerializedName("YUANBAO_USER_ID") var YUANBAO_USER_ID: String = "",
    @SerializedName("YUANBAO_NICK") var YUANBAO_NICK: String = "元宝",
    @SerializedName("PORT") var PORT: Int = 35500,
    @SerializedName("API_KEY") var API_KEY: String = "",
    @SerializedName("ADMIN_USERNAME") var ADMIN_USERNAME: String = "admin",
    @SerializedName("ADMIN_PASSWORD") var ADMIN_PASSWORD: String = "",
    @SerializedName("API_DOMAIN") var API_DOMAIN: String = "bot.yuanbao.tencent.com"
)

// ── 响应体 ──

data class StatusResponse(
    val status: String = "",
    val error: String? = null
)

data class ChatResponse(
    val reply: String = "",
    val error: String? = null
)
