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

import java.util.List;

public class DriversMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location nLastLocation;
    LocationRequest nLocationRequest;

    private FusedLocationProviderClient nFusedLocationClient;

   // private int status = 0;
    private String pasajeroId;
    private float rideDistance;
    private Boolean isLoggingOut = false;
    private String driverID, customerID = "";

    private FirebaseAuth nAuth;
    private FirebaseUser currentUser;
    private LatLng destinoLatLng, pickupLatLng;
    private  DatabaseReference assignedCustomerRef, assignedCustomerPickUpRef, driverAvailableRef;

    private SupportMapFragment mapFragment;
    private LinearLayout nInfoPasajero;
    private ImageView nImagenPerfilPasajero;
    private TextView nNombrePasajero, nTelefonoPasajero, nDestinoPasajero;
    private Button driverLogoutBtn, driverSettingsBtn, nEstadoViaje, nHistorial;
    Marker pickUpMarker;
    private ValueEventListener  assignedCustomerRefListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        nAuth = FirebaseAuth.getInstance();
        currentUser = nAuth.getCurrentUser();
        driverID = nAuth.getCurrentUser().getUid();

        driverLogoutBtn = findViewById(R.id.driverLogoutBtn);
        driverSettingsBtn= findViewById(R.id.driverSettingsBtn);
        driverAvailableRef = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        nFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        driverLogoutBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                isLoggingOut = true;
                disconnectDriver();
                nAuth.signOut();
                logoutDriver();

            }
        });
getAssignedCustomerRequest();

    }

    private void getAssignedCustomerRequest()
    {
        assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Drivers").child(driverID).child("CustomerRideID");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if(snapshot.exists()){
                    customerID = snapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                }
                else
                {
                    customerID = "";

                    if (pickUpMarker != null){
                        pickUpMarker.remove();
                    }

                    if (assignedCustomerRefListener != null){
                        assignedCustomerRef.removeEventListener(assignedCustomerRefListener);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    private void getAssignedCustomerPickupLocation()
    {
      assignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference().child("CustomerRequests")
      .child(customerID).child("l");

      assignedCustomerRefListener = assignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
              if (snapshot.exists()) {
                  List<Object> customerLocationMap = (List<Object>) snapshot.getValue();
                  double locationLat = 0;
                  double locationLong = 0;



                  if (customerLocationMap.get(0) != null)
                  {
                      locationLat =Double.parseDouble(customerLocationMap.get(0).toString());
                  }

                  if (customerLocationMap.get(1) != null)
                  {
                      locationLong =Double.parseDouble(customerLocationMap.get(1).toString());
                  }

                  LatLng driverLatLng = new LatLng(locationLat, locationLong);
                   mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Lugar de recogida pasajero").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
              }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {

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

                    LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();

                    String provider = locationManager.getBestProvider(criteria, true);
                    @SuppressLint("MissingPermission") Location nlocation = locationManager.getLastKnownLocation(provider);

                    GeoFire geoFire = new GeoFire(driverAvailableRef);
                    String customerID = geoFire.getDatabaseReference().push().getKey();

//                    if (!pasajeroId.equals("") && nLastLocation != null && location != null){
//                        rideDistance += nLastLocation.distanceTo(location)/1000;
//                    }

                    LatLng latLng = new LatLng(nlocation.getLatitude(),nlocation.getLongitude());
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
                            ActivityCompat.requestPermissions(DriversMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(DriversMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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

        Intent welcomeIntent = new Intent(DriversMapActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }
}