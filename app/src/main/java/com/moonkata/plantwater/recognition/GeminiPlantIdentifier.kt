package com.moonkata.plantwater.recognition

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.moonkata.plantwater.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// AI Studio 무료 티어 Gemini Flash로 식물 식별. API 키는 로컬 property(local.properties)에서만 읽어서
// 커밋에 올라가지 않게 함 (build.gradle.kts -> BuildConfig.GEMINI_API_KEY)
object GeminiPlantIdentifier {

    private const val TAG = "GeminiPlantIdentifier"
    // 고정 버전(gemini-2.5-flash)은 신규 API 키에서 404(단종)로 막혀있어서,
    // 항상 최신 flash 모델을 가리키는 별칭으로 교체
    private const val MODEL = "gemini-flash-latest"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val PROMPT = """
        사진 속 식물을 식별해서 아래 JSON 형식으로만 답해줘. 다른 텍스트나 마크다운은 포함하지 마.
        {
          "commonName": "한국어 일반 이름",
          "scientificName": "학명",
          "wateringIntervalDays": 물주기 권장 간격(1~30 사이 정수, 일 단위),
          "wateringAdvice": "물주기 관련 한국어 조언 한 문장",
          "lightAdvice": "광량 관련 한국어 조언 한 문장"
        }
        사진에 식물이 없거나 식별할 수 없으면 commonName을 빈 문자열로 줘.
    """.trimIndent()

    suspend fun identify(bitmap: Bitmap): Result<PlantIdentification> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Gemini 인식 요청 시작 (model=$MODEL)")
        runCatching {
            val apiKey = BuildConfig.GEMINI_API_KEY
            check(apiKey.isNotBlank()) { "GEMINI_API_KEY가 설정되지 않았습니다 (local.properties 확인)" }

            val requestBody = buildRequestBody(bitmap.toBase64Jpeg())
            val connection = openConnection(apiKey)

            connection.outputStream.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(requestBody.toString()) }
            }

            val responseCode = connection.responseCode
            val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader()
                .use { it.readText() }

            Log.d(TAG, "Gemini 응답 코드: $responseCode")
            check(responseCode in 200..299) { "Gemini API 오류 ($responseCode): $responseText" }

            parseIdentification(responseText)
        }.onSuccess { result ->
            Log.d(TAG, "인식 성공: ${result.commonName} (${result.scientificName}), 물주기 ${result.wateringIntervalDays}일")
        }.onFailure { e ->
            Log.e(TAG, "인식 실패: ${e.message}", e)
        }
    }

    private fun openConnection(apiKey: String): HttpURLConnection {
        // 연결 자체가 안 되는 네트워크에서는 안드로이드 기본 okhttp가 여러 IP를 순서대로 재시도하며
        // 매번 connectTimeout만큼 기다리므로, 값을 낮춰서 폴백까지 걸리는 총 시간을 줄임
        return (URL("$ENDPOINT?key=$apiKey").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 6_000
            readTimeout = 12_000
        }
    }

    private fun Bitmap.toBase64Jpeg(): String {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildRequestBody(base64Image: String): JSONObject {
        val textPart = JSONObject().put("text", PROMPT)
        val imagePart = JSONObject().put(
            "inline_data",
            JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", base64Image)
        )
        val content = JSONObject().put("parts", JSONArray().put(textPart).put(imagePart))
        val generationConfig = JSONObject().put("responseMimeType", "application/json")

        return JSONObject()
            .put("contents", JSONArray().put(content))
            .put("generationConfig", generationConfig)
    }

    private fun parseIdentification(responseJson: String): PlantIdentification {
        val text = JSONObject(responseJson)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val json = JSONObject(text)
        val commonName = json.optString("commonName")
        check(commonName.isNotBlank()) { "식물을 인식하지 못했습니다" }

        return PlantIdentification(
            commonName = commonName,
            scientificName = json.optString("scientificName"),
            wateringIntervalDays = json.optInt("wateringIntervalDays", 7).coerceIn(1, 30),
            wateringAdvice = json.optString("wateringAdvice"),
            lightAdvice = json.optString("lightAdvice")
        )
    }
}
