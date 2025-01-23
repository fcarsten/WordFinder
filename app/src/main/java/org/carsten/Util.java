package org.carsten;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
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

    static public void lookupWordDefinition(Activity app, Context context, Result result) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/"+result)
                .build();

            client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                app.runOnUiThread(() -> {
                    Toast.makeText(context, "Error looking up word: " +e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                Log.e("API Response", e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("API Response", responseBody);

                    try {
                        String definitionStr = "Definition not found";

                        JSONObject jsonObject = new JSONArray(responseBody).getJSONObject(0);
                        JSONArray meaningArray = jsonObject.getJSONArray("meanings");
                        JSONObject meaning = meaningArray.getJSONObject(0);

                        String partOfSpeech = meaning.getString("partOfSpeech");

                        JSONArray definitionArray = meaning.getJSONArray("definitions");
                        JSONObject definitionObj = definitionArray.getJSONObject(0);

                        String definition = definitionObj.getString("definition");

                        definitionStr = partOfSpeech+ ": " + definition;

                        String finalDefinitionStr = definitionStr;
                        app.runOnUiThread(() -> {
                            Toast.makeText(context, finalDefinitionStr, Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        app.runOnUiThread(() -> {
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
            }
        });
    }

}
