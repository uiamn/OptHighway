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
import com.google.maps.GeoApiContext
import com.google.maps.PlacesApi
import com.google.maps.PlaceAutocompleteRequest
import java.lang.ref.WeakReference

class PlaceSuggestActivity : AppCompatActivity() {
    private val TAG = PlaceSuggestActivity::class.java.simpleName
    private lateinit var geoApiContext: GeoApiContext
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placesuggest)

        geoApiContext = GeoApiContext.Builder().apiKey(intent.getStringExtra(ExtraEnum.GEO_API_KEY.v)).build()

        Log.d("hoge", "onCreate")

        findViewById<Button>(R.id.reqSuggestButton).setOnClickListener {
            val handler = SuggestHandler(this)
            val query = findViewById<EditText>(R.id.searchWordInput).text.toString()

            if(query != "") SuggestThread(geoApiContext, handler, query).start()
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayListOf())
        val listView = findViewById<ListView>(R.id.suggestedPlaces)
        listView.adapter = adapter

        listView.setOnItemClickListener {parent, _, pos, _ ->
            val t = parent.getItemAtPosition(pos).toString()
            val data = Intent()
            data.putExtra(ExtraEnum.SUGGEST_RESULT.v, t)
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private fun updateSuggestedList(suggested: List<String>) {
        adapter.clear()
        adapter.addAll(suggested)
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
                val suggestedPlaces = msg.obj as List<String>
                activity.updateSuggestedList(suggestedPlaces)
            }
        }
    }

    private class SuggestThread(
            geoApiContext: GeoApiContext,
            handler: SuggestHandler,
            query: String
    ) : Thread() {
        // 入力された地点名から，緯度経度を取得するスレッド
        private val geoApiContext = geoApiContext
        private val handler = handler
        private val query = query

        override fun run() {
            val token = PlaceAutocompleteRequest.SessionToken()

            val response = PlacesApi.placeAutocomplete(geoApiContext, query, token)
                    .language("ja")
                    .await()

            val res = response.map {
                it -> it.description
            }

            Log.d("hoge", res.toString())


            handler.sendMessage(handler.obtainMessage(WhatEnum.SUGGEST_RESULT.v, res))
        }
    }

}