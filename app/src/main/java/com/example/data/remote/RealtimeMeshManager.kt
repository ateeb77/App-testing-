package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.local.IncidentDao
import com.example.data.local.IncidentEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.*
import com.example.util.AlertSoundManager
import com.example.util.GeoUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class ConnectivityStatus(val label: String, val colorHex: Long) {
    ONLINE("ONLINE", 0xFF16A34A),
    WEAK("WEAK CONNECTION", 0xFFD97706),
    OFFLINE("OFFLINE", 0xFFDC2626)
}

class RealtimeMeshManager(
    private val context: Context,
    private val incidentDao: IncidentDao,
    private val preferencesManager: PreferencesManager
) {
    private val tag = "JagrukMesh"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val payloadAdapter = moshi.adapter(SyncPayload::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Infinite for WebSocket
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null

    private val _connectivityStatus = MutableStateFlow(ConnectivityStatus.ONLINE)
    val connectivityStatus: StateFlow<ConnectivityStatus> = _connectivityStatus.asStateFlow()

    private val _criticalAlertIncident = MutableStateFlow<Incident?>(null)
    val criticalAlertIncident: StateFlow<Incident?> = _criticalAlertIncident.asStateFlow()

    private val deviceId = "device_" + System.currentTimeMillis().toString().takeLast(6)

    init {
        registerNetworkCallback()
        startWebSocketListener()
        startPeriodicSyncFallback()
    }

    private fun registerNetworkCallback() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = cm.getNetworkCapabilities(network)
                    val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    val hasCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                    val isWeak = (caps?.linkDownstreamBandwidthKbps ?: 1000) < 300

                    _connectivityStatus.value = if (isWeak) {
                        ConnectivityStatus.WEAK
                    } else if (hasWifi || hasCellular) {
                        ConnectivityStatus.ONLINE
                    } else {
                        ConnectivityStatus.WEAK
                    }
                }

                override fun onLost(network: Network) {
                    _connectivityStatus.value = ConnectivityStatus.OFFLINE
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Network callback registration failed", e)
        }
    }

    fun startWebSocketListener() {
        val channel = preferencesManager.syncChannel.value
        val wsUrl = "wss://ntfy.sh/$channel/ws"

        val request = Request.Builder().url(wsUrl).build()
        webSocket?.close(1000, "Reconnecting")

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connected to channel: $channel")
                _connectivityStatus.value = ConnectivityStatus.ONLINE
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(tag, "Incoming message: $text")
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "WebSocket error: ${t.message}. Retrying in 5 seconds...")
                scope.launch {
                    delay(5000)
                    startWebSocketListener()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed: $reason")
            }
        })
    }

    private fun handleIncomingMessage(rawJson: String) {
        scope.launch {
            try {
                // ntfy.sh wraps messages with event: "message" and message field
                val jsonObject = org.json.JSONObject(rawJson)
                val event = jsonObject.optString("event", "")
                if (event != "message") return@launch

                val innerMessage = jsonObject.optString("message", "")
                if (innerMessage.isBlank()) return@launch

                val payload = payloadAdapter.fromJson(innerMessage) ?: return@launch

                when (payload.type) {
                    "UPSERT" -> {
                        val inc = payload.incident ?: return@launch
                        incidentDao.insertOrUpdate(IncidentEntity.fromIncident(inc))

                        // Check if current user is inside this incident's danger zone!
                        val myLat = preferencesManager.latitude.value
                        val myLon = preferencesManager.longitude.value
                        val isInside = GeoUtils.isInsideGeofence(
                            myLat, myLon,
                            inc.latitude, inc.longitude,
                            inc.radiusMeters
                        )

                        if (isInside && (inc.status == IncidentStatus.ACTIVE || inc.status == IncidentStatus.VERIFIED)) {
                            _criticalAlertIncident.value = inc
                            if (preferencesManager.soundEnabled.value) {
                                AlertSoundManager.playEmergencyAlert(
                                    context,
                                    preferencesManager.vibrateEnabled.value
                                )
                            }
                        }
                    }

                    "STATUS_CHANGE" -> {
                        val incId = payload.incidentId ?: return@launch
                        val newStatus = payload.newStatus ?: return@launch
                        incidentDao.updateStatus(incId, newStatus, payload.timestamp)
                        if (_criticalAlertIncident.value?.id == incId &&
                            (newStatus == IncidentStatus.RESOLVED.name || newStatus == IncidentStatus.DISMISSED.name)
                        ) {
                            _criticalAlertIncident.value = null
                        }
                    }

                    "CORROBORATE" -> {
                        val incId = payload.incidentId ?: return@launch
                        val inc = incidentDao.getIncidentById(incId)
                        if (inc != null) {
                            val count = inc.corroboratingReports + 1
                            val newConf = if (count >= 3) Confidence.HIGH.name else Confidence.MEDIUM.name
                            incidentDao.addCorroboration(incId, newConf, payload.timestamp)
                        }
                    }

                    "RESET" -> {
                        incidentDao.clearAll()
                        _criticalAlertIncident.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse incoming mesh message", e)
            }
        }
    }

    /**
     * Broadcasts an incident or event to all connected devices on the mesh channel.
     */
    fun broadcastPayload(payload: SyncPayload) {
        scope.launch {
            val channel = preferencesManager.syncChannel.value
            val url = "https://ntfy.sh/$channel"
            val jsonBody = payloadAdapter.toJson(payload)

            val requestBody = jsonBody.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Title", "JAGRUK-SYNC")
                .addHeader("Priority", "urgent")
                .build()

            try {
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(tag, "Broadcast successfully sent to channel $channel: ${payload.type}")
                } else {
                    Log.w(tag, "Broadcast response code: ${response.code}")
                }
            } catch (e: IOException) {
                Log.e(tag, "Broadcast network error: ${e.message}")
            }
        }
    }

    /**
     * Periodic sync fallback ensures any missed messages are pulled.
     */
    private fun startPeriodicSyncFallback() {
        scope.launch {
            while (isActive) {
                delay(30000)
                try {
                    val channel = preferencesManager.syncChannel.value
                    val url = "https://ntfy.sh/$channel/json?poll=1&since=30s"
                    val req = Request.Builder().url(url).build()
                    val resp = okHttpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        resp.body?.string()?.lines()?.forEach { line ->
                            if (line.isNotBlank()) {
                                handleIncomingMessage(line)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore fallback polling errors
                }
            }
        }
    }

    fun dismissCriticalAlert() {
        _criticalAlertIncident.value = null
    }

    fun acknowledgeAlert(incidentId: String) {
        scope.launch {
            incidentDao.markAcknowledged(incidentId)
            _criticalAlertIncident.value = null
            broadcastPayload(
                SyncPayload(
                    type = "ACK",
                    incidentId = incidentId,
                    senderId = deviceId
                )
            )
        }
    }
}
