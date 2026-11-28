package com.kakeibox.receipt

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.InternalServerException
import com.anthropic.errors.OverloadedException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate

/**
 * レシートの画像（と端末で読み取った文字）を Claude に送り、カテゴリごとの金額にしてもらう。
 *
 * @param apiKey ユーザーの Anthropic API キー
 * @param baseUrl テスト用（通常は null で Anthropic の API）
 */
class ReceiptReader(apiKey: String, baseUrl: String? = null) {
    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .apply { if (baseUrl != null) baseUrl(baseUrl) }
        .timeout(Duration.ofSeconds(120))
        .maxRetries(2)
        .build()

    /**
     * @param jpegBase64 縮小したレシート画像（JPEG の base64）
     * @param ocrText 端末の文字認識の結果（使えなければ null）
     * @param categories 支出カテゴリの選択肢（先頭から並び順）
     * @param fallbackKey 当てはまらないときのカテゴリ（その他）
     */
    suspend fun read(
        jpegBase64: String,
        ocrText: String?,
        categories: List<CategoryOption>,
        fallbackKey: String,
        today: LocalDate = LocalDate.now(),
    ): ReceiptDraft = withContext(Dispatchers.IO) {
        val schema = JsonOutputFormat.Schema.builder().apply {
            ReceiptPrompt.schema(categories).forEach { (k, v) -> putAdditionalProperty(k, JsonValue.from(v)) }
        }.build()

        val params = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(16000L)
            .system(ReceiptPrompt.system(categories, today))
            .outputConfig(
                OutputConfig.builder()
                    // 読み取りと仕分けだけなので軽めに（思考は常にオン）
                    .effort(OutputConfig.Effort.LOW)
                    .format(JsonOutputFormat.builder().schema(schema).build())
                    .build(),
            )
            .addUserMessageOfContentBlockParams(
                listOf(
                    ContentBlockParam.ofImage(
                        Base64ImageSource.builder()
                            .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                            .data(jpegBase64)
                            .build(),
                    ),
                    ContentBlockParam.ofText(ReceiptPrompt.userText(ocrText)),
                ),
            )
            // 安全上の理由で断られたときは、サーバー側で別のモデルが引き継ぐ
            .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .build()

        val message = try {
            client.messages().create(params)
        } catch (e: UnauthorizedException) {
            throw ReceiptException("API キーが正しくありません。設定で確認してください", e)
        } catch (e: PermissionDeniedException) {
            throw ReceiptException("この API キーでは利用できません（権限がありません）", e)
        } catch (e: RateLimitException) {
            throw ReceiptException("利用の上限に達しました。しばらくしてから試してください", e)
        } catch (e: OverloadedException) {
            throw ReceiptException("AI が混み合っています。しばらくしてから試してください", e)
        } catch (e: InternalServerException) {
            throw ReceiptException("AI 側でエラーが起きました。しばらくしてから試してください", e)
        } catch (e: BadRequestException) {
            val detail = e.apiMessage().orElse("")
            if (detail.contains("credit", ignoreCase = true)) {
                throw ReceiptException("API の残高が不足しています（Anthropic のコンソールで確認してください）", e)
            }
            throw ReceiptException("読み取りを依頼できませんでした（$detail）", e)
        } catch (e: AnthropicServiceException) {
            throw ReceiptException("読み取りに失敗しました（エラー ${e.statusCode()}）", e)
        } catch (e: AnthropicIoException) {
            throw ReceiptException("通信できませんでした。インターネット接続を確認してください", e)
        }

        when (message.stopReason().orElse(null)) {
            StopReason.REFUSAL -> throw ReceiptException("この画像は読み取れませんでした（AI が処理を断りました）")
            StopReason.MAX_TOKENS -> throw ReceiptException("読み取り結果が長すぎて途中で切れました。もう一度試してください")
            else -> Unit
        }
        val text = message.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
        if (text.isBlank()) throw ReceiptException("読み取り結果が空でした。もう一度試してください")
        try {
            ReceiptPrompt.parse(text, today, categories.map { it.key }.toSet(), fallbackKey)
        } catch (e: org.json.JSONException) {
            throw ReceiptException("読み取り結果を解釈できませんでした。もう一度試してください", e)
        }
    }

    companion object {
        const val MODEL = "claude-opus-5-5"
        const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
    }
}
