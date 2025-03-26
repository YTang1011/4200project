package com.example.a4200project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class JokeActivity extends AppCompatActivity {

    private TextView tvSetup, tvPunchline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joke);

        tvSetup = findViewById(R.id.tvSetup);
        tvPunchline = findViewById(R.id.tvPunchline);
        Button btnAnother = findViewById(R.id.btnAnother);

        displayJokeFromIntent();

        btnAnother.setOnClickListener(v -> fetchNewJoke());
    }

    private void displayJokeFromIntent() {
        String setup = getIntent().getStringExtra("setup");
        String punchline = getIntent().getStringExtra("punchline");
        tvSetup.setText(setup);
        tvPunchline.setText(punchline);
    }

    private void fetchNewJoke() {
        JokeFetcher.fetchNewJoke(new JokeFetcher.JokeCallback() {
            @Override
            public void onJokeReceived(String setup, String punchline) {
                tvSetup.setText(setup);
                tvPunchline.setText(punchline);
            }

            @Override
            public void onError() {
                Toast.makeText(JokeActivity.this,
                        "Failed to fetch new joke",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}