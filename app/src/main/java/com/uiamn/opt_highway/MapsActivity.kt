package com.uiamn.opt_highway

import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.graphics.Camera
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var REQUEST_PERMISSION = 1000

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var callback: LocationCallback

    companion object {
        var PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 位置情報を扱ふためのpermissionを取得し，startLocationUpdatesを開始
        startLocationUpdates()

        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<Button>(R.id.reload_button).setOnClickListener {
            moveCameraToCurrentPosition()
        }
    }

    private fun moveCameraToCurrentPosition() {
        Log.d("hogehoge", "fugafug")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("hogehoge", "fugafkjlfsjkl")
            return
        }
        locationClient.lastLocation.addOnCompleteListener { task ->
            if(!task.isSuccessful || task.result == null) {
                Log.e("location", "位置情報の取得に失敗")
                return@addOnCompleteListener
            }

            val location = task.result
            val ll = LatLng(location.latitude, location.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(ll))
            mMap.addMarker(MarkerOptions().position(ll).title("現在地"))
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15F))
        }
    }

    private fun startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            for(permission in PERMISSIONS) {
                if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION)
                }
            }
        }

        locationClient = LocationServices.getFusedLocationProviderClient(this)
        request = LocationRequest.create()
        request.interval = 100L
        request.fastestInterval = 50L
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        callback = object: LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                Log.d("location", p0.toString())
            }
        }

        locationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == REQUEST_PERMISSION) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "許可して下さい", Toast.LENGTH_LONG).show()
                this.finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        moveCameraToCurrentPosition()
    }
}