package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Confidence
import com.example.data.model.DemoLocation
import com.example.data.model.Incident
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.GeoUtils

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onReportDangerClick: () -> Unit,
    onViewLiveMapClick: () -> Unit,
    onAlertHistoryClick: () -> Unit,
    onEmergencyHelpClick: () -> Unit,
    onOpenProfileClick: () -> Unit
) {
    val userRole by viewModel.userRole.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val locationLabel by viewModel.locationLabel.collectAsState()
    val connectivity by viewModel.connectivityStatus.collectAsState()
    val safetyState by viewModel.safetyState.collectAsState()
    val activeIncidents by viewModel.activeIncidents.collectAsState()
    val userLat by viewModel.userLat.collectAsState()
    val userLon by viewModel.userLon.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(horizontal = 16.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JAGRUK",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepNavy,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Connectivity Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(connectivity.colorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "• ${connectivity.label}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color(connectivity.colorHex),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = "Know danger. Nearby. In time.",
                        fontSize = 13.sp,
                        color = NeutralMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Role Chip / Profile Shortcut
                AssistChip(
                    onClick = onOpenProfileClick,
                    label = { Text(userRole.title, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (userRole) {
                                UserRole.REPORTER -> Icons.Default.Campaign
                                UserRole.RESIDENT -> Icons.Default.Person
                                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SlateNavy.copy(alpha = 0.1f),
                        labelColor = DeepNavy
                    ),
                    border = null
                )
            }
        }

        // Demo Location Banner (When Demo Mode is Active)
        if (isDemoMode) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SlateNavy,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DEMO LOCATION: $locationLabel",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "(${String.format("%.4f", userLat)}, ${String.format("%.4f", userLon)})",
                            color = AccentTeal,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Main Safety Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("safety_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (safetyState.isSafe) SafeGreenContainer else DangerRedContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (safetyState.isSafe) SafeGreen else DangerRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (safetyState.isSafe) Icons.Default.Shield else Icons.Default.Warning,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (safetyState.isSafe) "🟢 YOU ARE SAFE" else "🔴 DANGER ZONE DETECTED",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (safetyState.isSafe) SafeGreen else DangerRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (safetyState.isSafe) {
                                "No active emergency hazards reported inside your geofence radius."
                            } else {
                                "${safetyState.nearestIncident?.hazardType?.displayName ?: "Emergency"} reported ${GeoUtils.formatDistance(safetyState.distanceMeters)} from you."
                            },
                            fontSize = 13.sp,
                            color = NeutralDark,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Primary Action: 🚨 REPORT DANGER
        item {
            Button(
                onClick = onReportDangerClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .testTag("report_danger_primary_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🚨",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "REPORT DANGER",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = PureWhite
                    )
                }
            }
        }

        // Secondary Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewLiveMapClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_view_map_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateNavy)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIVE MAP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAlertHistoryClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_alert_history_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateNavy)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ALERTS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onEmergencyHelpClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_emergency_help_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp), tint = PureWhite)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HELP 112", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }
        }

        // Quick Two-Phone Demo Control Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeutralLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ TWO-PHONE LIVE DEMO SHORTCUTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateNavy,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Room: ${viewModel.syncChannel.value.take(12)}...",
                            fontSize = 10.sp,
                            color = NeutralMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = userRole == UserRole.REPORTER,
                            onClick = { viewModel.setUserRole(UserRole.REPORTER) },
                            label = { Text("Phone A (Reporter)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = userRole == UserRole.RESIDENT,
                            onClick = { viewModel.setUserRole(UserRole.RESIDENT) },
                            label = { Text("Phone B (Resident)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.triggerFlashFloodDemo() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("trigger_flash_flood_demo_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "🌊 Trigger 'FLASH FLOOD — LIVE DEMO' Scenario",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = DeepNavy
                        )
                    }
                }
            }
        }

        // Nearby Alerts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Emergency Incidents (${activeIncidents.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
                if (activeIncidents.isNotEmpty()) {
                    Text(
                        text = "Live Mesh Sync",
                        fontSize = 11.sp,
                        color = SafeGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (activeIncidents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🛡️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "All Clear in Your Area",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DeepNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No emergency hazards reported nearby. Press 'Report Danger' above or trigger the live demo scenario to test two-phone communication.",
                            fontSize = 13.sp,
                            color = NeutralMedium,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            items(activeIncidents) { incident ->
                val distance = GeoUtils.calculateDistanceMeters(
                    userLat, userLon,
                    incident.latitude, incident.longitude
                )
                val isInside = distance <= incident.radiusMeters

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("incident_item_${incident.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isInside) DangerRedContainer else WarningAmberContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(incident.hazardType.emoji, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = incident.hazardType.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = DeepNavy
                                    )
                                    Text(
                                        text = "${GeoUtils.formatDistance(distance)} away • ${GeoUtils.formatRelativeTime(incident.createdAt)}",
                                        fontSize = 12.sp,
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
                            lineHeight = 18.sp
                        )

                        if (incident.corroboratingReports > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceLight
                            ) {
                                Text(
                                    text = "👥 Community Corroboration: ${incident.corroboratingReports} independent reports",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = SlateNavy,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Government Complement Statement
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantLight
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PUBLIC SAFETY NOTICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeutralMedium,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Jagruk is designed to complement official emergency-warning infrastructure such as India's SACHET and emergency response number 112. It focuses on community-level, hyperlocal hazard reporting and early awareness.",
                        fontSize = 11.sp,
                        color = NeutralMedium,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
