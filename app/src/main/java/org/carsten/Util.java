package org.carsten;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONArray;
import org.json.JSONObject;
public class Util {
    static public boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }

    static public void lookupWordDefinition(Activity app, Context context, String word) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/"+word)
                .build();

            client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                app.runOnUiThread(() -> Toast.makeText(context, "Error looking up word: " +e.getMessage(), Toast.LENGTH_SHORT).show());
                Log.e("API Response", e.getMessage(), e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        app.runOnUiThread(() -> Toast.makeText(context, "Definition lookup failed with empty response.", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    String responseBodyStr = responseBody.string();

                    Log.d("API Response", responseBodyStr);

                    try {
                        String definitionStr = "Definition not found";

                        JSONObject jsonObject = new JSONArray(responseBodyStr).getJSONObject(0);
                        JSONArray meaningArray = jsonObject.getJSONArray("meanings");
                        JSONObject meaning = meaningArray.getJSONObject(0);

                        String partOfSpeech = meaning.getString("partOfSpeech");

                        JSONArray definitionArray = meaning.getJSONArray("definitions");
                        JSONObject definitionObj = definitionArray.getJSONObject(0);

                        String definition = definitionObj.getString("definition");

                        definitionStr = partOfSpeech+ ": " + definition;

                        String finalDefinitionStr = definitionStr;
                        app.runOnUiThread(() -> Toast.makeText(context, finalDefinitionStr, Toast.LENGTH_SHORT).show());
                    } catch (Exception e) {
                        app.runOnUiThread(() -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

}
