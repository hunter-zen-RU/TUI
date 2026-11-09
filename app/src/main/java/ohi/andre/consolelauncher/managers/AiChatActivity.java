package ohi.andre.consolelauncher.managers;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.R;

public class AiChatActivity extends Activity {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText inputEt;
    private ImageButton sendBtn;
    private AiManager aiManager;
    private final List<ChatMessage> messages = new ArrayList<>();

    private static class ChatMessage {
        String text;
        boolean isUser;

        ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ai_chat_layout);

        recyclerView = findViewById(R.id.chat_rv);
        inputEt = findViewById(R.id.chat_input_et);
        sendBtn = findViewById(R.id.chat_send_btn);

        aiManager = new AiManager(this);
        adapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        sendBtn.setOnClickListener(v -> sendMessage());
        
        messages.add(new ChatMessage("Hello! I am your Gemini assistant. How can I help you?", false));
        adapter.notifyDataSetChanged();
    }

    private void sendMessage() {
        String text = inputEt.getText().toString().trim();
        if (text.isEmpty()) return;

        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
        inputEt.setText("");

        aiManager.generateResponse(text, new AiManager.AiCallback() {
            @Override
            public void onResponse(CharSequence response) {
                messages.add(new ChatMessage(response.toString(), false));
                adapter.notifyItemInserted(messages.size() - 1);
                recyclerView.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onError(String error) {
                messages.add(new ChatMessage("Error: " + error, false));
                adapter.notifyItemInserted(messages.size() - 1);
                recyclerView.scrollToPosition(messages.size() - 1);
            }
        });
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private final List<ChatMessage> data;

        ChatAdapter(List<ChatMessage> data) { this.data = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatMessage m = data.get(position);
            TextView tv = (TextView) holder.itemView;
            tv.setText((m.isUser ? "YOU: " : "AI: ") + m.text);
            tv.setTextColor(m.isUser ? Color.parseColor("#88C0D0") : Color.parseColor("#00FF66"));
            tv.setTextSize(14);
            tv.setPadding(16, 8, 16, 8);
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View v) { super(v); }
        }
    }
}
