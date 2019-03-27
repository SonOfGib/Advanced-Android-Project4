package edu.temple.mapchat.chat;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.temple.mapchat.KeyService;
import edu.temple.mapchat.MainActivity;
import edu.temple.mapchat.R;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private String mUsername;
    private String mPartnerName;
    private ArrayList<Message> mMessageList;
    private String mSavedChatTag;
    private SharedPreferences mPrefs;

    private final String sendMessageURL = "https://kamorris.com/lab/send_message.php";

    public static final String CHAT_TAG_PREFIX = "CHAT_LOG_";

    //receive messages
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BROADCAST", "Received");
            String to = intent.getStringExtra("to");
            String sender = intent.getStringExtra("partner");
            String content = intent.getStringExtra("message");
            content = decryptMessage(content);
            if(sender.equals(mPartnerName)) {
                Message message = new Message(sender, content);
                mMessageList.add(message);
                mMessageAdapter.notifyDataSetChanged();
                mMessageRecycler.scrollToPosition(mMessageList.size() - 1);
            }
            //else not our message to deal with.
        }
    };
    private boolean mBounded = false;
    private KeyService mKeyService;
    private boolean mDecryptOnBind;
    private String mCipher;

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("new_message"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        mUsername = intent.getStringExtra(MainActivity.USERNAME_EXTRA);
        mPartnerName = intent.getStringExtra(MainActivity.PARTNER_NAME_EXTRA);
        String content = intent.getStringExtra("content");

        Log.d("Intent Contents", mUsername + ", " + mPartnerName);

        Intent serviceIntent = new Intent(this,KeyService.class);
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);

        mSavedChatTag = CHAT_TAG_PREFIX + mPartnerName;

        mPrefs = getSharedPreferences("androidChatApp", MODE_PRIVATE);

        String jsonChatLog = mPrefs.getString(mSavedChatTag, "");
        if(!jsonChatLog.equals("")){
            mMessageList = parseLogJson(jsonChatLog);
        }
        else{
            mMessageList = new ArrayList<>();
        }
        if(content != null){
            //we have to create a message now!

            if(mBounded) {
                content = mKeyService.decrypt(content);
                Message msg = new Message(mPartnerName, content);
                mMessageList.add(msg);
                mMessageAdapter.notifyDataSetChanged();
                mMessageRecycler.scrollToPosition(mMessageList.size() -1);

            }
            else {
                mDecryptOnBind = true;
                mCipher = content;
            }

        }

        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, mMessageList, mUsername);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageAdapter);
        if(mMessageList.size() - 1 > 0)
            mMessageRecycler.scrollToPosition(mMessageList.size() - 1);

        //Setup send button.
        //TODO encrypt sent text.
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
                mMessageRecycler.scrollToPosition(mMessageList.size() -1);
                messageContent.setText("");
                sendMessage(msg);

                String json = logToJson();
                mPrefs.edit().putString(mSavedChatTag, json).apply();




            }
        });


    }

    private void sendMessage(Message msg) {
        final String user = msg.getSender();
        final String partnerUser = mPartnerName;
        final String message = msg.getContent();
        final String encryptedMessage = mKeyService.encrypt(message, partnerUser);

        Log.d("Send Message Post", user + ", " + partnerUser + ", " + message);
        if(user == null || partnerUser.equals("")|| message == null){
            return;
        }
        StringRequest stringRequest = new StringRequest(Request.Method.POST, sendMessageURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Send Message Result", ""+response); //the response contains the result from the server, a json string or any other object returned by your server

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace(); //log the error resulting from the request for diagnosis/debugging

            }
        }){
            @Override
            protected Map<String, String> getParams(){
                Map<String, String> postMap = new HashMap<>();
                postMap.put("user", user);
                postMap.put("partneruser", ""+partnerUser);
                postMap.put("message", ""+ encryptedMessage);
                return postMap;
            }
        };
        Volley.newRequestQueue(this).add(stringRequest);
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

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBounded = false;
            mKeyService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBounded = true;
            KeyService.LocalBinder mLocalBinder = (KeyService.LocalBinder) service;

            mKeyService = mLocalBinder.getService();
            //If we have been requested to store a key on connection ...
            if(mDecryptOnBind){
                String content = mKeyService.decrypt(mCipher);
                Message msg = new Message(mPartnerName, content);
                mMessageList.add(msg);
                mMessageAdapter.notifyDataSetChanged();
                mMessageRecycler.scrollToPosition(mMessageList.size() -1);

                mDecryptOnBind = false;
                mCipher = "";
            }
        }
    };

    String decryptMessage(String message){
        return mKeyService.decrypt(message);
    }
}

