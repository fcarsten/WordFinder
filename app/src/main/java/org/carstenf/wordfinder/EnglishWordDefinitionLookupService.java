package org.carstenf.wordfinder;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class EnglishWordDefinitionLookupService implements WordDefinitionLookupService {
    private static final OkHttpClient client = new OkHttpClient();
    public static final String DICTIONARYAPI_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/";

    @Override
    public void lookupWordDefinition(GameState gameState, String word) {
        Request request = new Request.Builder()
                .url(DICTIONARYAPI_URL +word)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API Response", e.getMessage(), e);
                gameState.processWordLookupError(word, getLanguage(), "Error looking up word: " +e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        gameState.processWordLookupError(word, getLanguage(),
                                "Definition lookup for " +word+" failed with empty response.");
                        return;
                    }

                    String responseBodyStr = responseBody.string();
                    Log.d("API Response", responseBodyStr);

                    try {
                        JSONObject jsonObject = new JSONArray(responseBodyStr).getJSONObject(0);
                        JSONArray meaningArray = jsonObject.getJSONArray("meanings");
                        JSONObject meaning = meaningArray.getJSONObject(0);

                        String partOfSpeech = meaning.getString("partOfSpeech");

                        JSONArray definitionArray = meaning.getJSONArray("definitions");
                        JSONObject definitionObj = definitionArray.getJSONObject(0);

                        String definition = definitionObj.getString("definition");

                        String definitionStr = word +" ("+partOfSpeech + "): " + definition;

                        gameState.processWordLookupResult(new WordInfo(word, getLanguage(), definitionStr, null));

                    } catch (Exception e) {
                        gameState.processWordLookupError(word, getLanguage(),
                                "Error looking up "+word+": " + e.getMessage());
                    }
                }
                else {
                    gameState.processWordLookupError(word, getLanguage(),
                            "Definition not found for: "+ word);
                }
            }
        });
    }

    @Override
    public String getLanguage() {
        return "E";
    }
}
