package com.uiamn.opt_highway

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class PlaceSuggestActivity : AppCompatActivity() {
    private val TAG = PlaceSuggestActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placesuggest)

        Log.d("hoge", "onCreate")

        val po = arrayListOf("hoge", "fuga", "piyo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, po)

        val listView = findViewById<ListView>(R.id.suggestedPlaces)
        listView.adapter = adapter
    }

}