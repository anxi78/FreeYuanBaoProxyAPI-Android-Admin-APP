package app.fybpapi.admin.api

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap

// ── 简单的内存 Cookie 存储（持久化 session） ──
class SimpleCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store.getOrPut(url.host) { mutableListOf() }.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return store[url.host] ?: emptyList()
    }

    fun clear() {
        store.clear()
    }
}

// ── HTTP API 封装 ──
object ApiService {
    private val gson = Gson()
    private val JSON = "application/json".toMediaType()

    private var cookieJar = SimpleCookieJar()
    private var http: OkHttpClient? = null
    private var _baseUrl: String = ""

    val baseUrl: String get() = _baseUrl

    /** 设置服务器地址并初始化 HTTP 客户端 */
    fun init(server: String) {
        var url = server.trim()
        if (url.isBlank()) throw IllegalArgumentException("服务器地址不能为空")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        if (!url.endsWith("/")) url += "/"
        _baseUrl = url
        cookieJar.clear()
        http = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .build()
    }

    /** 执行 HTTP 请求，返回 JSON 字符串或 null（401 未授权） */
    private fun call(method: String, path: String, body: Any? = null): String? {
        val client = http ?: throw IllegalStateException("请先设置服务器地址")
        val url = _baseUrl + path.trimStart('/')
        val jsonBody = body?.let { gson.toJson(it) }

        val req = Request.Builder().url(url).apply {
            when (method) {
                "GET" -> get()
                "POST" -> post(jsonBody?.toRequestBody(JSON) ?: "".toRequestBody(null))
            }
        }.build()

        val resp = client.newCall(req).execute()
        val code = resp.code
        val bodyStr = resp.body?.string()

        // 401 = 未登录 / session 过期，Android 端需要跳回登录页
        return if (code == 401) null else bodyStr
    }

    // ── 登录（不跟随重定向，从 302 判断成功） ──
    fun login(username: String, password: String): Boolean {
        val base = _baseUrl
        if (base.isBlank()) return false

        // 临时客户端：不跟随重定向，以便捕获 302
        val loginClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        val url = base + "admin/login"
        val body = gson.toJson(LoginRequest(username, password)).toRequestBody(JSON)
        val req = Request.Builder().url(url).post(body).build()

        return try {
            val resp = loginClient.newCall(req).execute()
            val code = resp.code
            resp.close()
            code == 302 // 302 = 登录成功（设置了 session cookie 并重定向）
        } catch (_: Exception) {
            false
        }
    }

    /** 检查 session 是否有效（调用 getConfig 看是否 401） */
    fun checkSession(): Boolean {
        return getConfig() != null
    }

    /** 获取配置 */
    fun getConfig(): ConfigData? {
        val json = call("GET", "/admin/api/config") ?: return null
        return try { gson.fromJson(json, ConfigData::class.java) } catch (_: Exception) { null }
    }

    /** 保存配置，返回错误信息，null 表示成功 */
    fun saveConfig(config: ConfigData): String? {
        val json = call("POST", "/admin/api/config", config) ?: return "未授权"
        return try {
            val r = gson.fromJson(json, StatusResponse::class.java)
            if (r.status == "ok") null else r.error ?: "保存失败"
        } catch (_: Exception) { "响应解析失败" }
    }

    /** 重启服务，返回错误信息，null 表示成功 */
    fun restart(): String? {
        val json = call("POST", "/admin/api/restart") ?: return "未授权"
        return try {
            val r = gson.fromJson(json, StatusResponse::class.java)
            if (r.status == "ok") null else r.error ?: "重启失败"
        } catch (_: Exception) { "响应解析失败" }
    }

    /** 发送聊天消息 */
    fun sendChat(message: String): String? {
        val json = call("POST", "/admin/api/chat", ChatRequest(message)) ?: return null
        return try {
            val r = gson.fromJson(json, ChatResponse::class.java)
            r.reply.ifBlank { r.error ?: "无响应" }
        } catch (_: Exception) { "响应解析失败" }
    }

    /** 清除 session */
    fun logout() {
        cookieJar.clear()
    }
}
