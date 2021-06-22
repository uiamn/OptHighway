package com.uiamn.opt_highway

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.FindPlaceFromTextRequest
import com.google.maps.FindPlaceFromTextRequest.FieldMask
import com.google.maps.PlacesApi

import com.google.maps.GeoApiContext

class GetLatLngFromPositionName(geoApiContext: GeoApiContext) {
    private val geoApiContext = geoApiContext

    fun getLatLngFromPositionName(deptText: String, destText: String): DeptDestLatLng {
        Log.d("aaaaa", deptText)
        Log.d("bbbbb", destText)

        val deptRes = PlacesApi.findPlaceFromText(
            geoApiContext,
            deptText,
            FindPlaceFromTextRequest.InputType.TEXT_QUERY
        ).language("ja").fields(FieldMask.GEOMETRY).await()
        val destRes = PlacesApi.findPlaceFromText(
            geoApiContext,
            destText,
            FindPlaceFromTextRequest.InputType.TEXT_QUERY
        ).language("ja").fields(FieldMask.GEOMETRY).await()

        Log.d("deptRes", deptRes.candidates[0].toString())

        val deptLatLng = deptRes.candidates[0].geometry.location
        val destLatLng = destRes.candidates[0].geometry.location

        return DeptDestLatLng(LatLng(deptLatLng.lat, deptLatLng.lng), LatLng(destLatLng.lat, destLatLng.lng))
    }
}