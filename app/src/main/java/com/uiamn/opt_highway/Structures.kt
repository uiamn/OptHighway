package com.uiamn.opt_highway

import com.google.android.gms.maps.model.LatLng

class Structures {
    data class LatLngWithName(val name: String, val point: LatLng)
    data class HighwaySection(val entryIC: LatLngWithName, val outIC: LatLngWithName, val toll: Long)
}


