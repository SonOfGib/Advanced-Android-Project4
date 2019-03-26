package edu.temple.sean.chatapplicationlab4.chat;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import edu.temple.sean.chatapplicationlab4.MainActivity;
import edu.temple.sean.chatapplicationlab4.Partner;
import edu.temple.sean.chatapplicationlab4.R;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private String mUsername;
    private String mPartnerName;
    private ArrayList<Message> mMessageList;
    private String mSavedChatTag;
    private SharedPreferences mPrefs;

    private final String CHAT_TAG_PREFIX = "CHAT_LOG_";

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

        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, mMessageList, mUsername);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageAdapter);

        //Setup send button.
        //TODO encrypt sent text.


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

