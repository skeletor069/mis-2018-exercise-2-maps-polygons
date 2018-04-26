package com.example.mis.polygons;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;

import static android.location.LocationManager.GPS_PROVIDER;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public class MarkerData{
        public double latitude;
        public double longitude;
        public String message;
        public LatLng latLng;
    }

    private GoogleMap mMap;
    SupportMapFragment mapFragment;
    RelativeLayout startupScreen;
    double longitude;
    double latitude;
    ImageButton resetBtn;
    Button polygonBtn;
    TextView infoText;
    EditText customDescriptionEdtTxt;
    SharedPreferences defaultPref;
    Polygon polygon;
    Marker centroidMarker;

    double EARTH_RADIUS = 6371009;

    boolean polygonOnScreen = false;

    ArrayList<MarkerData> markerData = new ArrayList<MarkerData>();
    ArrayList<MarkerData> sessionMarkerData = new ArrayList<MarkerData>();
    ArrayList<Marker> sessionMarkers = new ArrayList<Marker>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        InitVars();

        if (StartupCheckSuccess()) {
            MoveMapToMyLocation();
        }

    }

    void InitVars(){
        defaultPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(!defaultPref.contains(getResources().getString(R.string.KEY_next_pos))){
            SharedPreferences.Editor editor = defaultPref.edit();
            editor.putInt(getResources().getString(R.string.KEY_next_pos), 0);
            editor.commit();
        }
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        startupScreen = (RelativeLayout) findViewById(R.id.startupScreen);
        resetBtn = (ImageButton) findViewById(R.id.resetBtn);
        infoText = (TextView) findViewById(R.id.infoText);
        customDescriptionEdtTxt = (EditText) findViewById(R.id.customDescription);
        polygonBtn = (Button) findViewById(R.id.polygonBtn);


        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetBtn.setVisibility(View.INVISIBLE);
                infoText.setText("Loading...");
                if (StartupCheckSuccess()) {
                    MoveMapToMyLocation();
                }
            }
        });

        polygonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!polygonOnScreen)
                    DrawPolygonUsingMarkerData();
                else
                    RemovePreviousPolgon();
                //sessionMarkerData.clear();
            }
        });


    }

    void RemovePreviousPolgon(){

        for (Marker marker : sessionMarkers){
            marker.remove();
        }

        sessionMarkerData.clear();
        sessionMarkers.clear();
        polygonBtn.setText("Start Polygon");
        if(polygon != null)
            polygon.remove();
        if(centroidMarker != null)
            centroidMarker.remove();
        polygonOnScreen = false;
    }

    void DrawPolygonUsingMarkerData(){

        if(sessionMarkerData.size() < 3){
            ShowLongToast("You need atleast 3 markers to draw polygon.");
            return;
        }

        PolygonOptions rectOptions = new PolygonOptions();
        rectOptions.fillColor(0x669999FF);

        Log.d("sessionDataValid", (sessionMarkerData == null) ? "Null": "Ok");

        for(MarkerData data : sessionMarkerData){
            rectOptions.add(data.latLng);
            Log.d("sessionDataValidLoop", (sessionMarkerData == null) ? "Null": "Ok");
        }

        Log.d("sessionDataValidLoop", "Ended");

        polygon = mMap.addPolygon(rectOptions);
        polygonBtn.setText("End Polygon");
        CalculateAreaOfPolygon();
        polygonOnScreen = true;

    }

    void CalculateAreaOfPolygon(){

        LatLng centerLatLng = null;

        ArrayList<LatLng> temp = new ArrayList<LatLng>();

        Builder builder = new LatLngBounds.Builder();
        for(int i = 0 ; i < sessionMarkerData.size() ; i++)
        {
            builder.include(sessionMarkerData.get(i).latLng);
            temp.add(sessionMarkerData.get(i).latLng);
        }
        LatLngBounds bounds = builder.build();
        centerLatLng =  bounds.getCenter();

        MarkerOptions op = new MarkerOptions().position(centerLatLng).title("Are of polygon : " + ComputeSignedArea(temp, EARTH_RADIUS) + "sqm");
        centroidMarker = mMap.addMarker(op);


    }

    void MoveMapToMyLocation() {
        LocationManager lm = (LocationManager) getSystemService(android.content.Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        Location location = lm.getLastKnownLocation(GPS_PROVIDER);
        longitude = location.getLongitude();
        latitude = location.getLatitude();
        mapFragment.getMapAsync(this);
    }

    boolean StartupCheckSuccess(){
        if(!isNetworkAvailable()){
            infoText.setText("Please connect to the internet and click the button above.");
            resetBtn.setVisibility(View.VISIBLE);
        }
        else {
            startupScreen.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager
                = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for(String permission: permissions){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
                //denied
                Toast.makeText(this,"Location Access permission denied. Can't center map to your location.", Toast.LENGTH_LONG).show();

            }else{
                if(ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED){
                    //allowed
                    //startupScreen.setVisibility(View.GONE);
                    Toast.makeText(this,"Location Access permission granted.", Toast.LENGTH_SHORT).show();
                    MoveMapToMyLocation();
                } else{
                    //set to never ask again
                    Toast.makeText(this,"Go to settings and accept location access permission.", Toast.LENGTH_LONG).show();
                    //do something here.
                }
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if(!customDescriptionEdtTxt.getText().toString().trim().equals("")) {
                    MarkerOptions op = new MarkerOptions().position(latLng).title(customDescriptionEdtTxt.getText().toString());
                    Marker marker = mMap.addMarker(op);
                    sessionMarkers.add(marker);


                    int nextPos = defaultPref.getInt(getResources().getString(R.string.KEY_next_pos),0);

                    MarkerData temp = new MarkerData();
                    temp.latitude = latLng.latitude;
                    temp.longitude = latLng.longitude;
                    temp.message = customDescriptionEdtTxt.getText().toString();
                    temp.latLng = latLng;
                    markerData.add(temp);
                    sessionMarkerData.add(temp);

                    SharedPreferences.Editor editor = defaultPref.edit();
                    editor.putLong(getResources().getString(R.string.KEY_Lat_)+nextPos, (long) latLng.latitude);
                    editor.putLong(getResources().getString(R.string.KEY_Long_)+nextPos, (long) latLng.longitude);
                    editor.putString(getResources().getString(R.string.KEY_message_)+nextPos, customDescriptionEdtTxt.getText().toString());
                    nextPos++;
                    editor.putInt(getResources().getString(R.string.KEY_next_pos), nextPos);
                    editor.apply();


                    ShowShortToast("Marker added successfully");
                    customDescriptionEdtTxt.setText("");
                }
                else{
                    ShowLongToast("Enter a custom description and then long press on map.");
                }
            }
        });

        // Add a marker in Sydney and move the camera
        LatLng yourLocation = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(yourLocation).title("You!!!"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(yourLocation));
    }

    private void ShowAllPreviousMarkers() {
        for(int i = 0; i < defaultPref.getInt(getResources().getString(R.string.KEY_next_pos), 0); i++){
            if(defaultPref.contains(getResources().getString(R.string.KEY_Lat_) + i)){

                MarkerData temp = new MarkerData();

                temp.latitude = (double) defaultPref.getLong(getResources().getString(R.string.KEY_Lat_) + i, 0);
                temp.longitude = (double) defaultPref.getLong(getResources().getString(R.string.KEY_Long_) + i, 0);
                temp.message = defaultPref.getString(getResources().getString(R.string.KEY_message_) + i, "");
                temp.latLng = new LatLng(temp.latitude,temp.longitude);
                mMap.addMarker(new MarkerOptions().position(temp.latLng).title(temp.message));

                markerData.add(temp);
            }
        }
    }

    void ShowLongToast(String text){
        Toast.makeText(getApplicationContext(),text, Toast.LENGTH_LONG).show();
    }

    void ShowShortToast(String text){
        Toast.makeText(getApplicationContext(),text, Toast.LENGTH_SHORT).show();
    }

    static double ComputeSignedArea(ArrayList<LatLng> path, double radius)
    {
        int size = path.size();
        if (size < 3) { return 0; }
        double total = 0;
        LatLng prev = path.get(size - 1);
        double prevTanLat = Math.tan((Math.PI / 2 - Math.toRadians(prev.latitude)) / 2);
        double prevLng = Math.toRadians(prev.longitude);

        for (LatLng point : path)
        {
            double tanLat = Math.tan((Math.PI / 2 - Math.toRadians(point.latitude)) / 2);
            double lng = Math.toRadians(point.longitude);
            total += PolarTriangleArea(tanLat, lng, prevTanLat, prevLng);
            prevTanLat = tanLat;
            prevLng = lng;
        }
        return total * (radius * radius);
    }

    static double PolarTriangleArea(double tan1, double lng1, double tan2, double lng2)
    {
        double deltaLng = lng1 - lng2;
        double t = tan1 * tan2;
        return 2 * Math.atan2(t * Math.sin(deltaLng), 1 + t * Math.cos(deltaLng));
    }
}
