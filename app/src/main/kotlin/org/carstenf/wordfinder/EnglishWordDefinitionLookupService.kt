package org.carstenf.wordfinder

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

class EnglishWordDefinitionLookupService : WordDefinitionLookupService {
    override fun lookupWordDefinition(gameState: GameState, word: String) {
        val request: Request = Builder()
            .url(DICTIONARYAPI_URL + word)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API Response", e.message, e)
                gameState.processWordLookupError(
                    word,
                    language,
                    "Error looking up word: " + e.message
                )
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body
                    if (responseBody == null) {
                        gameState.processWordLookupError(
                            word, language,
                            "Definition lookup for $word failed with empty response."
                        )
                        return
                    }

                    val responseBodyStr = responseBody.string()
                    Log.d("API Response", responseBodyStr)

                    try {
                        val jsonObject = JSONArray(responseBodyStr).getJSONObject(0)
                        val meaningArray = jsonObject.getJSONArray("meanings")
                        val meaning = meaningArray.getJSONObject(0)

                        val partOfSpeech = meaning.getString("partOfSpeech")

                        val definitionArray = meaning.getJSONArray("definitions")
                        val definitionObj = definitionArray.getJSONObject(0)

                        val definition = definitionObj.getString("definition")

                        val definitionStr = "$word ($partOfSpeech): $definition"

                        gameState.processWordLookupResult(
                            WordInfo(
                                word,
                                language, definitionStr, null
                            )
                        )
                    } catch (e: Exception) {
                        gameState.processWordLookupError(
                            word, language,
                            "Error looking up " + word + ": " + e.message
                        )
                    }
                } else {
                    gameState.processWordLookupError(
                        word, language,
                        "Definition not found for: $word"
                    )
                }
            }
        })
    }

    override val language: String = "E"

    companion object {
        private val client = OkHttpClient()
        const val DICTIONARYAPI_URL: String = "https://api.dictionaryapi.dev/api/v2/entries/en/"
    }
}
