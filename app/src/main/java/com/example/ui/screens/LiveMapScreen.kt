package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Confidence
import com.example.data.model.Incident
import com.example.data.model.IncidentStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.GeoUtils
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(
    viewModel: MainViewModel,
    onReportClick: () -> Unit
) {
    val activeIncidents by viewModel.activeIncidents.collectAsState()
    val userLat by viewModel.userLat.collectAsState()
    val userLon by viewModel.userLon.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val safetyState by viewModel.safetyState.collectAsState()

    var selectedIncident by remember { mutableStateOf<Incident?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )

    // Center map initially or when reset
    fun centerOnUser() {
        zoomScale = 1.0f
        panOffsetX = 0f
        panOffsetY = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("live_map_screen")
    ) {
        // Main Interactive Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.4f, 4.0f)
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                    }
                }
        ) {
            val canvasCenterX = size.width / 2f + panOffsetX
            val canvasCenterY = size.height / 2f + panOffsetY

            // 1 degree latitude ~ 111,000 meters
            // Scale: 1 meter in pixels at zoomScale 1.0
            val pixelsPerMeter = (size.width / 2400f) * zoomScale

            // Draw Background Grid / Radar circles
            val maxRadarRadius = size.width * 1.5f * zoomScale
            var ringDist = 250f * pixelsPerMeter
            while (ringDist < maxRadarRadius) {
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = ringDist,
                    center = Offset(canvasCenterX, canvasCenterY),
                    style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                )
                ringDist += 500f * pixelsPerMeter
            }

            // Draw Crosshairs
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, canvasCenterY),
                end = Offset(size.width, canvasCenterY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(canvasCenterX, 0f),
                end = Offset(canvasCenterX, size.height),
                strokeWidth = 1.5f
            )

            // Draw Active Incidents
            for (incident in activeIncidents) {
                // Calculate pixel offset from user (center of map)
                val dLat = incident.latitude - userLat
                val dLon = incident.longitude - userLon

                // Approximate meters
                val metersNorth = dLat * 111320.0
                val metersEast = dLon * (40075000.0 * cos(Math.toRadians(userLat)) / 360.0)

                val incPixelX = canvasCenterX + (metersEast * pixelsPerMeter).toFloat()
                val incPixelY = canvasCenterY - (metersNorth * pixelsPerMeter).toFloat()

                val radiusPixels = (incident.radiusMeters * pixelsPerMeter).toFloat()

                // Danger zone fill
                val zoneColor = when (incident.confidence) {
                    Confidence.VERIFIED -> DangerRed
                    Confidence.HIGH -> DangerRed
                    Confidence.MEDIUM -> WarningAmber
                    Confidence.LOW -> Color(0xFFEAB308)
                }

                // Outer animated pulsing circle
                drawCircle(
                    color = zoneColor.copy(alpha = 0.15f),
                    radius = radiusPixels * radarPulse,
                    center = Offset(incPixelX, incPixelY)
                )

                // Main danger perimeter
                drawCircle(
                    color = zoneColor.copy(alpha = 0.35f),
                    radius = radiusPixels,
                    center = Offset(incPixelX, incPixelY)
                )
                drawCircle(
                    color = zoneColor,
                    radius = radiusPixels,
                    center = Offset(incPixelX, incPixelY),
                    style = Stroke(width = 3f)
                )

                // Distance Line connecting User to Incident
                drawLine(
                    color = zoneColor.copy(alpha = 0.7f),
                    start = Offset(canvasCenterX, canvasCenterY),
                    end = Offset(incPixelX, incPixelY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // Incident Center Marker
                drawCircle(
                    color = PureWhite,
                    radius = 20f,
                    center = Offset(incPixelX, incPixelY)
                )
                drawCircle(
                    color = zoneColor,
                    radius = 16f,
                    center = Offset(incPixelX, incPixelY)
                )

                // Draw Text Label on Canvas
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    drawText(
                        "${incident.hazardType.emoji} ${incident.hazardType.displayName} (${incident.radiusMeters.toInt()}m)",
                        incPixelX,
                        incPixelY - 26f,
                        paint
                    )
                }
            }

            // Draw User Marker at Center
            val userMarkerColor = if (safetyState.isSafe) SafeGreen else DangerRed

            // User range pulse
            drawCircle(
                color = userMarkerColor.copy(alpha = 0.2f),
                radius = 45f * radarPulse,
                center = Offset(canvasCenterX, canvasCenterY)
            )
            // Outer white ring
            drawCircle(
                color = PureWhite,
                radius = 18f,
                center = Offset(canvasCenterX, canvasCenterY)
            )
            // Inner colored beacon
            drawCircle(
                color = userMarkerColor,
                radius = 14f,
                center = Offset(canvasCenterX, canvasCenterY)
            )

            // User label
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                drawText("YOU (${if (safetyState.isSafe) "SAFE" else "INSIDE ZONE"})", canvasCenterX, canvasCenterY + 40f, paint)
            }
        }

        // Top Status & Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Safety Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (safetyState.isSafe) SafeGreen else DangerRed,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (safetyState.isSafe) "🟢 YOU ARE SAFE" else "🔴 INSIDE DANGER ZONE",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Demo Location Badge
                if (isDemoMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateNavy.copy(alpha = 0.9f),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = "📍 DEMO GPS",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = AccentTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Threats Count Bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DeepNavy.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Incidents: ${activeIncidents.size}",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Zoom: ${(zoomScale * 100).toInt()}% • Drag to pan",
                        color = AccentTeal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Map Control Floating Buttons on Right
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(
                onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(4.0f) },
                modifier = Modifier.size(44.dp),
                containerColor = SlateNavy,
                contentColor = PureWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            FloatingActionButton(
                onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.4f) },
                modifier = Modifier.size(44.dp),
                containerColor = SlateNavy,
                contentColor = PureWhite
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            FloatingActionButton(
                onClick = { centerOnUser() },
                modifier = Modifier.size(44.dp),
                containerColor = AccentTeal,
                contentColor = DeepNavy
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on User")
            }
        }

        // Bottom Incident Selector / Sheet
        if (activeIncidents.isNotEmpty()) {
            val incident = selectedIncident ?: activeIncidents.first()
            val dist = GeoUtils.calculateDistanceMeters(
                userLat, userLon,
                incident.latitude, incident.longitude
            )
            val isInside = dist <= incident.radiusMeters

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { selectedIncident = incident }
                    .testTag("map_incident_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isInside) DangerRedContainer else WarningAmberContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(incident.hazardType.emoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = incident.hazardType.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NeutralDark
                                )
                                Text(
                                    text = "${GeoUtils.formatDistance(dist)} away • Radius: ${incident.radiusMeters.toInt()}m",
                                    fontSize = 13.sp,
                                    color = if (isInside) DangerRed else NeutralMedium,
                                    fontWeight = if (isInside) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(incident.confidence.colorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = incident.confidence.label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(incident.confidence.colorHex),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = incident.description,
                        fontSize = 13.sp,
                        color = NeutralDark,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.corroborateIncident(incident.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Corroborate (${incident.corroboratingReports})", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.reportMistake(incident.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                        ) {
                            Text("Report Mistake", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
