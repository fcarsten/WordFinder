package org.carstenf.wordfinder;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

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
                        app.runOnUiThread(() -> Toast.makeText(context, "Definition lookup for " +word+" failed with empty response.", Toast.LENGTH_SHORT).show());
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
                        app.runOnUiThread(() -> {
                            View view = app.findViewById(android.R.id.content);
                            Snackbar snackbar = Snackbar.make(view, definitionStr, Snackbar.LENGTH_LONG);
                            View snackbarView = snackbar.getView();

                            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                            if (textView != null) {
                                textView.setMaxLines(10);
                            } else {
                                Log.e("Util", "TextView not found in Snackbar view to adjust number of lines");
                            }

                            ViewGroup.LayoutParams params = snackbarView.getLayoutParams();
                            params.width = ViewGroup.LayoutParams.WRAP_CONTENT; // Wrap the width to text size
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT; // Optional: Wrap height
                            snackbarView.setLayoutParams(params);

                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                            layoutParams.gravity = Gravity.CENTER; // Adjust gravity if needed
                            snackbarView.setLayoutParams(layoutParams);

                            snackbar.show();
                        });
                    } catch (Exception e) {
                        app.runOnUiThread(() -> Toast.makeText(context, "Error looking up "+word+": " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
                else {
                    app.runOnUiThread(() -> Toast.makeText(context, "Definition not found for: "+ word, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

}
