package com.kakeibox

import com.kakeibox.update.ReleaseInfo
import com.kakeibox.update.Updater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    private val json = """
        {
          "tag_name": "v1.0.123",
          "body": "- ウィジェットを小さく\n- 不具合の修正",
          "assets": [
            {"name": "notes.txt", "browser_download_url": "https://example.com/notes.txt", "size": 10},
            {"name": "kakeibox-123.apk", "browser_download_url": "https://github.com/n1m4-org/Kakeibox/releases/download/v1.0.123/kakeibox-123.apk", "size": 3500000}
          ]
        }
    """.trimIndent()

    @Test
    fun parsesApkAssetAndVersion() {
        val info = Updater.parseLatestRelease(json)!!
        assertEquals(123, info.versionCode)
        assertEquals("1.0.123", info.versionName)
        assertTrue(info.apkUrl.endsWith("/kakeibox-123.apk"))
        assertEquals(3_500_000L, info.size)
        assertTrue(info.notes.startsWith("- ウィジェット"))
    }

    @Test
    fun noApkMeansNull() {
        assertNull(Updater.parseLatestRelease("""{"tag_name":"v1","assets":[{"name":"x.zip","browser_download_url":"u"}]}"""))
        assertNull(Updater.parseLatestRelease("not json"))
    }

    @Test
    fun newerComparesVersionCode() {
        val info = ReleaseInfo(123, "1.0.123", "u", "", 0)
        assertTrue(Updater.isNewer(info, 122))
        assertFalse(Updater.isNewer(info, 123))
        assertFalse(Updater.isNewer(info, 200))
    }
}
