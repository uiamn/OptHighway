package com.uiamn.opt_highway

import android.content.pm.PackageManager
import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.GeoApiContext
import java.lang.ref.WeakReference

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var REQUEST_PERMISSION = 1000

    private val MSG_RESULT = 1234

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var callback: LocationCallback

    private lateinit var geoApiContext: GeoApiContext

    private val gllh = MapsActivity.gLLFPNHandler(this)

    companion object {
        var PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("a", "start!")
        super.onCreate(savedInstanceState)

        // 位置情報を扱ふためのpermissionを取得し，startLocationUpdatesを開始
        startLocationUpdates()

        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // GeoApiContextを構築
        geoApiContext = GeoApiContext.Builder()
                .apiKey(getString(R.string.google_maps_key))
                .build()


        findViewById<Button>(R.id.reload_button).setOnClickListener {
            moveCameraToCurrentPosition()
        }

        findViewById<Button>(R.id.startSearchButton).setOnClickListener {
            getLatLngFromPositionName()
        }
    }

    private fun getLatLngFromPositionName() {
        val deptText = findViewById<EditText>(R.id.departureInput).text.toString()
        val destText = findViewById<EditText>(R.id.destinationInput).text.toString()

        Log.d("aaaaa", deptText)
        Log.d("bbbbb", destText)

        gLLFPNThread(gllh, geoApiContext, deptText, destText).start()

//        val deptRes = findPlaceFromText(geoApiContext, deptText, InputType.TEXT_QUERY).language("ja").awaitIgnoreError()
//        val destRes = findPlaceFromText(geoApiContext, destText, InputType.TEXT_QUERY).language("ja").awaitIgnoreError()
//
//        val deptLatLng = deptRes.candidates[0].geometry.location
//        val destLatLng = destRes.candidates[0].geometry.location
//
//        mMap.addMarker(MarkerOptions().position(LatLng(deptLatLng.lat, deptLatLng.lng)).title("出発地"))
//        mMap.addMarker(MarkerOptions().position(LatLng(destLatLng.lat, destLatLng.lng)).title("目的地"))
    }

    private fun po(v: DeptDestLatLng) {
        Log.d("hoge", v.toString())
        mMap.addMarker(MarkerOptions().position(v.dept).title("出発地"))
        mMap.addMarker(MarkerOptions().position(v.dest).title("目的地"))
    }

    private class gLLFPNHandler(activity: MapsActivity) : Handler(Looper.getMainLooper()) {
        private var activityRef: WeakReference<MapsActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = activityRef.get()
            if (activity == null || activity.isFinishing) {
                return
            }

            if (msg.what == activity.MSG_RESULT) {
                activity.po(msg.obj as DeptDestLatLng)
            }
        }
    }

    private class gLLFPNThread(
        handler: gLLFPNHandler,
        geoApiContext: GeoApiContext,
        deptText: String,
        destText: String
    ) : Thread() {
        private var gllfpn = GetLatLngFromPositionName(geoApiContext)
        private val handler = handler
        private val deptText = deptText
        private val destText = destText

        override fun run() {
            val latLngs = gllfpn.getLatLngFromPositionName(deptText, destText)
            handler.sendMessage(handler.obtainMessage(1234, latLngs))
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
//        mMap.isMyLocationEnabled = true;
        moveCameraToCurrentPosition()
    }
}