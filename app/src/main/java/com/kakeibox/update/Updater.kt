package com.kakeibox.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/** GitHub Releases の最新版の情報 */
data class ReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
    val size: Long,
)

/**
 * アプリ内更新：GitHub Releases の最新版を確認し、APK をダウンロードして Android のインストール画面を開く。
 * 接続先は GitHub（API と APK のダウンロード）だけ。
 */
object Updater {
    const val REPO = "n1m4-org/Kakeibox"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases/latest"
    private const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"
    private val ASSET_NAME = Regex("""kakeibox-(\d+)\.apk""")

    /** 最新リリースの JSON から、APK（kakeibox-<versionCode>.apk）の情報を取り出す。APK がなければ null */
    fun parseLatestRelease(json: String): ReleaseInfo? = runCatching {
        val o = JSONObject(json)
        val assets = o.optJSONArray("assets") ?: return null
        (0 until assets.length()).map { assets.getJSONObject(it) }.firstNotNullOfOrNull { a ->
            val code = ASSET_NAME.matchEntire(a.optString("name"))?.groupValues?.get(1)?.toIntOrNull() ?: return@firstNotNullOfOrNull null
            ReleaseInfo(
                versionCode = code,
                versionName = o.optString("tag_name").removePrefix("v").ifBlank { code.toString() },
                apkUrl = a.getString("browser_download_url"),
                notes = o.optString("body").trim(),
                size = a.optLong("size"),
            )
        }
    }.getOrNull()

    /** 今の版より新しいか */
    fun isNewer(info: ReleaseInfo, currentVersionCode: Int): Boolean = info.versionCode > currentVersionCode

    /** 最新リリースを取得する。リリースがない・通信できないときは例外 */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val conn = (URL(API_LATEST).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Kakeibox-Android")
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        try {
            when (conn.responseCode) {
                200 -> parseLatestRelease(conn.inputStream.bufferedReader().use { it.readText() })
                404 -> null // まだリリースがない
                else -> error("HTTP ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }

    /** APK をアプリのキャッシュにダウンロードする */
    suspend fun download(context: Context, info: ReleaseInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // 古いものは消す
        val file = File(dir, "kakeibox-${info.versionCode}.apk")
        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Kakeibox-Android")
            connectTimeout = 10_000
            readTimeout = 30_000
        }
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.size
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        file
    }

    /** このアプリからのインストールが許可されているか（Android 8 以降は初回に許可が必要） */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** 「不明なアプリのインストール」の設定画面を開く */
    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Android のインストール（更新）画面を開く */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** リリースのページをブラウザで開く */
    fun openReleasesPage(context: Context) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
