package com.example.a4200project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EnjoyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enjoy);

        Button btnJoke = findViewById(R.id.btnJoke);
        btnJoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchJoke();
            }
        });
    }

    private void fetchJoke() {
        JokeFetcher.fetchNewJoke(new JokeFetcher.JokeCallback() {
            @Override
            public void onJokeReceived(String setup, String punchline) {
                Intent intent = new Intent(EnjoyActivity.this, JokeActivity.class);
                intent.putExtra("setup", setup);
                intent.putExtra("punchline", punchline);
                startActivity(intent);
            }

            @Override
            public void onError() {
                Toast.makeText(EnjoyActivity.this,
                        "Failed to fetch joke",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
