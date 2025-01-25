package org.carstenf.wordfinder;

import android.util.Log;
import android.widget.Toast;

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
    @Override
    public void lookupWordDefinition(WordFinder wordFinderApp, String word) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/"+word)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API Response", e.getMessage(), e);
                wordFinderApp.displayToast("Error looking up word: " +e.getMessage(), Toast.LENGTH_SHORT);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        wordFinderApp.displayToast("Definition lookup for " +word+" failed with empty response.", Toast.LENGTH_SHORT);
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

                        wordFinderApp.displayWordDefinition(definitionStr);

                    } catch (Exception e) {
                        wordFinderApp.displayToast("Error looking up "+word+": " + e.getMessage(), Toast.LENGTH_SHORT);
                    }
                }
                else {
                    wordFinderApp.displayToast("Definition not found for: "+ word, Toast.LENGTH_SHORT);
                }
            }
        });
    }
}
