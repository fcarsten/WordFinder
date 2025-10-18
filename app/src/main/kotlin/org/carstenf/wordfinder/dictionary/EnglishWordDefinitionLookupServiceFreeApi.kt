package org.carstenf.wordfinder.dictionary

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class EnglishWordDefinitionLookupServiceFreeApi : WordDefinitionLookupService {

    override val language: String = "E" // NON-NLS

    override fun lookupWordDefinition(lookupManager: WordDefinitionLookupManager, task: WordLookupTask) {
        val url = "$FREEDICTIONARYAPI_URL/en/${task.word.lemma.lowercase()}"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API Response", e.message ?: "Unknown error", e) // NON-NLS
                lookupManager.processWordLookupError(
                    task, language, "Error looking up word: ${e.message}"
                )
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    lookupManager.processWordLookupResult(
                        task,
                        WordInfo(
                            task.word.displayText,
                            language,
                            null,
                            "https://en.wiktionary.org/w/index.php?title=Special:Search&search=${task.word.displayText}"
                        )
                    )
                    return
                }

                try {
                    val bodyStr = response.body.string()
                    Log.d("API Response", bodyStr) // NON-NLS

                    val json = JSONObject(bodyStr)
                    val entries = json.optJSONArray("entries") // NON-NLS
                    if (entries == null || entries.length() == 0) {
                        lookupManager.processWordLookupResult(
                            task,
                            WordInfo(
                                task.word.displayText,
                                language,
                                null,
                                "https://en.wiktionary.org/w/index.php?title=Special:Search&search=${task.word.displayText}"
                            )
                        )
                        return
                    }

                    // Take the first entry
                    val entry = entries.getJSONObject(0)
                    val partOfSpeech = entry.optString("partOfSpeech", "unknown") // NON-NLS

                    val senses = entry.optJSONArray("senses") // NON-NLS
                    var definition = ""
                    if (senses != null && senses.length() > 0) {
                        definition = senses.getJSONObject(0).optString("definition", "") // NON-NLS
                    }

                    val definitionStr =
                        "${task.word.displayText} ($partOfSpeech): $definition"

                    val sourceObj = json.optJSONObject("source") // NON-NLS
                    val wiktionaryUrl =
                        sourceObj?.optString("url") // NON-NLS
                            ?: "https://en.wiktionary.org/w/index.php?title=Special:Search&search=${task.word.displayText}"

                    lookupManager.processWordLookupResult(
                        task,
                        WordInfo(
                            task.word.displayText,
                            language,
                            definitionStr,
                            wiktionaryUrl
                        )
                    )
                } catch (e: Exception) {
                    lookupManager.processWordLookupError(
                        task, language,
                        "Error parsing ${task.word.displayText}: ${e.message}"
                    )
                }
            }
        })
    }

    companion object {
        private val client = OkHttpClient()
        private const val FREEDICTIONARYAPI_URL = "https://freedictionaryapi.com/api/v1/entries" // NON-NLS
    }
}
