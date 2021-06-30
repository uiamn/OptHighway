package com.uiamn.opt_highway

import android.app.Activity
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.FindPlaceFromTextRequest
import com.google.maps.FindPlaceFromTextRequest.FieldMask
import com.google.maps.PlacesApi
import com.google.maps.DirectionsApi

import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration
import java.time.Instant
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

    private fun getDurationSeconds(result: DirectionsResult): Long {
        return result.routes[0].legs[0].duration.inSeconds
    }


    fun obtainHighwaySection(icPath: ArrayList<String>, deptLatLng: LatLng, destLatLng: LatLng, deptTime: Instant, arrivalTime: Instant): Structures.HighwaySection? {
        val deptLatLngStr = "%f,%f".format(deptLatLng.latitude, deptLatLng.longitude)
        val destLatLngStr = "%f,%f".format(destLatLng.latitude, destLatLng.longitude)
        val icFmt = "%sインターチェンジ"
        val lastICName = icFmt.format(icPath.last())

        // 下道で全て行っても間に合はないかの確認
        var res = DirectionsApi.getDirections(geoApiContext, deptLatLngStr, destLatLngStr)
            .language("ja").arrivalTime(arrivalTime).avoid(DirectionsApi.RouteRestriction.TOLLS).await()
        val secondsExceptTolls = getDurationSeconds(res)

        Log.d("secondsExceptTolls", secondsExceptTolls.toString())

        if(arrivalTime.isAfter(deptTime.plusSeconds(secondsExceptTolls))) {
            // TODO: まともなものを返す
            Log.d("hogehoge", "下道で行っても間に合ひます！")
            return null
        }

        // 初めにICの終点から目的地までの高速道路を使用しない所要時間を取得しておく
        // 到着時間にarriveTimeを指定
        res = DirectionsApi.getDirections(geoApiContext, lastICName, destLatLngStr)
            .language("ja").arrivalTime(arrivalTime).avoid(DirectionsApi.RouteRestriction.TOLLS).await()
        val secondsICToDest = getDurationSeconds(res)

        // limitSecondsは最後のICに到着するまでの制限時間
        val limitSeconds = Duration.between(deptTime, arrivalTime).seconds - secondsICToDest

        Log.d("hogehoge", limitSeconds.toString())

        // 制限時間が0以下なら当然間に合はない
        if(limitSeconds <= 0) {
            Log.d("hogehoge", "あまりに時間設定が厳しすぎます")
            return null
        }

        // 最も近いICから乗って間に合ふか？
        res = DirectionsApi.getDirections(geoApiContext, deptLatLngStr, icFmt.format(icPath[0]))
            .language("ja").departureTime(deptTime).avoid(DirectionsApi.RouteRestriction.TOLLS).await()
        val secondsDeptToFirstIC = getDurationSeconds(res)

        res = DirectionsApi.getDirections(geoApiContext, icFmt.format(icPath[0]), lastICName)
            .language("ja").departureTime(deptTime.plusSeconds(secondsDeptToFirstIC)).await()
        val secondsFirstICToLastIC = getDurationSeconds(res)

        // 間に合はない場合
        if(secondsDeptToFirstIC + secondsFirstICToLastIC > limitSeconds) {
            Log.d("hogehoge", "全て高速道路を使っても間に合ひません")
            return null
        }

        // 現在地点からICまでにかかる時間 + ICから最後のICまでにかかる時間が制限時間内になるICのうち
        // 最も最後に近いICをにぶたんして探す
        var head = 0
        var tail = icPath.size - 1
        var pivot = (head + tail) / 2

        var highwayMeters = 0L

        for(i__ in 0 until tail) { // TODO: whileでもいいけど．．．
            val fmtedICName = icFmt.format(icPath[pivot])
            res = DirectionsApi.getDirections(geoApiContext, deptLatLngStr, fmtedICName)
                .language("ja").departureTime(deptTime).avoid(DirectionsApi.RouteRestriction.TOLLS).await()
            val secondsDeptToEntryIC = getDurationSeconds(res)

            res = DirectionsApi.getDirections(geoApiContext, fmtedICName, lastICName)
                .language("ja").departureTime(deptTime.plusSeconds(secondsDeptToEntryIC)).await()
            highwayMeters = res.routes[0].legs[0].distance.inMeters
            val secondsEntryICToLastIC = getDurationSeconds(res)

            if(secondsDeptToEntryIC + secondsEntryICToLastIC > limitSeconds) {
                // 間に合はない場合
                head = pivot
            } else {
                // 間に合ふ場合
                tail = pivot
            }

            if (tail - head <= 1) break
            pivot = (head + tail) / 2
        }

        Log.d("ENTRY TO HIGHWAY AT", icPath[pivot])
        return null
    }
}