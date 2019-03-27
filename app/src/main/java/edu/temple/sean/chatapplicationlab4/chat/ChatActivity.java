package edu.temple.sean.chatapplicationlab4.chat;


import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.lang.reflect.Type;
import java.util.ArrayList;

import edu.temple.sean.chatapplicationlab4.MainActivity;
import edu.temple.sean.chatapplicationlab4.R;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private String mUsername;
    private String mPartnerName;
    private ArrayList<Message> mMessageList;
    private String mSavedChatTag;
    private SharedPreferences mPrefs;
    private BroadcastReceiver mReceiver;

    private final String sendMessageURL = "https://kamorris.com/lab/send_message.php";

    public static final String CHAT_TAG_PREFIX = "CHAT_LOG_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        mUsername = intent.getStringExtra(MainActivity.USERNAME_EXTRA);
        mPartnerName = intent.getStringExtra(MainActivity.PARTNER_NAME_EXTRA);

        mSavedChatTag = CHAT_TAG_PREFIX + mPartnerName;

        mPrefs = getSharedPreferences("androidChatApp", MODE_PRIVATE);

        String jsonChatLog = mPrefs.getString(mSavedChatTag, "");
        if(!jsonChatLog.equals("")){
            mMessageList = parseLogJson(jsonChatLog);
        }
        else{
            mMessageList = new ArrayList<>();
        }

        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, mMessageList, mUsername);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageAdapter);

        //Setup send button.
        //TODO encrypt sent text.
        //TODO send message to server.
        Button sendBttn = findViewById(R.id.button_chatbox_send);
        final EditText messageContent = findViewById(R.id.edittext_chatbox);
        sendBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = messageContent.getText().toString();
                if(content.length() > 160){
                    content = content.substring(0,160);
                    Toast.makeText(ChatActivity.this,
                            "Messages ought be less than 160 characters, your message was" +
                                    " truncated.", Toast.LENGTH_SHORT)
                            .show();
                }
                Message msg = new Message(mUsername, content);
                mMessageList.add(msg);
                mMessageAdapter.notifyDataSetChanged();
                messageContent.setText("");

                String json = logToJson();
                mPrefs.edit().putString(mSavedChatTag, json).apply();




            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        String json = logToJson();
        mPrefs.edit().putString(mSavedChatTag, json).apply();

    }

    private String logToJson(){
       Gson gson = new Gson();
       return gson.toJson(mMessageList);
    }

    private ArrayList<Message> parseLogJson(String jsonChatLog) {
        Gson gson = new Gson();
        if(!jsonChatLog.equals("")) {
            Type type = new TypeToken<ArrayList<Message>>(){}.getType();
            return gson.fromJson(jsonChatLog, type);
        }
        else{
            return null;
        }
    }
}

