package com.example.data.api

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "GeminiApi"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) {
            ""
        } else {
            key
        }
    }

    /**
     * Helper to make a raw POST request to Gemini v1beta API
     */
    private suspend fun callGemini(
        systemInstruction: String?,
        prompt: String,
        mimeType: String? = null,
        imageBase64: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "APIKEY_MISSING"
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val requestJson = JSONObject()

            // System Instruction configuration
            if (!systemInstruction.isNullOrEmpty()) {
                val sysPart = JSONObject().put("text", systemInstruction)
                val sysContent = JSONObject().put("parts", JSONArray().put(sysPart))
                requestJson.put("systemInstruction", sysContent)
            }

            // Chat content parts
            val partsArr = JSONArray()
            partsArr.put(JSONObject().put("text", prompt))

            if (!mimeType.isNullOrEmpty() && !imageBase64.isNullOrEmpty()) {
                val inlineData = JSONObject().apply {
                    put("mimeType", mimeType)
                    put("data", imageBase64)
                }
                partsArr.put(JSONObject().put("inlineData", inlineData))
            }

            val contentsArr = JSONArray().put(
                JSONObject().put("parts", partsArr)
            )
            requestJson.put("contents", contentsArr)

            // Request Body
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini call failed: Code ${response.code}, Body: $errBody")
                    return@withContext "Error: HTTP ${response.code} - ${response.message}"
                }

                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                    }
                }
                
                return@withContext "No response from AI."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini Call", e)
            return@withContext "Error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    /**
     * Module 5: Summarization. Types: "chapter", "topic", "bullet", "exam"
     */
    suspend fun generateSummary(noteTitle: String, noteText: String, typeName: String): String {
        val sysInstruction = "You are an expert AI Academic Tutor. Provide an educational summary of the study material provided. Ensure it is written in a beautiful, structured format with clear headings, bullet points, and key definitions. Supports bilingual English and Bengali responses based on notes content."
        
        val prompt = """
            Please summarize this Note: "$noteTitle".
            Format requested: "$typeName Summary".
            
            Guidelines:
            - If "Chapter Summary": provide a cohesive, structured chapter overview, explaining the logical flow and key concepts.
            - If "Topic Summary": extract and define the 5-7 most important academic topics detailed in this note.
            - If "Bullet Point Summary": outline all major and minor ideas in a neat bulleted list.
            - If "Exam-Oriented Summary": highlight the exact terms, potential exam questions, standard formulas, and crucial definitions likely to be graded.
            
            Note Content:
            $noteText
        """.trimIndent()

        return callGemini(sysInstruction, prompt)
    }

    /**
     * Module 6: Chat with Notes. Must answer strictly from provided context with source citation.
     */
    suspend fun chatWithNotes(
        noteTitle: String,
        noteContent: String,
        userQuestion: String,
        chatHistoryJson: String // Serialized array of user/assistant messages
    ): String {
        val sysInstruction = """
            You are "AI Study Assistant", a strict academic RAG (Retrieval-Augmented Generation) bot.
            Your task is to answer the student's question based ONLY on the study note provided.
            
            CRITICAL RULES:
            1. Rely ONLY on the Note Content below to answer. If the answer cannot be found in the note, say: "I cannot find the answer to this question in the uploaded notes. Please add more materials!"
            2. PROVIDE PAGE OR LOCATION CITATION. Since this is loaded via a digital note, cite the source name (e.g. Note "$noteTitle") and estimate the relative position or virtual page (e.g. "Source: $noteTitle | Page 1" or similar relative section) dynamically at the end of your answer in a separate "Source Citation" line.
            3. If the note contains Bengali, answer in Bengali. If English, answer in English. Support bilingual input/output naturally.
            4. Keep answers accurate, factual, and strictly relevant to the document content. No external assumptions.
        """.trimIndent()

        // We can parse history and build a clear context string
        val historyContextBuilder = StringBuilder()
        try {
            if (chatHistoryJson.isNotEmpty()) {
                val arr = JSONArray(chatHistoryJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val sender = obj.getString("sender")
                    val message = obj.getString("message")
                    historyContextBuilder.append("$sender: $message\n")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building chat history context", e)
        }

        val prompt = """
            --- UPLOADED NOTE DETAIL ---
            Title: $noteTitle
            Content:
            $noteContent
            
            --- CHAT CONVERSATION HISTORY ---
            $historyContextBuilder
            
            --- CURRENT STUDENT QUESTION ---
            Student: $userQuestion
            
            AI Response:
        """.trimIndent()

        return callGemini(sysInstruction, prompt)
    }

    /**
     * Module 7: Quiz Generator. Returns structured JSON containing MCQs, True/False, and Short Questions.
     */
    suspend fun generateQuiz(noteTitle: String, noteContent: String): String {
        val sysInstruction = """
            You are an advanced Exam Inspector and Quiz Master.
            Analyze the provided study note and generate a robust practice quiz in valid raw JSON format only.
            
            Your JSON model MUST exactly follow this schema structure:
            {
              "title": "Quiz Title based on note",
              "mcqs": [
                {
                  "question": "Clear, direct multiple-choice question",
                  "options": ["A. choice 1", "B. choice 2", "C. choice 3", "D. choice 4"],
                  "correctAnswer": "A",
                  "explanation": "Brief explanation of why this choice is correct."
                }
              ],
              "trueFalse": [
                {
                  "question": "True or False statement",
                  "correctAnswer": "True",
                  "explanation": "Why this statement is true or false."
                }
              ],
              "shortQuestions": [
                {
                  "question": "Conceptual direct short question",
                  "sampleAnswer": "Comprehensive model answer text.",
                  "rubric": "What points key elements must contain."
                }
              ]
            }

            CRITICAL: Returning a valid, parsable JSON structure is required. Do not enclose it in markdown blocks! Just return the JSON string itself. If there's some issue, fallback to returning valid JSON. Ensure Bengali is supported if the note is in Bengali. Generate at least 3 MCQs, 2 True/False, and 2 Short Questions.
        """.trimIndent()

        val prompt = """
            Please generate a quiz for the note: "$noteTitle"
            
            Note Content:
            $noteContent
        """.trimIndent()

        val rawResponse = callGemini(sysInstruction, prompt)
        
        // Let's strip potential ```json markdown blocks if the AI outputs them despite instructions
        return cleanJsonString(rawResponse)
    }

    /**
     * Module 8: Flashcard Generator.
     */
    suspend fun generateFlashcards(noteTitle: String, noteContent: String): String {
        val sysInstruction = """
            You are a memory retention coach. Organize core topics from this note into practical flashcards.
            Return a raw JSON structure representing flashcards inside an array of objects.
            
            Schema:
            [
              {
                "question": "Concise term, question, or key word?",
                "answer": "Instant answer, definition, or translation."
              }
            ]
            
            CRITICAL: Return only the JSON array of flashcards, no formatting code tags like ```json. Include at least 5 key flashcards.
        """.trimIndent()

        val prompt = """
            Generate flashcards for: "$noteTitle"
            Note content:
            $noteContent
        """.trimIndent()

        val rawResponse = callGemini(sysInstruction, prompt)
        return cleanJsonString(rawResponse)
    }

    /**
     * Module 9: Study Planner. Generates day-by-day plan in JSON.
     */
    suspend fun generateStudyPlan(subject: String, examDate: String, hours: Float): String {
        val sysInstruction = """
            You are a highly organized academic study scheduler.
            Generate a personalized day-by-day study roadmap leading up to the exam date.
            Return a raw JSON structure matching this schema:
            {
              "subject": "Subject name",
              "examCountdownDays": 8,
              "schedule": [
                {
                  "day": "Day 1",
                  "focusTopic": "Main topic to cover today",
                  "allocatedHours": 2.5,
                  "activities": ["Read Chapter 1 notes", "Do practice MCQs 1-10"],
                  "status": "Pending"
                }
              ]
            }
            Generate sequential daily tasks starting from today (June 11, 2026) up to the specified exam date: $examDate. Space out topic work, active recall, question practice, and final mock revision.
            CRITICAL: Return ONLY raw JSON, do not wrap in markdown boxes.
        """.trimIndent()

        val prompt = """
            Generate a custom study plan.
            Subject: $subject
            Exam Date: $examDate
            Study Hours per Day: $hours hours
        """.trimIndent()

        val rawResponse = callGemini(sysInstruction, prompt)
        return cleanJsonString(rawResponse)
    }

    /**
     * Module 4: OCR System. Performs multimodal text extraction from image bytes.
     */
    suspend fun performOcr(mimeType: String, imageBase64: String): String {
        val sysInstruction = """
            You are a world-class Optical Character Recognition (OCR) scanner.
            Your task is to extract all printed and handwritten text perfectly from the provided image.
            
            RULES:
            1. Transcribe English and Bengali text precisely. Preserve formatting, line breaks, bullet points, and equations.
            2. Do NOT add commentary, do not explain what is in the image, and do not summarize. Only output the extracted text. If no text exists, say "No readable text detected."
        """.trimIndent()

        val prompt = "Perform OCR on this study note. Extract all text verbatim."

        return callGemini(sysInstruction, prompt, mimeType, imageBase64)
    }

    /**
     * Module 11: Research Paper Assistant.
     */
    suspend fun analyzeResearchPaper(paperTitle: String, paperContent: String): String {
        val sysInstruction = """
            You are an Academic Research Assistant. Analyze the uploaded scholarly paper and extract key structural metrics.
            Return a beautifully styled academic summary containing exactly:
            1. Study Objective / Research Goal: State what problem the authors set out to solve.
            2. Methodology: Summarize the experimental setup, variables, and datasets.
            3. Key Findings & Results: Highlight major statistical insights, theorems, or data points.
            4. Limitations & Future Work: Highlight drawbacks or future avenues stated.
            5. Auto-Generated Citation Format:
               - APA Format
               - MLA Format
               - IEEE Format
        """.trimIndent()

        val prompt = """
            Analyze research paper: "$paperTitle"
            
            Paper text:
            $paperContent
        """.trimIndent()

        return callGemini(sysInstruction, prompt)
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```")
            }
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```")
            }
        }
        return clean.trim()
    }
}
