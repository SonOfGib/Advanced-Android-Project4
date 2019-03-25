package edu.temple.sean.chatapplicationlab4;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PartnerFragment.OnListFragmentInteractionListener {

    boolean mMapView = false;
    GoogleMap mMap = null;
    ArrayList<Partner> mPartners;
    HashMap<String,Marker> mMarkers = new HashMap<>();
    Context mContext;
    PartnerFragment mPartFrag;
    String mUsername;
    Location mPosition;
    boolean mLocationDisabled = false;

    LocationManager lm;
    LocationListener ll;
    boolean twoPanes = false;
    Handler restCallHandler = new Handler();
    int delay = 30*1000; //1 second=1000 milisecond
    Runnable runnable;
    String url ="https://kamorris.com/lab/get_locations.php";
    String postURL = "https://kamorris.com/lab/register_location.php";
    RequestQueue queue;
    SharedPreferences sharedPref;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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


        final TextView usernameField = findViewById(R.id.editText);
        mContext = this;
        queue = Volley.newRequestQueue(this);
        //  Determine if only one or two panes are visible
        twoPanes = (findViewById(R.id.mapFragment) != null);
        PartnerFragment partFrag = new PartnerFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapFragment, partFrag).commit();
        mPartFrag = partFrag;
        getRequest();
        // get or create SharedPreferences
        sharedPref = getSharedPreferences("androidChatApp", MODE_PRIVATE);
        // save your string in SharedPreferences
        String storedName = sharedPref.getString("username", null);
        if(storedName != null){
            usernameField.setText(storedName);
            mUsername = storedName;
            post(mPosition);
        }

        if (twoPanes){
            SupportMapFragment map = new SupportMapFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mapFragment, map).commit();
            map.getMapAsync(this);
        }
        else{
            Button swapViewButton = findViewById(R.id.switchViewBtn);
            if(mMapView)
                swapViewButton.setText("View List");
            else
                swapViewButton.setText("View Map");

            swapViewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                    if(!mMapView) {
                        SupportMapFragment mapFragment = new SupportMapFragment();
                        transaction.replace(R.id.mapFragment, mapFragment).commit();
                        mapFragment.getMapAsync(MainActivity.this);
                    }
                    else{
                        PartnerFragment partnerFragment = new PartnerFragment();
                        transaction.replace(R.id.mapFragment, partnerFragment).commit();
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

        Button submitBtn = findViewById(R.id.submitButton);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!usernameField.getText().toString().equals("")) {
                    mUsername = usernameField.getText().toString();
                    sharedPref.edit().putString("username", mUsername).commit();
                    post(mPosition);
                }
            }
        });

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
                    }
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            0, 10, ll);
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 10, ll);
                } else {
                    mLocationDisabled = true;
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onResume() {
        //start handler as activity become visible
        restCallHandler.postDelayed( runnable = new Runnable() {
            public void run() {
                getRequest();
                restCallHandler.postDelayed(runnable, delay);
            }
        }, delay);

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lm.removeUpdates(ll);
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
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED && !mLocationDisabled) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
                    }
                    Location lo = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if(lo != null){
                        mPosition = lo;
                        float distance = Math.abs(distance((float) l.latitude, (float) l.longitude,
                                (float) mPosition.getLatitude(), (float) mPosition.getLongitude()));
                        data.setDistance(distance);
                    }
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


    //Useless
    @Override
    public void onListFragmentInteraction(Partner item) {
        //TODO: Is this necessary?
    }
}
