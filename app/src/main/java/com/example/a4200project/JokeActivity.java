package com.example.a4200project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class JokeActivity extends AppCompatActivity {
    private TextView tvSetup, tvPunchline;
    private JokeDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joke);

        dbHelper = new JokeDbHelper(this);
        tvSetup = findViewById(R.id.tvSetup);
        tvPunchline = findViewById(R.id.tvPunchline);
        Button btnAnother = findViewById(R.id.btnAnother);
        Button btnSave = findViewById(R.id.btnSave);

        displayJokeFromIntent();

        btnAnother.setOnClickListener(v -> fetchNewJoke());
        btnSave.setOnClickListener(v -> saveCurrentJoke());
    }

    private void saveCurrentJoke() {
        String setup = tvSetup.getText().toString();
        String punchline = tvPunchline.getText().toString();

        if (!setup.isEmpty() && !punchline.isEmpty()) {
            long result = dbHelper.addJoke(setup, punchline);
            if (result != -1) {
                Toast.makeText(this, "Joke saved!", Toast.LENGTH_SHORT).show();
            }
        }
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