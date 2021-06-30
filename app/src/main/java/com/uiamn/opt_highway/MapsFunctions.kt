package com.uiamn.opt_highway

import android.app.Activity
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.FindPlaceFromTextRequest
import com.google.maps.FindPlaceFromTextRequest.FieldMask
import com.google.maps.PlacesApi

import com.google.maps.GeoApiContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalTime

class MapsFunctions(geoApiContext: GeoApiContext) {
    private val geoApiContext = geoApiContext

    fun obtainLatLngFromPositionName(deptText: String, destText: String): Structures.DeptDestLatLng {
        // TODO: APIのリクエスト数を抑へるためにダミーデータ(二子玉川駅と駿府城公園)を返してゐる
        return Structures.DeptDestLatLng(
            LatLng(35.6124215, 139.6253779),
            LatLng(34.9792769, 138.3786288)
        )


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

        val deptLatLng = deptRes.candidates[0].geometry.location
        val destLatLng = destRes.candidates[0].geometry.location

        return Structures.DeptDestLatLng(
            LatLng(deptLatLng.lat, deptLatLng.lng),
            LatLng(destLatLng.lat, destLatLng.lng)
        )
    }

    fun obtainNearestInterChange(activity: Activity, latLng: LatLng): Structures.LatLngWithName {
        val inputStream = activity.resources.assets.open("final_interchanges.json")
        val br = BufferedReader(InputStreamReader(inputStream))
        val jsonText = br.readText()
        val interChanges = JSONArray(jsonText)

        fun sqDist(lat: Double, lng: Double): Double =
            Math.pow((lat - latLng.latitude), 2.0) + Math.pow((lng - latLng.longitude), 2.0)

        var minDist = Double.MAX_VALUE
        var minIndex = 0

        for (i in 0 until interChanges.length()) {
            val ic = interChanges.getJSONObject(i)
            val point = ic.getJSONArray("point")
            // NOTE: 経度緯度の順で入ってゐることに注意！
            val dist = sqDist(point.getDouble(1), point.getDouble(0))

            if (dist < minDist) {
                minDist = dist
                minIndex = i
            }
        }
        val nearestIC = interChanges.getJSONObject(minIndex)
        val name = nearestIC.getString("name")
        val point = nearestIC.getJSONArray("point")

        return Structures.LatLngWithName(name, LatLng(point.getDouble(1), point.getDouble(0)))
    }

    fun obtainHighwaySection(icPath: ArrayList<String>, deptTime: LocalTime, arriveTime: LocalTime) {

    }
}