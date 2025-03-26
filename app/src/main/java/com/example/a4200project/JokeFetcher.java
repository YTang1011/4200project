package com.example.a4200project;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JokeFetcher {
    public interface JokeCallback {
        void onJokeReceived(String setup, String punchline);
        void onError();
    }

    public static void fetchNewJoke(JokeCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String[] joke = new String[2];
            try {
                URL url = new URL("https://official-joke-api.appspot.com/random_joke");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                joke[0] = json.getString("setup");
                joke[1] = json.getString("punchline");
            } catch (Exception e) {
                e.printStackTrace();
            }

            handler.post(() -> {
                if (joke[0] != null && joke[1] != null) {
                    callback.onJokeReceived(joke[0], joke[1]);
                } else {
                    callback.onError();
                }
            });
        });
    }
}