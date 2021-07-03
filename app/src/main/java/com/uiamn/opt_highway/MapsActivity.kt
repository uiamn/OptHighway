package com.uiamn.opt_highway

import android.content.pm.PackageManager
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.DialogInterface
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
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

import com.google.maps.GeoApiContext
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, TimePickerDialog.OnTimeSetListener {

    private lateinit var mMap: GoogleMap
    private var REQUEST_PERMISSION = 1000

    private val DEPT_SUGGEST_REQ = 1234
    private val DEST_SUGGEST_REQ = 1235

    private val WHAT_THREAD_RESULT = 1236

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var callback: LocationCallback

    private lateinit var deptLatLng: LatLng
    private lateinit var destLatLng: LatLng
    private lateinit var deptTime: Instant
    private lateinit var arriveTime: Instant

    private var isSelectedDeptTime = false

    private lateinit var geoApiContext: GeoApiContext
    private lateinit var mapsAPI: MapsFunctions

    private val spinner = ProgressDialog.newInstance("最適な経路を探索しています．．．")

    private val handler = MapsActivity.HandlerInMapsActivity(this)

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

        mapsAPI = MapsFunctions(this, geoApiContext)

        findViewById<EditText>(R.id.departureInput).setOnClickListener {
            val intent = Intent(this, PlaceSuggestActivity::class.java)
            intent.putExtra(ExtraEnum.GEO_API_KEY.v, getString(R.string.google_maps_key))
            startActivityForResult(intent, DEPT_SUGGEST_REQ)
        }

        findViewById<EditText>(R.id.destinationInput).setOnClickListener {
            val intent = Intent(this, PlaceSuggestActivity::class.java)
            intent.putExtra(ExtraEnum.GEO_API_KEY.v, getString(R.string.google_maps_key))
            startActivityForResult(intent, DEST_SUGGEST_REQ)
        }

        findViewById<EditText>(R.id.deptTimeInput).setOnClickListener {
            isSelectedDeptTime = true
            TimePicker().show(supportFragmentManager, "timePicker")
        }

        findViewById<EditText>(R.id.arriveTimeInput).setOnClickListener {
            isSelectedDeptTime = false
            TimePicker().show(supportFragmentManager, "timePicker")
        }

        findViewById<Button>(R.id.reload_button).setOnClickListener {

        }

        findViewById<Button>(R.id.setCurrentPosButton).setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return@setOnClickListener
            }
            locationClient.lastLocation.addOnCompleteListener { task ->
                if(!task.isSuccessful || task.result == null) {
                    Log.e("location", "位置情報の取得に失敗")
                    return@addOnCompleteListener
                }

                val location = task.result
                val ll = LatLng(location.latitude, location.longitude)
                deptLatLng = ll
                findViewById<EditText>(R.id.departureInput).setText("(現在地)")
            }
        }

        findViewById<Button>(R.id.startSearchButton).setOnClickListener {
            val deptArriveTime = getInputtedTime() ?: return@setOnClickListener
            deptTime = deptArriveTime.first
            arriveTime = deptArriveTime.second

            if(::deptLatLng.isInitialized && ::destLatLng.isInitialized) {
//                GetNearestInterChangeThread(handler, mapsAPI, this, deptLatLng, destLatLng)
                spinner.show(supportFragmentManager, "TAG")
                OverAllThread(handler, this, mapsAPI, deptLatLng, destLatLng, deptTime, arriveTime).start()
            } else {
                Toast.makeText(this, "先に出発地と目的地を入力して下さい", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun launchGoogleMapApp(entryICLatLng: LatLng) {
        intent.action = Intent.ACTION_VIEW
        intent.setClassName(
                "com.google.android.apps.maps",
                "com.google.android.maps.MapsActivity"
        )

        intent.data = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving&avoid=tolls,ferries"
                        .format(deptLatLng.latitude, deptLatLng.longitude, entryICLatLng.latitude, entryICLatLng.longitude))
        startActivity(intent)
    }

    private fun launchGoogleMapApp() {
        intent.action = Intent.ACTION_VIEW
        intent.setClassName(
                "com.google.android.apps.maps",
                "com.google.android.maps.MapsActivity"
        )

        intent.data = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving&avoid=tolls,ferries"
                        .format(deptLatLng.latitude, deptLatLng.longitude, destLatLng.latitude, destLatLng.longitude))
        startActivity(intent)
    }

    private fun getInputtedTime(): Pair<Instant, Instant>? {
        val deptTimeText = findViewById<EditText>(R.id.deptTimeInput).text.toString()
        val arriveTimeText = findViewById<EditText>(R.id.arriveTimeInput).text.toString()

        if(deptTimeText == "") {
            Toast.makeText(this, "出発時刻が入力されていません", Toast.LENGTH_LONG).show()
            return null
        } else if(arriveTimeText == "") {
            Toast.makeText(this, "到着時刻が入力されていません", Toast.LENGTH_LONG).show()
            return null
        }

        val nowLocalDate = LocalDate.now()
        val year = nowLocalDate.year
        val month = nowLocalDate.monthValue
        val day = nowLocalDate.dayOfMonth

        val dateText = "%d-%s-%s".format(
                year,
                if(month > 9) month.toString() else "0%d".format(month),
                if(day > 9) day.toString() else "0%d".format(day)
        )

        val deptInstant = Instant.parse("%sT%s:00Z".format(dateText, deptTimeText))
        val arriveInstantTemp = Instant.parse("%sT%s:00Z".format(dateText, arriveTimeText))

        // 日付を超える場合
        val arriveInstant = if(deptInstant.isAfter(arriveInstantTemp)) arriveInstantTemp.plusSeconds(3600 * 24) else arriveInstantTemp

        return Pair(deptInstant, arriveInstant)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == DEPT_SUGGEST_REQ || requestCode == DEST_SUGGEST_REQ) {
            val isDept = requestCode == DEPT_SUGGEST_REQ

            val editTextId = if(isDept) R.id.departureInput else R.id.destinationInput
            data?.let {
                findViewById<EditText>(editTextId).setText(it.getStringExtra(ExtraEnum.SUGGEST_RESULT_NAME.v))
                val lat = it.getDoubleExtra(ExtraEnum.SUGGEST_RESULT_LAT.v, 0.0)
                val lng = it.getDoubleExtra(ExtraEnum.SUGGEST_RESULT_LNG.v, 0.0)
                val latLng = LatLng(lat, lng)
                val marker = MarkerOptions().position(latLng).title(if(isDept) "出発地" else "目的地").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                addMarkerAndZooming(marker)

                if(isDept) {
                    deptLatLng = latLng
                } else {
                    destLatLng = latLng
                }
            }
        }
    }

    private fun addMarker(m: MarkerOptions) {
        mMap.addMarker(m)
    }

    private fun addMarkerAndZooming(m: MarkerOptions) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(m.position))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15F))
        mMap.addMarker(m)
    }

    private class HandlerInMapsActivity(activity: MapsActivity) : Handler(Looper.getMainLooper()) {
        private var activityRef: WeakReference<MapsActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = activityRef.get()
            if (activity == null || activity.isFinishing) {
                return
            }

            if (msg.what == activity.WHAT_THREAD_RESULT) {
                activity.spinner.dismiss()

                if(msg.obj == null) {
                    Toast.makeText(activity, "高速道路を使っても間に合うことができません", Toast.LENGTH_LONG).show()
                    return
                }

                val result = msg.obj as Structures.HighwaySection

                val dialog = if(result.toll == 0L) {
                    // 下道で間に合う場合
                    activity.generateYesNoDialog(
                            "高速道路を使用せずに間に合います．GoogleMapを起動しますか？",
                            {
                                activity.launchGoogleMapApp()
                            },
                            {}
                    )
                } else {
                    activity.generateYesNoDialog(
                            "最適な経路が見つかりました．GoogleMapを起動しますか？",
                            {
                                activity.launchGoogleMapApp(result.entryIC!!.point)
                            },
                            {
                                activity.addMarker(MarkerOptions().position(result.entryIC!!.point).title(result.entryIC.name).icon(
                                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
                                ))
                                activity.addMarker(MarkerOptions().position(result.outIC!!.point).title(result.outIC.name).icon(
                                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                                ))
                                activity.mMap.moveCamera(CameraUpdateFactory.newLatLng(result.entryIC!!.point))
                                activity.mMap.moveCamera(CameraUpdateFactory.zoomTo(15F))
                            }
                    )
                }

                dialog.show()
            }
        }
    }

    private fun generateYesNoDialog(message: String, yesFun: () -> Unit, noFun: () -> Unit): AlertDialog {
        return AlertDialog.Builder(this).setMessage(message)
                .setPositiveButton("はい",
                        DialogInterface.OnClickListener { _, _ ->
                            yesFun()
                        })
                .setNegativeButton("いいえ",
                        DialogInterface.OnClickListener { _, _ ->
                            noFun()
                        })
                .create()
    }

    private class OverAllThread(
            handler: HandlerInMapsActivity,
            activity: MapsActivity,
            mapsAPI: MapsFunctions,
            deptCoordinate: LatLng,
            destCoordinate: LatLng,
            deptTime: Instant,
            arrivalTime: Instant
    ) : Thread() {
        private val handler = handler
        private val activity = activity
        private val mapsAPI = mapsAPI
        private val deptCoordinate = deptCoordinate
        private val destCoordinate = destCoordinate
        private val deptTime = deptTime
        private val arrivalTime = arrivalTime

        override fun run() {
            // 最寄りのICを取得
            val deptNearestIC = mapsAPI.obtainNearestInterChange(deptCoordinate)
            val destNearestIC = mapsAPI.obtainNearestInterChange(destCoordinate)

            Log.d("hoge", deptNearestIC.toString())
            Log.d("hoge", destNearestIC.toString())

            // 最短のパスを取得
            val gf = GraphFunctions(activity)
            val minimumPath = gf.searchMinimumPath(deptNearestIC.name, destNearestIC.name)

            Log.d("hoge", minimumPath.toString())

            // 最も効率の良い高速道路の使ひ方を取得
            val result = mapsAPI.obtainHighwaySection(minimumPath, deptCoordinate, destCoordinate, deptTime, arrivalTime)
            handler.sendMessage(handler.obtainMessage(activity.WHAT_THREAD_RESULT, result))
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

    override fun onTimeSet(view: android.widget.TimePicker?, hourOfDay: Int, minute: Int) {
        findViewById<EditText>(if(isSelectedDeptTime)R.id.deptTimeInput else R.id.arriveTimeInput)
                .setText("%s:%s".format(
                        if(hourOfDay > 9) hourOfDay.toString() else "0%d".format(hourOfDay),
                        if(minute > 9) minute.toString() else "0%d".format(minute)
                ))
    }

    class ProgressDialog: DialogFragment() {
        companion object {
            fun newInstance(message: String): ProgressDialog {
                val instance = ProgressDialog()
                val arguments = Bundle()
                arguments.putString("message", message)
                instance.arguments = arguments
                return instance
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val mMessage = arguments!!.getString("message")

            val builder = AlertDialog.Builder(activity!!)
            val inflater = activity!!.layoutInflater
            val view = inflater.inflate(R.layout.dialog_progress, null)
            val mMessageTextView = view.findViewById(R.id.progress_message) as TextView
            mMessageTextView.text = mMessage
            builder.setView(view)
            return builder.create()
        }
    }
}