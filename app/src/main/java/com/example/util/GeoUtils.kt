package com.example.util

import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates great-circle distance between two GPS coordinates using Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val originLatRad = Math.toRadians(lat1)
        val destLatRad = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) +
                sin(dLon / 2).pow(2) * cos(originLatRad) * cos(destLatRad)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Checks whether a resident is inside an incident's danger geofence zone.
     */
    fun isInsideGeofence(
        residentLat: Double, residentLon: Double,
        incidentLat: Double, incidentLon: Double,
        radiusMeters: Double
    ): Boolean {
        val distance = calculateDistanceMeters(residentLat, residentLon, incidentLat, incidentLon)
        return distance <= radiusMeters
    }

    /**
     * Formats distance into clean human-readable text.
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.roundToInt()}m"
        } else {
            String.format("%.1fkm", meters / 1000.0)
        }
    }

    /**
     * Formats relative time.
     */
    fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        if (diff < 0) return "Just now"
        val seconds = diff / 1000
        if (seconds < 60) return "${seconds.coerceAtLeast(1)}s ago"
        val minutes = seconds / 60
        if (minutes < 60) return "${minutes}m ago"
        val hours = minutes / 60
        if (hours < 24) return "${hours}h ago"
        val days = hours / 24
        return "${days}d ago"
    }
}
