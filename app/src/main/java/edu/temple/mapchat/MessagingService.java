package edu.temple.mapchat;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import edu.temple.mapchat.chat.ChatActivity;
import edu.temple.mapchat.chat.Message;

import static edu.temple.mapchat.MainActivity.CHANNEL_ID;
import static edu.temple.mapchat.MainActivity.PARTNER_NAME_EXTRA;
import static edu.temple.mapchat.MainActivity.USERNAME_EXTRA;


public class MessagingService extends FirebaseMessagingService {


    private LocalBroadcastManager localBroadcastManager;

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
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        // Check if message contains a data payload.
       // if (remoteMessage.getData().size() > 0) {
            Log.d("FirebaseMessagingServ", "Message data payload: " + remoteMessage.getData().get("payload"));
            try {
                JSONObject json = new JSONObject(remoteMessage.getData().get("payload"));
                manageMessageJSON(json);
            } catch (JSONException e) {
               Log.e("FirebaseMessagingServ", "Error", e);
            }

  //      }

        // Check if message contains a notification payload.
//        if (remoteMessage.getNotification() != null) {
//            Log.d("FirebaseMessagingServ", "Message Notification Body: " + remoteMessage.getNotification().getBody());
//        }

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

            SharedPreferences prefs = getSharedPreferences("androidChatApp", MODE_PRIVATE);
            String messages = prefs.getString(ChatActivity.CHAT_TAG_PREFIX + sender, "");
            if(!messages.equals("")){
                messageList = parseLogJson(messages);
            }
            messageList.add(msg);
            Gson gson = new Gson();
            String outJson = gson.toJson(messageList);

            Log.d("Message Received", content);

            if(isAppOnForeground(this, "edu.temple.mapchat")){
                Intent intent = new Intent("new_message");
                intent.putExtra("to", to);
                intent.putExtra("partner", sender);
                intent.putExtra("message", content);
                localBroadcastManager.sendBroadcast(intent);
            }
            else{
                prefs.edit().putString(ChatActivity.CHAT_TAG_PREFIX + sender, outJson).apply();

                Intent newIntent = new Intent(this, ChatActivity.class);
                newIntent.putExtra(USERNAME_EXTRA, to);
                newIntent.putExtra(PARTNER_NAME_EXTRA, sender);
                PendingIntent pi = PendingIntent.getActivity(this,111, newIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                //Build a notification

                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(this, CHANNEL_ID)
                                .setSmallIcon(R.mipmap.ic_launcher_round)
                                .setContentTitle("You have a new message.")
                                .setContentText(sender + ": " +content)
                                .setContentIntent(pi)
                                .setAutoCancel(true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(this);
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(1010, builder.build());
            }



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

    private boolean isAppOnForeground(Context context,String appPackageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(appPackageName)) {
                //                Log.e("app",appPackageName);
                return true;
            }
        }
        return false;
    }
}
