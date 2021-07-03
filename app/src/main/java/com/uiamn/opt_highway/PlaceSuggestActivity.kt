package com.uiamn.opt_highway

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import com.google.maps.*
import java.lang.ref.WeakReference

class PlaceSuggestActivity : AppCompatActivity() {
    private val TAG = PlaceSuggestActivity::class.java.simpleName
    private lateinit var geoApiContext: GeoApiContext
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var token: PlaceAutocompleteRequest.SessionToken
    private lateinit var selectedName: String
    private var suggestList = listOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placesuggest)

        geoApiContext = GeoApiContext.Builder().apiKey(intent.getStringExtra(ExtraEnum.GEO_API_KEY.v)).build()

        Log.d("hoge", "onCreate")

        findViewById<Button>(R.id.reqSuggestButton).setOnClickListener {
            val handler = SuggestHandler(this)
            val query = findViewById<EditText>(R.id.searchWordInput).text.toString()

            if(query != "") {
                token = PlaceAutocompleteRequest.SessionToken()
                SuggestThread(geoApiContext, handler, query, token).start()
            }
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayListOf())
        val listView = findViewById<ListView>(R.id.suggestedPlaces)
        listView.adapter = adapter

        listView.setOnItemClickListener {parent, _, pos, _ ->
            selectedName = parent.getItemAtPosition(pos).toString()

            val handler = SuggestHandler(this)
            PlaceLatLngThread(geoApiContext, handler, selectedName, token).start()
        }
    }

    private fun backToMainActivity(latLng: LatLng) {
        val data = Intent()
        data.putExtra(ExtraEnum.SUGGEST_RESULT_NAME.v, selectedName)
        data.putExtra(ExtraEnum.SUGGEST_RESULT_LAT.v, latLng.latitude)
        data.putExtra(ExtraEnum.SUGGEST_RESULT_LNG.v, latLng.longitude)
        setResult(RESULT_OK, data)
        finish()
    }

    private fun updateSuggestedList(suggested: List<Pair<String, String>>) {
        adapter.clear()
        suggestList = suggested

        val names = suggested.map{
            it.first
        }

        adapter.addAll(names)
        adapter.notifyDataSetChanged()
    }

    private class SuggestHandler(activity: PlaceSuggestActivity): Handler(Looper.getMainLooper()) {
        private val activityRef = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            val activity = activityRef.get()
            if (activity == null || activity.isFinishing) {
                return
            }

            if (msg.what == WhatEnum.SUGGEST_RESULT.v) {
                val suggestedPlaces = msg.obj as List<Pair<String, String>>
                activity.updateSuggestedList(suggestedPlaces)
            } else if (msg.what == WhatEnum.PLACE_SUGGEST_LATLNG_RESULT.v) {
                val latLng = msg.obj as LatLng
                activity.backToMainActivity(latLng)
            }
        }
    }

    private class SuggestThread(
            geoApiContext: GeoApiContext,
            handler: SuggestHandler,
            query: String,
            token: PlaceAutocompleteRequest.SessionToken
    ) : Thread() {
        private val geoApiContext = geoApiContext
        private val handler = handler
        private val query = query
        private val token = token

        override fun run() {
            val response = PlacesApi.placeAutocomplete(geoApiContext, query, token)
                    .language("ja")
                    .await()

            val res = response.map {
                Pair(it.description, it.placeId)
            }

            Log.d("hoge", res.toString())

            handler.sendMessage(handler.obtainMessage(WhatEnum.SUGGEST_RESULT.v, res))
        }
    }

    private class PlaceLatLngThread(
            geoApiContext: GeoApiContext,
            handler: SuggestHandler,
//            placeId: String,
            placeText: String, // 本当はPlaceIDで取得したかったが，placeDetailsを使はうとすると例外で落ちる
            token: PlaceAutocompleteRequest.SessionToken
    ) : Thread() {
        private val geoApiContext = geoApiContext
        private val handler = handler
        private val address = placeText.split(' ')[0]
        private val token = token

        override fun run() {
//            val response = PlacesApi.placeDetails(geoApiContext, placeId, token)
//                    .fields(PlaceDetailsRequest.FieldMask.GEOMETRY)
//                    .language("ja")
//                    .await()
            val response = PlacesApi.findPlaceFromText(
                    geoApiContext,
                    address,
                    FindPlaceFromTextRequest.InputType.TEXT_QUERY
            ).language("ja").fields(FindPlaceFromTextRequest.FieldMask.GEOMETRY).await()

            // com.google.maps.model.LatLng と com.google.android.gms.maps.model.LatLng がある
            val loc = response.candidates[0].geometry.location
            val res = LatLng(loc.lat, loc.lng)
            Log.d("hoge", res.toString())


            handler.sendMessage(handler.obtainMessage(WhatEnum.PLACE_SUGGEST_LATLNG_RESULT.v, res))
        }
    }

}