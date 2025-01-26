package org.carstenf.wordfinder;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

public class GermanWordDefinitionLookupService implements WordDefinitionLookupService {

    private static final String WIKTIONARY_API_URL = "https://de.wiktionary.org/w/api.php?action=query&prop=extracts&explaintext=true&format=json&titles=";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public void lookupWordDefinition(WordFinder wordFinderApp, String word) {
        String lowercaseWord= word.toLowerCase();
        String capitalizedWord = Character.toUpperCase(lowercaseWord.charAt(0)) + lowercaseWord.substring(1);
        // capitalizedWord = "fasern|Fasern";

        WiktionaryLookup wiktionaryLookup = new WiktionaryLookup();
        wiktionaryLookup.getMeaningAsync( capitalizedWord, meaning -> {
            if (meaning != null) {
                wordFinderApp.displayWordDefinition(meaning);
            } else {
                wordFinderApp.displayToast("Definition not found for: " + word, Toast.LENGTH_SHORT);
            }
        });

//        getMeaning(wordFinderApp, word);
    }

    public static void getMeaning(@NonNull WordFinder wordFinderApp, @NonNull String word) {
        if(word.isEmpty()) return;

        String lowercaseWord= "fasern"; // word.toLowerCase();
        String capitalizedWord = Character.toUpperCase(lowercaseWord.charAt(0)) + lowercaseWord.substring(1);

        String searchTerm = capitalizedWord + "|" + lowercaseWord  ;

        String url = WIKTIONARY_API_URL + searchTerm;

        Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        wordFinderApp.displayToast("Definition lookup for " + word + " failed with empty response.", Toast.LENGTH_SHORT);
                        return;
                    }

                    String responseBodyStr = responseBody.string();
                    Log.d("API Response", responseBodyStr);
                    try {
                        String definitionStr = parseMeaningFromJson(responseBodyStr);
                        wordFinderApp.displayWordDefinition(definitionStr);
                    } catch (Exception e) {
                        wordFinderApp.displayToast("Error looking up " + word + ": " + e.getMessage(), Toast.LENGTH_SHORT);
                    }
                } else {
                    wordFinderApp.displayToast("Definition not found for: " + word, Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(WordFinder.TAG, e.getMessage(), e);
                wordFinderApp.displayToast("Error looking up word: " + e.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    private static String parseMeaningFromJson(String jsonResponse) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode pagesNode = rootNode.path("query").path("pages");

        // Iterate through the pages (usually only one page for a word)
        for (JsonNode pageNode : pagesNode) {
            if (pageNode.has("extract")) {
                return pageNode.path("extract").asText();
            }
        }

        return null;
    }
}
