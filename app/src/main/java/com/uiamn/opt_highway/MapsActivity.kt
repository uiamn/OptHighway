package com.uiamn.opt_highway

import android.content.pm.PackageManager
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import com.google.android.gms.maps.model.*

import com.google.maps.GeoApiContext
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var REQUEST_PERMISSION = 1000

    private val DEPT_SUGGEST_REQ = 1234
    private val DEST_SUGGEST_REQ = 1235

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var callback: LocationCallback

    private lateinit var geoApiContext: GeoApiContext
    private lateinit var mapsAPI: MapsFunctions

    private val getLatLngFromPosNameHandler = MapsActivity.HandlerInMapsActivity(this)

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

        mapsAPI = MapsFunctions(geoApiContext)


        findViewById<Button>(R.id.reload_button).setOnClickListener {
            val intent = Intent(this, PlaceSuggestActivity::class.java)

            intent.putExtra(ExtraEnum.GEO_API_KEY.v, getString(R.string.google_maps_key))

            // TODO: requestCodeを直す
            startActivityForResult(intent, 1234)


//            intent.action = Intent.ACTION_VIEW
//            intent.setClassName(
//                "com.google.android.apps.maps",
//                "com.google.android.maps.MapsActivity"
//            )
//
//            intent.data = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=Space+Needle+Seattle+WA&destination=Pike+Place+Market+Seattle+WA&travelmode=bicycling")
//            startActivity(intent)
        }

        findViewById<Button>(R.id.startSearchButton).setOnClickListener {
            getLatLngFromPositionName()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val suggestResult = data!!.getStringExtra(ExtraEnum.SUGGEST_RESULT.v)

        // TODO: departureなのかdestinationなのかわける
        findViewById<EditText>(R.id.departureInput).setText(suggestResult)
    }

    private fun getLatLngFromPositionName() {
        val deptText = findViewById<EditText>(R.id.departureInput).text.toString()
        val destText = findViewById<EditText>(R.id.destinationInput).text.toString()

        Log.d("aaaaa", deptText)
        Log.d("bbbbb", destText)

        GetLatLngFromPositionNameThread(getLatLngFromPosNameHandler, mapsAPI, deptText, destText).start()
    }

    private fun addMarkerAtDeptAndDestPoint(v: Structures.DeptDestLatLng) {
        Log.d("hoge", v.toString())
    }

    private fun addMarker(m: MarkerOptions) {
        mMap.addMarker(m)
    }

    private class HandlerInMapsActivity(activity: MapsActivity) : Handler(Looper.getMainLooper()) {
        private var activityRef: WeakReference<MapsActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = activityRef.get()
            if (activity == null || activity.isFinishing) {
                return
            }

            if (msg.what == WhatEnum.GLLFPN_RESULT.v) {
                // 出発地点，目的地点の名称から取得した座標にピンを建てる
                val po = msg.obj as Structures.DeptDestLatLng
                activity.addMarker(MarkerOptions().position(po.dept).title("出発地").icon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                ))
                activity.addMarker(MarkerOptions().position(po.dest).title("目的地").icon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                ))

                activity.addMarkerAtDeptAndDestPoint(msg.obj as Structures.DeptDestLatLng)

                // TODO: すぐには実行しない
                GetNearestInterChangeThread(this, activity.mapsAPI, activity, po.dept, po.dest).start()
            } else if (msg.what == WhatEnum.NIC_RESULT.v) {
                // 出発地点，目的地点に最も近いICにピンを建てる
                val po = msg.obj as List<*>
                val icNearestToDept = po[0] as Structures.LatLngWithName
                val icNearestToDest = po[1] as Structures.LatLngWithName

                activity.addMarker(MarkerOptions().position(icNearestToDept.point).title(icNearestToDept.name).icon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
                ))
                activity.addMarker(MarkerOptions().position(icNearestToDest.point).title(icNearestToDest.name).icon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                ))

                // TODO: すぐには実行しない
                GetMinimumPathThread(this, activity, icNearestToDept.name, icNearestToDest.name).start()
            } else if (msg.what == WhatEnum.MP_RESULT.v) {
                val po = msg.obj as ArrayList<String>

                // TODO: ダミーデータ
                val deptLatLng = LatLng(35.6124215, 139.6253779)
                val destLatLng = LatLng(34.9792769, 138.3786288)
                val deptTime = Instant.now()
                val arrivalTime = Instant.now().plusSeconds(3600 * 3)


                // TODO: すぐには実行しない
                GetOptimalHighwaySectionThread(this, activity.mapsAPI, po, deptLatLng, destLatLng, deptTime, arrivalTime).start()
            }
        }
    }

    private class GetLatLngFromPositionNameThread(
            handler: HandlerInMapsActivity,
            mapsAPI: MapsFunctions,
            deptText: String,
            destText: String
    ) : Thread() {
        // 入力された地点名から，緯度経度を取得するスレッド
        private val mapsAPI = mapsAPI
        private val handler = handler
        private val deptText = deptText
        private val destText = destText

        override fun run() {
            val latLngs = mapsAPI.obtainLatLngFromPositionName(deptText, destText)
            handler.sendMessage(handler.obtainMessage(WhatEnum.GLLFPN_RESULT.v, latLngs))
        }
    }

    private class GetNearestInterChangeThread(
            handler: HandlerInMapsActivity,
            mapsAPI: MapsFunctions,
            activity: Activity,
            deptCoordinate: LatLng,
            destCoordinate: LatLng
    ) : Thread() {
        // 出発地点と目的地点に最も近いインタチェンジを取得するスレッド
        private val handler = handler
        private val activity = activity
        private val mapsAPI = mapsAPI
        private val deptCoordinate = deptCoordinate
        private val destCoordinate = destCoordinate

        override fun run() {
            // TODO: API消費量を抑へるためにダミーデータにしてゐる
//            val deptNearestIC = mapsAPI.obtainNearestInterChange(activity, deptCoordinate)
//            val destNearestIC = mapsAPI.obtainNearestInterChange(activity, destCoordinate)
            val deptNearestIC = Structures.LatLngWithName("東京", LatLng(35.6124215,139.6253779))
            val destNearestIC = Structures.LatLngWithName("静岡", LatLng(34.9792769,138.3786288))

            handler.sendMessage(handler.obtainMessage(WhatEnum.NIC_RESULT.v, listOf(deptNearestIC, destNearestIC)))
        }
    }

    private class GetMinimumPathThread(
            handler: HandlerInMapsActivity,
            activity: Activity,
            inICName: String,
            outICName: String
    ) : Thread() {
        // 流入ICから流出ICまでの最も短い高速道路の経路を探索するスレッド
        private val handler = handler
        private val activity = activity
        private val inICName = inICName
        private val outICName = outICName

        override fun run() {
            val gf = GraphFunctions(activity)
            val minimumPath = gf.searchMinimumPath(inICName, outICName)

            Log.d("a", minimumPath.toString())
            handler.sendMessage(handler.obtainMessage(WhatEnum.MP_RESULT.v, minimumPath))
        }
    }


    private class GetOptimalHighwaySectionThread(
            handler: HandlerInMapsActivity,
            mapsAPI: MapsFunctions,
            icPath: ArrayList<String>,
            deptLatLng: LatLng,
            destLatLng: LatLng,
            deptTime: Instant,
            arrivalTime: Instant
    ) : Thread() {
        // 入力された地点名から，緯度経度を取得するスレッド
        private val mapsAPI = mapsAPI
        private val handler = handler
        private val icPath = icPath
        private val deptLatLng = deptLatLng
        private val destLatLng = destLatLng
        private val deptTime = deptTime
        private val arrivalTime = arrivalTime

        override fun run() {
            val optSection = mapsAPI.obtainHighwaySection(icPath, deptLatLng, destLatLng, deptTime, arrivalTime)
            handler.sendMessage(handler.obtainMessage(WhatEnum.OPT_SEC_RESULT.v, optSection))
        }
    }



    private fun moveCameraToCurrentPosition() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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