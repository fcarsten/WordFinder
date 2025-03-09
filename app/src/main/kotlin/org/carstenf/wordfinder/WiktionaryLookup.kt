/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
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
// Created with help from DeepSeek (https://chat.deepseek.com/)
//
const val WIKTIONARY_API_URL: String = "https://de.wiktionary.org/w/api.php?action=query&prop=extracts&explaintext=true&format=json&titles="

class WiktionaryLookup {

    private val client = OkHttpClient()
    private val objectMapper = ObjectMapper()

    // Function to fetch the meaning of a word (callable from Java)
    fun getMeaningAsync(word: String, callback: WiktionaryCallback) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val meaning = withContext(Dispatchers.IO) {
                    fetchMeaning(word)
                }
                callback.onResult(meaning?.let { extractMeanings(it) })
            } catch (e: IOException) {
                callback.onError(e)
            }
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
            try {
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
            } catch (e: IOException) {
                throw IOException("Error fetching meaning: ${e.message}: ${e.message}")
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

    /**
    * Extracts the actual word meanings from the Wiktionary API response.
    * The meanings start after a line that begins with "Bedeutungen:".
    * The meanings are lines that start with '[' and end before the first line that does not start with '['.
    */
    private fun extractMeanings(responseText: String): String {
        val lines = responseText.split("\n")
        val meanings = mutableListOf<String>()
        var isMeaningSection = false
        var isMerkmaleSection = false
        var preamble = true

        for (line in lines) {
            if (line.startsWith("Bedeutungen:")) {
                isMeaningSection = true
                continue // Skip the "Bedeutungen:" line
            }

            if (isMeaningSection) {
                if (preamble || line.startsWith("[")) { // Sometimes there seems to be
                    // additional lines before the first "[...." line
                    //
                    if(line.startsWith("["))
                        preamble = false
                    meanings.add(line)
                } else {
                    // Stop when we encounter a line that does not start with '['
                    break
                }
            }

            if(isMerkmaleSection) {
                meanings.add(line)
                isMerkmaleSection = false // Can't tell end of section reliably so only taking the first line
            }

            if(line.startsWith("Grammatische Merkmale:")) {
                isMerkmaleSection = true
            }

        }

        return meanings.joinToString("\n") // Combine meanings into a single string
    }
}