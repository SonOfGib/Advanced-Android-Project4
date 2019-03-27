package edu.temple.sean.chatapplicationlab4;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

import edu.temple.sean.chatapplicationlab4.chat.ChatActivity;
import edu.temple.sean.chatapplicationlab4.chat.Message;


public class MessagingService extends FirebaseMessagingService {


    //Commit new token to prefs so we can register our user.
    @Override
    public void onNewToken(String s) {
        Log.d("NEW_TOKEN", s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("FirebaseMessagingServ", "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d("FirebaseMessagingServ", "Message data payload: " + remoteMessage.getData().get("payload"));
            try {
                JSONObject json = new JSONObject(remoteMessage.getData().get("payload"));
                manageMessageJSON(json);
            } catch (JSONException e) {
               Log.e("FirebaseMessagingServ", "Error", e);
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d("FirebaseMessagingServ", "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private void manageMessageJSON(JSONObject json) {
        try {
            String to = json.getString("to");
            String sender = json.getString("from");
            String content = json.getString("message");
            //Broadcast that a message has been received for the chat activity, add message to stored list.
            Message msg = new Message(sender, content);
            ArrayList<Message> messageList = new ArrayList<>();

            //TODO BROADCAST
            SharedPreferences prefs = getSharedPreferences("androidChatApp", MODE_PRIVATE);
            String messages = prefs.getString(ChatActivity.CHAT_TAG_PREFIX + sender, "");
            if(!messages.equals("")){
                messageList = parseLogJson(messages);
            }
            messageList.add(msg);
            Gson gson = new Gson();
            String outJson = gson.toJson(messageList);

            Log.d("Message Received", content);

            prefs.edit().putString(ChatActivity.CHAT_TAG_PREFIX + sender, outJson).apply();



        }catch (JSONException e){
            Log.e("JSON Exception", "Message Problem", e);
        }
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
