package org.carstenf.wordfinder

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
//
// Created by DeepSeek (https://chat.deepseek.com/)
//
class WiktionaryLookup {

    private val client = OkHttpClient()
    private val objectMapper = ObjectMapper()
//    private val baseUrl = "https://de.wiktionary.org/w/api.php"
    private val WIKTIONARY_API_URL: String = "https://de.wiktionary.org/w/api.php?action=query&prop=extracts&explaintext=true&format=json&titles="
    // private val WIKTIONARY_API_URI: String = "https://de.wiktionary.org/w/api.php?action=query&prop=extracts&explaintext=true&format=json&titles=fasern|Fasern";

    // Function to fetch the meaning of a word (callable from Java)
    fun getMeaningAsync(word: String, callback: WiktionaryCallback) {
        CoroutineScope(Dispatchers.Main).launch {
            val meaning = withContext(Dispatchers.IO) {
                fetchMeaning(word)
            }
            callback.onResult(meaning)
        }
    }

    // Internal function to fetch and concatenate the meaning
    private fun fetchMeaning(word: String): String? {
        var continueParams: Map<String, String> = emptyMap()
        val fullMeaning = StringBuilder()
        var shouldContinue = true

        while (shouldContinue) {
            val url = buildUrl(word, continueParams)
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code: ${response.code}")
                }

                val jsonResponse = response.body?.string()
                if (jsonResponse != null) {
                    val (meaning, newContinueParams) = parseResponse(jsonResponse)
                    if (meaning != null) {
                        fullMeaning.append(meaning)
                    }

                    if (newContinueParams.isEmpty()) {
                        shouldContinue = false // Stop the loop
                    } else {
                        continueParams = newContinueParams
                    }
                }
            }
        }

        return if (fullMeaning.isNotEmpty()) fullMeaning.toString() else null
    }

    // Build the API URL with continue parameters
    private fun buildUrl(word: String, continueParams: Map<String, String>): String {
        var url = "$WIKTIONARY_API_URL$word"
        if (continueParams.isNotEmpty()) {
            url += "&continue=${continueParams["continue"]}&excontinue=${continueParams["excontinue"]}"
        }
        return url
    }

    // Parse the API response and extract the meaning and continue parameters
    private fun parseResponse(jsonResponse: String): Pair<String?, Map<String, String>> {
        val rootNode = objectMapper.readTree(jsonResponse)
        val pagesNode = rootNode.path("query").path("pages")

        var meaning: String? = null
        for (pageNode in pagesNode) {
            if (pageNode.has("extract")) {
                meaning = pageNode.path("extract").asText()
            }
        }

        val continueParams = mutableMapOf<String, String>()
        if (rootNode.has("continue")) {
            val continueNode = rootNode.path("continue")
            continueParams["continue"] = continueNode.path("continue").asText()
            continueParams["excontinue"] = continueNode.path("excontinue").asText()
        }

        return Pair(meaning, continueParams)
    }
}