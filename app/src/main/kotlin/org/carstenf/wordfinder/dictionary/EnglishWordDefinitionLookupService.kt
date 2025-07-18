package org.carstenf.wordfinder.dictionary

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

class EnglishWordDefinitionLookupService : WordDefinitionLookupService {
    override fun lookupWordDefinition(lookupManager: WordDefinitionLookupManager, task: WordLookupTask) {
        val request: Request = Request.Builder()
            .url(DICTIONARYAPI_URL + task.word.lemma)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API Response", e.message, e) // NON-NLS
                lookupManager.processWordLookupError(
                    task,
                    language,
                    "Error looking up word: " + e.message
                )
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body
                    if (responseBody == null) {
                        lookupManager.processWordLookupError(
                            task, language,
                            "Definition lookup for ${task.word.displayText} failed with empty response."
                        )
                        return
                    }

                    val responseBodyStr = responseBody.string()
                    Log.d("API Response", responseBodyStr) // NON-NLS

                    try {
                        val jsonObject = JSONArray(responseBodyStr).getJSONObject(0)
                        val meaningArray = jsonObject.getJSONArray("meanings") // NON-NLS
                        val meaning = meaningArray.getJSONObject(0)

                        val partOfSpeech = meaning.getString("partOfSpeech")

                        val definitionArray = meaning.getJSONArray("definitions") // NON-NLS
                        val definitionObj = definitionArray.getJSONObject(0)

                        val definition = definitionObj.getString("definition") // NON-NLS

                        val definitionStr = "${task.word.displayText} ($partOfSpeech): $definition"

                        lookupManager.processWordLookupResult(
                            task,
                            WordInfo(
                                task.word.displayText,
                                language, definitionStr,
                                "https://en.wiktionary.org/w/index.php?title=Special:Search&search=${task.word.displayText}"
                            )
                        )
                    } catch (e: Exception) {
                        lookupManager.processWordLookupError(
                            task, language,
                            "Error looking up ${task.word.displayText}: ${e.message}" // NON-NLS
                        )
                    }
                } else {
                    lookupManager.processWordLookupResult(
                        task,
                        WordInfo(
                            task.word.displayText,
                            language, null,
                            "https://en.wiktionary.org/w/index.php?title=Special:Search&search=${task.word.displayText}"
                        )
                    )
                }
            }
        })
    }

    override val language: String = "E" // NON-NLS

    companion object {
        private val client = OkHttpClient()
        const val DICTIONARYAPI_URL: String = "https://api.dictionaryapi.dev/api/v2/entries/en/"
    }
}