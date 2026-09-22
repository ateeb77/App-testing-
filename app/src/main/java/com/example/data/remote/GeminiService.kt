package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.HazardType
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String? = null)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiRequest(val contents: List<GeminiContent>)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent?)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class AiAnalysisResult(
    val suggestedHazard: HazardType,
    val visualConsistency: String, // "HIGH", "MEDIUM", "LOW"
    val urgencyScore: Int,         // 1 to 10
    val explanation: String,
    val isAiGenerated: Boolean
)

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun analyzeReport(
        reportedHazard: HazardType,
        description: String,
        hasImage: Boolean
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Check if real API key is present
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    You are JAGRUK's AI Emergency Incident Verification Assistant.
                    Analyze this citizen hazard report:
                    Reported Hazard: ${reportedHazard.displayName}
                    Description: "$description"
                    Has Photo Evidence: $hasImage

                    Respond strictly in 3 lines:
                    CONSISTENCY: [HIGH | MEDIUM | LOW]
                    URGENCY: [1-10]
                    EXPLANATION: [1-2 concise, clear factual sentences analyzing the risk without 100% certainty claims]
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    )
                )

                val response = api.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!text.isNullOrBlank()) {
                    var consistency = "HIGH"
                    var urgency = 7
                    var explanation = "Description aligns with typical ${reportedHazard.displayName} hazard patterns."

                    text.lines().forEach { line ->
                        val trimmed = line.trim()
                        when {
                            trimmed.startsWith("CONSISTENCY:", ignoreCase = true) -> {
                                consistency = trimmed.substringAfter(":").trim().uppercase()
                            }
                            trimmed.startsWith("URGENCY:", ignoreCase = true) -> {
                                urgency = trimmed.substringAfter(":").trim().toIntOrNull() ?: 7
                            }
                            trimmed.startsWith("EXPLANATION:", ignoreCase = true) -> {
                                explanation = trimmed.substringAfter(":").trim()
                            }
                        }
                    }

                    return@withContext AiAnalysisResult(
                        suggestedHazard = reportedHazard,
                        visualConsistency = consistency,
                        urgencyScore = urgency,
                        explanation = explanation,
                        isAiGenerated = true
                    )
                }
            } catch (e: Exception) {
                // Graceful fallback to rule-based logic
            }
        }

        // Rule-based fallback engine (Guaranteed 100% uptime)
        val textLower = description.lowercase()
        val consistency = when {
            hasImage && description.length > 20 -> "HIGH"
            hasImage || description.length > 10 -> "MEDIUM"
            else -> "LOW"
        }

        val explanation = when (reportedHazard) {
            HazardType.FLASH_FLOOD -> {
                if (textLower.contains("road") || textLower.contains("water") || textLower.contains("overflow")) {
                    "Report indicates rapid water accumulation affecting transport pathways. Extreme caution advised near low-lying drains."
                } else {
                    "Surface water elevation reported. Local topography suggests drainage vulnerability."
                }
            }
            HazardType.FIRE -> "Rapid thermal event with visible smoke plume risk. Potential for localized containment breach."
            HazardType.DANGEROUS_WILDLIFE, HazardType.DANGEROUS_ANIMAL -> "Apex/wild animal movement detected in human settlement perimeter. High avoidance recommended."
            HazardType.ROAD_ACCIDENT -> "Vehicular obstruction causing severe route disruption and potential structural hazard."
            HazardType.STRUCTURAL_HAZARD -> "Compromised masonry/structural stability detected. Safe standoff distance required."
            HazardType.ELECTRICAL_HAZARD -> "High-voltage conductive hazard or downed cabling near pedestrian corridors."
            HazardType.HAZARDOUS_MATERIAL -> "Airborne or surface chemical contaminant risk. Immediate isolation perimeter recommended."
            HazardType.OTHER -> "Unclassified community hazard reported. Community corroboration requested."
        }

        AiAnalysisResult(
            suggestedHazard = reportedHazard,
            visualConsistency = consistency,
            urgencyScore = if (hasImage) 8 else 6,
            explanation = explanation,
            isAiGenerated = false
        )
    }
}
