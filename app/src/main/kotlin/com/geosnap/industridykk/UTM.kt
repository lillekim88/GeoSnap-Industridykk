
package com.geosnap.industridykk

import kotlin.math.*

data class UTM(val zone: Int, val band: Char, val easting: Double, val northing: Double) {
    override fun toString(): String {
        fun fmt(x: Double) = String.format(Locale.US, "%,.0f", x).replace(",", " ")
        return String.format(Locale.US, "%d%s %s %s", zone, band, fmt(easting), fmt(northing))
    }
}

object UTMConverter {
    private const val a = 6378137.0 // WGS84 major axis
    private const val f = 1 / 298.257223563
    private const val k0 = 0.9996
    private val e2 = f * (2 - f)

    fun fromLatLon(lat: Double, lon: Double): UTM {
        val zone = floor((lon + 180) / 6) + 1
        val lon0 = Math.toRadians((zone * 6 - 183))
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val ePrime2 = e2 / (1 - e2)
        val N = a / sqrt(1 - e2 * sin(latRad).pow(2))
        val T = tan(latRad).pow(2)
        val C = ePrime2 * cos(latRad).pow(2)
        val A = cos(latRad) * (lonRad - lon0)
        val M = a * ((1 - e2/4 - 3*e2*e2/64 - 5*e2*e2*e2/256) * latRad
                - (3*e2/8 + 3*e2*e2/32 + 45*e2*e2*e2/1024) * sin(2*latRad)
                + (15*e2*e2/256 + 45*e2*e2*e2/1024) * sin(4*latRad)
                - (35*e2*e2*e2/3072) * sin(6*latRad))
        val easting = k0 * N * (A + (1 - T + C) * A.pow(3) / 6 + (5 - 18*T + T*T + 72*C - 58*ePrime2) * A.pow(5) / 120) + 500000.0
        var northing = k0 * (M + N * tan(latRad) * (A*A/2 + (5 - T + 9*C + 4*C*C) * A.pow(4)/24 + (61 - 58*T + T*T + 600*C - 330*ePrime2) * A.pow(6)/720))
        val band = latitudeBand(lat)
        if (lat < 0) northing += 10000000.0
        return UTM(zone.toInt(), band, easting, northing)
    }

    private fun latitudeBand(lat: Double): Char {
        val bands = "CDEFGHJKLMNPQRSTUVWX" // X is 80-84N
        val idx = ((lat + 80) / 8).toInt().coerceIn(0, bands.length - 1)
        return bands[idx]
    }
}

fun UTM.Companion.fromLatLon(lat: Double, lon: Double) = UTMConverter.fromLatLon(lat, lon)

object Formatters {
    fun cardinalFrom(bearingDeg: Float): String {
        val b = ((bearingDeg % 360 + 360) % 360)
        return when (b) {
            in 315f..360f, in 0f..45f -> "N"
            in 45f..135f -> "Ø"
            in 135f..225f -> "S"
            else -> "V"
        }
    }
}
