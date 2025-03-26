package com.example.a4200project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class JokeListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private JokeAdapter adapter;
    private JokeDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joke_list);

        dbHelper = new JokeDbHelper(this);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadJokes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadJokes();
    }

    private void loadJokes() {
        List<JokeDbHelper.Joke> jokes = dbHelper.getAllJokes();
        adapter = new JokeAdapter(jokes);
        recyclerView.setAdapter(adapter);
    }

    private class JokeAdapter extends RecyclerView.Adapter<JokeAdapter.ViewHolder> {
        private List<JokeDbHelper.Joke> jokes;

        JokeAdapter(List<JokeDbHelper.Joke> jokes) {
            this.jokes = jokes;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_joke, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            JokeDbHelper.Joke joke = jokes.get(position);
            holder.tvSetup.setText(joke.getSetup());
            holder.tvPunchline.setText(joke.getPunchline());

            holder.btnDelete.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    JokeDbHelper.Joke currentJoke = jokes.get(currentPosition);
                    dbHelper.deleteJoke(currentJoke.getId());

                    jokes.clear();
                    jokes.addAll(dbHelper.getAllJokes());
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return jokes.size();
        }

        @Override
        public long getItemId(int position) {
            return jokes.get(position).getId();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSetup, tvPunchline;
            Button btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvSetup = itemView.findViewById(R.id.itemSetup);
                tvPunchline = itemView.findViewById(R.id.itemPunchline);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}