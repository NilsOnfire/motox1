package com.sena_app.motox1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback {


    private int status = 0;
    private int radius = 1;
    private float rideDistance;

    private Boolean driverFound = false;
    private Boolean isLoggingOut = false;
    private Boolean requestType = false;

    private String driverFoundID;
    public String bestProvider;
    private String customerID;




    private GoogleMap mMap;
    Location nLastLocation;
    LocationRequest nLocationRequest;
    private ValueEventListener driverLocationRefListener;

    private SupportMapFragment mapFragment;
    private LinearLayout nInfoPasajero;

    private ImageView nImagenPerfilPasajero;
    private TextView nNombrePasajero, nTelefonoPasajero, nDestinoPasajero;
    Marker driverMarker, pickUpMarker;

    private Button customerCallaCabBtn;
    private Button customerLogoutBtn, customerSettingsBtn, nEstadoViaje, nHistorial;


    private FusedLocationProviderClient nFusedLocationClient;
    private FirebaseAuth nAuth;
    private FirebaseUser currentUser;
    private LatLng customerPickupLocation;
    private LatLng destinoLatLng, pickupLatLng;
    public LocationManager locationManager;
    private DatabaseReference driverAvailableRef, driverLocationRef;
    private DatabaseReference driversRef;
    private DatabaseReference customerDatabaseRef;

    GeoQuery geoQuery;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        customerLogoutBtn = findViewById(R.id.customerLogoutBtn);
        customerSettingsBtn = findViewById(R.id.customerSettingsBtn);
        customerCallaCabBtn = findViewById(R.id.callAcabBtn);

        //currentUser = nAuth.getCurrentUser();
        nAuth = FirebaseAuth.getInstance();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("CustomerRequests");
        driverAvailableRef = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("DriversWorking");
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        nFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        customerLogoutBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                isLoggingOut = true;
                disconnectDriver();
                nAuth.signOut();
                logoutDriver();

            }
        });


        customerCallaCabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(requestType)
                {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if(driverFound != null){
                        driversRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");

                        driversRef.removeValue();

                        driverFoundID = null;

                    }

                    driverFound = false;
                    radius = 1;

                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    geoFire.removeLocation(customerID);

                    if (pickUpMarker != null)
                    {
                    pickUpMarker.remove();
                    }

                    if (driverMarker != null)
                    {
                        driverMarker.remove();
                    }

                    customerCallaCabBtn.setText("Call a cab");

                }
                else
                {
                    requestType = true;

                    LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();

                    String provider = locationManager.getBestProvider(criteria, true);
                    @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(provider);

                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    String customerID = geoFire.getDatabaseReference().push().getKey();
                    geoFire.setLocation(customerID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    customerPickupLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(customerPickupLocation).title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    customerCallaCabBtn.setText("Getting your Driver...");
                    getClosestDriver();
                }



           }

        });



    }

    private void getClosestDriver() {

        GeoFire geoFire = new GeoFire(driverAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(customerPickupLocation.latitude, customerPickupLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestType) {
                    driverFound = true;
                    driverFoundID = key;
//Driver found
                    driversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    driversRef.updateChildren(driverMap);

                    gettingDriverLocation();
                    customerCallaCabBtn.setText("Buscando ubicacion del conductor...");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if (!driverFound) {
                radius = radius + 1;
                getClosestDriver();

            }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void gettingDriverLocation()
    {
       driverLocationRefListener = driverLocationRef.child(driverFoundID).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
               if (snapshot.exists() && requestType)
               {
                  List<Object> driverLocationMap = (List<Object> ) snapshot.getValue();
                  double locationLat = 0;
                  double locationLong = 0;

                  customerCallaCabBtn.setText("Driver Found");

                  if (driverLocationMap.get(0) != null)
                  {
                  locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                  }

                   if (driverLocationMap.get(1) != null)
                   {
                       locationLong =Double.parseDouble(driverLocationMap.get(1).toString());
                   }

                   LatLng driverLatLng = new LatLng(locationLat, locationLong);

                   if(driverMarker != null)
                   {
                       driverMarker.remove();
                   }

                   Location location1 = new Location("");
                   location1.setLatitude(customerPickupLocation.latitude);
                   location1.setLongitude(customerPickupLocation.longitude);

                   Location location2 = new Location("");
                   location2.setLatitude(driverLatLng.latitude);
                   location2.setLongitude(driverLatLng.longitude);

                   float  distance = location1.distanceTo(location2);

                   if (distance < 90){
                       customerCallaCabBtn.setText("Driver is reached.");
                   }
                   else
                   {
                       customerCallaCabBtn.setText("Conductor encontrado" + String.valueOf(distance));
                   }



                   driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Conductor aqui").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

               }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {


            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        nLocationRequest = LocationRequest.create();
        nLocationRequest.setInterval(1000);
        nLocationRequest.setFastestInterval(1000);
        nLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                nFusedLocationClient.requestLocationUpdates(nLocationRequest, nLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }else{
                checkLocationPermission();
            }
        }
    }

    LocationCallback nLocationCallback = new LocationCallback(){
        public void onLocationResult(LocationResult locationResult){
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext() != null){
                    if (!customerID.equals("") && nLastLocation != null && location != null){
                        rideDistance += nLastLocation.distanceTo(location)/1000;
                    }

                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);

                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference().child("DriverWorking");
                    GeoFire geoFireWorking = new GeoFire(refWorking);

                    switch (customerID){
                        case "":
                            geoFireWorking.removeLocation(userId);
                            geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;

                        default:
                            geoFireAvailable.removeLocation(userId);
                            geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;
                    }
                }
            }
        }
    };

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this).setTitle("Dar Permiso").setMessage("Mensaje solicitud de permiso")
                        .setPositiveButton("Ok", (dialogInterface, i) -> {
                            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        nFusedLocationClient.requestLocationUpdates(nLocationRequest, nLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Por favor proporciona el permiso ", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void conectarConductor(){
        checkLocationPermission();
        nFusedLocationClient.requestLocationUpdates(nLocationRequest, nLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    protected void desconectarConductor() {
        if(nFusedLocationClient != null){
            nFusedLocationClient.removeLocationUpdates(nLocationCallback);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isLoggingOut){
            disconnectDriver();
        }
    }

    private void disconnectDriver() {
        if(nFusedLocationClient != null){
            nFusedLocationClient.removeLocationUpdates(nLocationCallback);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("DriverAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }

    private void logoutDriver() {

        Intent welcomeIntent = new Intent(CustomerMapActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }
}