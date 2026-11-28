package com.kakeibox

import com.kakeibox.receipt.CategoryOption
import com.kakeibox.receipt.ReceiptException
import com.kakeibox.receipt.ReceiptReader
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** 実際の API は呼ばず、ローカルのサーバーで送信内容と応答の扱いを確かめる */
class ReceiptReaderTest {
    private lateinit var server: MockWebServer
    private val cats = listOf(CategoryOption("FOOD", "食費"), CategoryOption("OTHER_EXPENSE", "その他"))
    private val today = LocalDate.of(2026, 11, 28)

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() { server.shutdown() }

    private fun reader() = ReceiptReader("sk-ant-test", server.url("/").toString().trimEnd('/'))

    private fun message(text: String, stop: String = "end_turn") = JSONObject(
        mapOf(
            "id" to "msg_1", "type" to "message", "role" to "assistant", "model" to "claude-opus-5-5",
            "content" to listOf(mapOf("type" to "text", "text" to text)),
            "stop_reason" to stop, "stop_sequence" to null,
            "usage" to mapOf("input_tokens" to 10, "output_tokens" to 20),
        ),
    ).toString()

    @Test
    fun sendsImageSchemaAndFallback_andParsesResult() = runBlocking {
        val body = """{"store":"店","date":"2026-11-27","payment":"CASH","total":500,"groups":[{"category":"FOOD","amount":500,"items":["パン"]}]}"""
        server.enqueue(MockResponse().setBody(message(body)).addHeader("content-type", "application/json"))

        val draft = reader().read("QUJD", "パン 500", cats, "OTHER_EXPENSE", today)
        assertEquals(500L, draft.total)
        assertEquals("FOOD", draft.lines.single().categoryKey)

        val req = server.takeRequest()
        assertEquals("/v1/messages", req.path)
        // SDK の版によって x-api-key か Authorization: Bearer で送る
        assertTrue(req.getHeader("x-api-key") == "sk-ant-test" || req.getHeader("Authorization") == "Bearer sk-ant-test")
        assertTrue(req.getHeader("anthropic-beta")!!.contains(ReceiptReader.FALLBACK_BETA))
        val sent = JSONObject(req.body.readUtf8())
        assertEquals("claude-opus-5-5", sent.getString("model"))
        assertEquals("default", sent.getString("fallbacks"))
        assertEquals("low", sent.getJSONObject("output_config").getString("effort"))
        val format = sent.getJSONObject("output_config").getJSONObject("format")
        assertEquals("json_schema", format.getString("type"))
        assertEquals("object", format.getJSONObject("schema").getString("type"))
        val content = sent.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        assertEquals("image", content.getJSONObject(0).getString("type"))
        assertEquals("QUJD", content.getJSONObject(0).getJSONObject("source").getString("data"))
        assertEquals("image/jpeg", content.getJSONObject(0).getJSONObject("source").getString("media_type"))
        assertTrue(content.getJSONObject(1).getString("text").contains("パン 500"))
        assertTrue(sent.getString("system").contains("- FOOD: 食費"))
    }

    @Test
    fun unauthorized_givesKeyMessage() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401).addHeader("content-type", "application/json")
                .setBody("""{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""),
        )
        try {
            reader().read("QUJD", null, cats, "OTHER_EXPENSE", today)
            fail()
        } catch (e: ReceiptException) {
            assertTrue(e.message!!.contains("API キー"))
        }
    }

    @Test
    fun refusal_givesMessage() = runBlocking {
        server.enqueue(MockResponse().setBody(message("", "refusal")).addHeader("content-type", "application/json"))
        try {
            reader().read("QUJD", null, cats, "OTHER_EXPENSE", today)
            fail()
        } catch (e: ReceiptException) {
            assertTrue(e.message!!.contains("断りました"))
        }
    }
}
