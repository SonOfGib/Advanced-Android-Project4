package edu.temple.mapchat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.temple.mapchat.chat.ChatActivity;
import edu.temple.mapchat.chat.Message;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        PartnerFragment.OnListFragmentInteractionListener, NfcAdapter.CreateNdefMessageCallback {



    private boolean mMapView = false;
    private GoogleMap mMap = null;
    private ArrayList<Partner> mPartners;
    private HashMap<String,Marker> mMarkers = new HashMap<>();
    private Context mContext;
    private PartnerFragment mPartFrag;
    private String mUsername;
    private Location mPosition;
    private boolean mLocationDisabled = false;
    private String mFCMToken;
    private boolean mBounded = false;
    private KeyService mKeyService;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;

    private LocationManager lm;
    private LocationListener ll;
    private boolean twoPanes = false;
    private Handler restCallHandler = new Handler();
    private int delay = 30*1000; //1 second=1000 milisecond
    private Runnable runnable;
    private String url ="https://kamorris.com/lab/get_locations.php";
    private String postURL = "https://kamorris.com/lab/register_location.php";
    private String postRegisterURL = "https://kamorris.com/lab/fcm_register.php";
    private RequestQueue queue;
    private SharedPreferences sharedPref;

    private static final String USER_PREF_KEY = "USERNAME_PREF";
    public static final String USERNAME_EXTRA = "USERNAME_EXTRA";
    public static final String PARTNER_NAME_EXTRA = "PARTNER_EXTRA";
    public static final String CHANNEL_ID = "feb9406e-25c7-4b9b-a50a-317c3f39ba44";


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
        }
    };
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BROADCAST", "Received");
            String to = intent.getStringExtra("to");
            String sender = intent.getStringExtra("partner");
            String content = intent.getStringExtra("message");
            content = mKeyService.decrypt(content);
            Message msg = new Message(sender, content);
            ArrayList<Message> messageList = new ArrayList<>();
            String messages = sharedPref.getString(ChatActivity.CHAT_TAG_PREFIX + sender,
                    "");
            if (!messages.equals("")) {
                messageList = parseLogJson(messages);
            }
            messageList.add(msg);
            Gson gson = new Gson();
            String outJson = gson.toJson(messageList);

            sharedPref.edit().putString(ChatActivity.CHAT_TAG_PREFIX + sender, outJson).commit();

            Intent newIntent = new Intent(MainActivity.this, ChatActivity.class);
            newIntent.putExtra(USERNAME_EXTRA, to);
            newIntent.putExtra(PARTNER_NAME_EXTRA, sender);
            PendingIntent pi = PendingIntent.getActivity(mContext, 111, newIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            //Build a notification

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setContentTitle("You have a new message.")
                            .setContentText(sender + ": " + content)
                            .setContentIntent(pi)
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(MainActivity.this);
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(1010, builder.build());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        Intent intent = new Intent(this, KeyService.class);


        bindService(intent, mConnection, BIND_AUTO_CREATE);

        LocationManager locationManager = getSystemService(LocationManager.class);
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {

            public void onLocationChanged(Location location) {
                Log.d("LOCATION", ""+location);
                mPosition = location;
                post(location);

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("TOKEN GET", "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        // Log and toast
                        Log.d("TOKEN GET", token);
                        mFCMToken = token;

                    }
                });
        ll = locationListener;
        lm = locationManager;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && !mLocationDisabled) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
        }
        else if (!mLocationDisabled) {
            Log.d("here", "here");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 10, locationListener);
        }


        mContext = this;
        queue = Volley.newRequestQueue(this);
        //  Determine if only one or two panes are visible
        twoPanes = (findViewById(R.id.mapFragment) != null);
        PartnerFragment partFrag = new PartnerFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.listFragment, partFrag).commit();
        mPartFrag = partFrag;
        // get or create SharedPreferences
        sharedPref = getSharedPreferences("androidChatApp", MODE_PRIVATE);

        //init username with prefs
        initUsername();

        Intent nfcIntent = new Intent(this, MainActivity.class);
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, 0);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mNfcAdapter.setNdefPushMessageCallback(this, this);

        if (twoPanes){
            SupportMapFragment map = new SupportMapFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mapFragment, map).commit();
            map.getMapAsync(this);
            getRequest();
        }
        else{
            Button swapViewButton = findViewById(R.id.switchViewBtn);
            if(mMapView)
                swapViewButton.setText("View List");
            else
                swapViewButton.setText("View Map");
            getRequest();

            swapViewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                    if(!mMapView) {
                        SupportMapFragment mapFragment = new SupportMapFragment();
                        transaction.replace(R.id.listFragment, mapFragment).commit();
                        mapFragment.getMapAsync(MainActivity.this);
                    }
                    else{
                        PartnerFragment partnerFragment = new PartnerFragment();
                        transaction.replace(R.id.listFragment, partnerFragment).commit();
                        mPartFrag = partnerFragment;
                        getRequest();
                    }
                    //toggle map view flag
                    mMapView = !mMapView;
                    Button btn = (Button) view;
                    if(mMapView)
                        btn.setText("View List");
                    else
                        btn.setText("View Map");
                }
            });
        }

    }

    //Prompt for username if we don't have one yet, otherwise get it from prefs and keep going.
    private void initUsername() {
        TextView usernameField = findViewById(R.id.usernameView);
        String storedName = sharedPref.getString(USER_PREF_KEY, null);
        if(storedName != null){
            mUsername = storedName;
            post(mPosition);
            usernameField.setText("Username: "+ mUsername);
        }
        else{
            //We don't have a username, time for first time setup!
            buildUsernameDialog();
            post(mPosition);
        }
    }


    private void buildUsernameDialog() {
        //Preparing views
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_layout, (ViewGroup) findViewById(R.id.layout_root));
        //layout_root should be the name of the "top-level" layout node in the dialog_layout.xml file.
        final EditText usernameBox = layout.findViewById(R.id.usernameView);

        //Building dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setView(layout);
        builder.setCancelable(false);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String localUsername = usernameBox.getText().toString();
                //Generate keys with our username.
                mKeyService.generateMyKeys();
                boolean committed = sharedPref.edit().putString(USER_PREF_KEY, localUsername).commit();
                if (committed) {
                    mUsername = localUsername;
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                initUsername();
                postRegister();

            }
        });
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 111: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "This app will not be usable without location permissions.", Toast.LENGTH_LONG).show();
                    }
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            0, 10, ll);
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 10, ll);
                } else {
                    mLocationDisabled = true;
                }
                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            readPayload(intent);
        }
    }

    @Override
    protected void onResume() {
        //start handler as activity become visible

        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);

        restCallHandler.postDelayed( runnable = new Runnable() {
            public void run() {
                getRequest();
                restCallHandler.postDelayed(runnable, delay);
            }
        }, delay);


        super.onResume();
    }

    private void readPayload(Intent intent) {
        String payload = new String(
                ((NdefMessage) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0])
                        .getRecords()[0]
                        .getPayload());
        Toast.makeText(this, "Recieved Key", Toast.LENGTH_LONG).show();

        //Lop off the 'en' language code.
        String jsonString = payload.substring(3);
        Log.d("Tag debug", jsonString);
        if(jsonString.equals("")){
            Log.d("Message Recieved?", "Message was empty!");
        }
        else {
            try {
                JSONObject json = new JSONObject(jsonString);
                String owner = json.getString("user");
                String pemKey = json.getString("key");
                //if(mBounded)
                    mKeyService.storePublicKeyPEM(owner, pemKey);
//                else{
//                    mStoreKeyWhenReady = true;
//                    mTempOwner = owner;
//                    mTempPemKey = pemKey;
//                }
            }
            catch (JSONException e){
                Log.e("JSON Exception", "Key Problem", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lm.removeUpdates(ll);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("new_message"));
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    }

    /**
     * Make a get request to the server, fetching the list of partners.
     */
    private void getRequest(){
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d("responseInfo","Response is: "+ response);
                        convertJsonToArrayList(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(mContext,"Couldn't fetch data!", Toast.LENGTH_SHORT).show();
                Log.e("fetchError","error was:",error.getCause());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    /**
     * Make a post to the server, providing our current position and username.
     */
    private void post(final Location location){
        Log.d("Post stuff", mUsername + ", " + location);
        if(mUsername == null || mUsername.equals("")|| location == null){
            return;
        }
        StringRequest stringRequest = new StringRequest(Request.Method.POST, postURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Volley Result", ""+response); //the response contains the result from the server, a json string or any other object returned by your server

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace(); //log the error resulting from the request for diagnosis/debugging

            }
        }){

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> postMap = new HashMap<>();
                postMap.put("user", mUsername);
                postMap.put("latitude", ""+location.getLatitude());
                postMap.put("longitude", ""+location.getLongitude());
                return postMap;
            }
        };
        Volley.newRequestQueue(mContext).add(stringRequest);
    }

    private void postRegister(){
        if(mUsername == null || mFCMToken == null){
            return;
        }
        StringRequest stringRequest = new StringRequest(Request.Method.POST, postRegisterURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Volley Register Result", ""+response); //the response contains the result from the server, a json string or any other object returned by your server

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace(); //log the error resulting from the request for diagnosis/debugging

            }
        }){

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> postMap = new HashMap<>();
                postMap.put("user", mUsername);
                postMap.put("token", ""+ mFCMToken);
                return postMap;
            }
        };
        Volley.newRequestQueue(mContext).add(stringRequest);
    }


    private void convertJsonToArrayList(String json){
        ArrayList<Partner> arrayList = new ArrayList<>();
        try {
            JSONArray jsonArr = new JSONArray(json);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                Partner data = new Partner();
                data.setName(jsonObj.getString("username"));
                data.setLastKnownPosition((float) jsonObj.getDouble("latitude"),
                        (float) jsonObj.getDouble("longitude"));
                LatLng l = data.getLastKnownPosition();
                if (mPosition == null) {
                    //TODO get last known app position from prefs.
                    //TODO store mPosition is savedInstanceState
                    data.setDistance(0.0f);
                }
                else{
                    float distance = Math.abs(distance((float) l.latitude, (float) l.longitude,
                            (float) mPosition.getLatitude(), (float) mPosition.getLongitude()));
                    data.setDistance(distance);
                }


                arrayList.add(data);
            }
        }
        catch (JSONException e){
            Log.e("GetReqError","convertJsonToArrayList",e);
        }
        mPartners = arrayList;
        mPartFrag.updatePartnerList(mPartners);
        if(mMap != null){
            updateMap();
        }
    }

    public float distance (float lat_a, float lng_a, float lat_b, float lng_b )
    {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return new Float(distance * meterConversion).floatValue();
    }

    @Override
    protected void onPause() {
        restCallHandler.removeCallbacks(runnable); //stop handler when activity not visible
        mNfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    private void updateMap(){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        Log.d("PartnersNull?","" + mPartners + " ");
        if(mPartners != null && mPartners.size() > 0) {
            for (Partner partner : mPartners) {
                Marker marker = mMarkers.get(partner.getName());
                if(marker == null) {
                    marker = mMap.addMarker(new MarkerOptions().title(partner.getName())
                            .position(partner.getLastKnownPosition()));
                    mMarkers.put(partner.getName(), marker);
                }
                else{
                    marker.remove();
                    marker = mMap.addMarker(new MarkerOptions().title(partner.getName())
                            .position(partner.getLastKnownPosition()));
                    mMarkers.put(partner.getName(), marker);
                }

                builder.include(marker.getPosition());
            }
            LatLngBounds bounds = builder.build();
            int padding = 40; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);
        }
    }
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        updateMap();
    }


    @Override
    public void onListFragmentInteraction(Partner item) {
        Intent intent = new Intent(this, ChatActivity.class);


        if(item.getName().equals(mUsername)){
            Toast.makeText(this, "You can't send a message to yourself!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        RSAPublicKey pubKey = mKeyService.getPublicKey(item.getName());
        if(pubKey == null){
            Toast.makeText(this, "You do not have a shared key for " +item.getName(),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        intent.putExtra(USERNAME_EXTRA, mUsername);
        intent.putExtra(PARTNER_NAME_EXTRA, item.getName());
        startActivity(intent);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Normal", importance);
            channel.setDescription("The default channel.");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
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

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String payload = "";
        String pubKey = mKeyService.getMyPublicKey();
        if(pubKey.equals("")){
            Log.d("SEND EMPTY KEY", "KEY WAS EMPTY!");
        }
        else{
            payload = "{\"user\":\""+ mUsername +"\",\"key\":\""+ pubKey +"\"}";
            Log.d("SENT KEY PAYLOAD", payload);
        }
        NdefRecord record = NdefRecord.createTextRecord(null, payload);
        NdefMessage msg = new NdefMessage(new NdefRecord[]{record});

        return msg;
    }
}
