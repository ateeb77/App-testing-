package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@Composable
fun AdminCommandCenterScreen(
    viewModel: MainViewModel,
    onOpenPresentationMode: () -> Unit
) {
    val stats by viewModel.adminStatistics.collectAsState()
    val allIncidents by viewModel.allIncidents.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showDemoGuide by remember { mutableStateOf(false) }
    var showRadiusDialogForIncident by remember { mutableStateOf<Incident?>(null) }
    var showEscalateDialogForIncident by remember { mutableStateOf<Incident?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
            .testTag("admin_command_center_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "JAGRUK COMMAND CENTER",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepNavy,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Real-time Verification, Geofencing & Control",
                        fontSize = 12.sp,
                        color = NeutralMedium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { showDemoGuide = !showDemoGuide },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showDemoGuide) "Hide Guide" else "Demo Guide", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onOpenPresentationMode,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Judges View", fontSize = 11.sp)
                    }
                }
            }
        }

        // Demo Guide Banner (Collapsible)
        if (showDemoGuide) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = InfoBlueContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📖 10-STEP TWO-PHONE DEMO SCRIPT FOR JUDGES",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = InfoBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val steps = listOf(
                            "1. Phone A sets Role: REPORTER (Location A).",
                            "2. Phone B sets Role: RESIDENT (Location B ~420m away).",
                            "3. Phone A creates Flood report (1000m radius).",
                            "4. Backend sync mesh receives report over internet.",
                            "5. AI/rules perform verification and calculate danger zone.",
                            "6. Phone B detected inside geofence -> immediately alerts!",
                            "7. Phone B plays emergency 3-tone chime + vibrates.",
                            "8. Phone B opens Live Map to see danger perimeter.",
                            "9. Admin Command Center verifies and adjusts danger radius.",
                            "10. Press 'Reset Demo' to restore ready state across both phones."
                        )
                        steps.forEach { step ->
                            Text(
                                text = step,
                                fontSize = 12.sp,
                                color = NeutralDark,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Statistics Grid (5 KPIs)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "ACTIVE INCIDENTS",
                        value = "${stats.activeIncidents}",
                        color = DangerRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "HIGH-RISK",
                        value = "${stats.highRiskIncidents}",
                        color = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "AFFECTED USERS",
                        value = "${stats.affectedUsersCount}",
                        color = AccentTeal,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "PENDING REVIEW",
                        value = "${stats.pendingVerification}",
                        color = Color(0xFFEAB308),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "RESOLVED",
                        value = "${stats.resolvedIncidents}",
                        color = SafeGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Toolbar: Trigger Demo Scenario & Reset Demo
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "HACKATHON CONTROLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerFlashFloodDemo() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("admin_trigger_flood_demo_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🌊 Launch Flood Demo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.resetDemo() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("admin_reset_demo_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🔄 Reset Demo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Incident Management Feed
        item {
            Text(
                text = "Incident Management Feed (${allIncidents.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )
        }

        if (allIncidents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "READY FOR DEMO: No incidents currently in system.",
                            fontWeight = FontWeight.Bold,
                            color = NeutralMedium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(allIncidents) { incident ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                Text(incident.hazardType.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = incident.hazardType.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DeepNavy
                                    )
                                    Text(
                                        text = "${incident.radiusMeters.toInt()}m radius • ${GeoUtils.formatRelativeTime(incident.createdAt)}",
                                        fontSize = 11.sp,
                                        color = NeutralMedium
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(incident.confidence.colorHex).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = incident.confidence.label,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = Color(incident.confidence.colorHex),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = incident.description,
                            fontSize = 13.sp,
                            color = NeutralDark,
                            lineHeight = 17.sp
                        )

                        if (incident.aiAnalysis != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🤖 AI: ${incident.aiAnalysis}",
                                fontSize = 11.sp,
                                color = SlateNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Admin Action Buttons (VERIFY, DISMISS, MODIFY RADIUS, ESCALATE, RESOLVE)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (incident.status != IncidentStatus.VERIFIED) {
                                Button(
                                    onClick = { viewModel.adminVerify(incident.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("Verify", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { showRadiusDialogForIncident = incident },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text("Radius", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showEscalateDialogForIncident = incident },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text("Escalate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (incident.status != IncidentStatus.RESOLVED) {
                                Button(
                                    onClick = { viewModel.adminResolve(incident.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("Resolve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (incident.status != IncidentStatus.DISMISSED) {
                                TextButton(
                                    onClick = { viewModel.adminDismiss(incident.id) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("Dismiss", fontSize = 11.sp, color = DangerRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modify Danger Radius Dialog
    if (showRadiusDialogForIncident != null) {
        val inc = showRadiusDialogForIncident!!
        var currentSliderRadius by remember { mutableFloatStateOf(inc.radiusMeters.toFloat()) }

        AlertDialog(
            onDismissRequest = { showRadiusDialogForIncident = null },
            title = { Text("Modify Danger Radius", fontWeight = FontWeight.Bold, color = DeepNavy) },
            text = {
                Column {
                    Text(
                        text = "Current Radius: ${currentSliderRadius.toInt()} meters",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = currentSliderRadius,
                        onValueChange = { currentSliderRadius = it },
                        valueRange = 100f..3000f,
                        steps = 29
                    )
                    Text(
                        text = "Residents inside this updated radius will automatically receive safety alerts.",
                        fontSize = 11.sp,
                        color = NeutralMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminUpdateRadius(inc.id, currentSliderRadius.toDouble())
                        showRadiusDialogForIncident = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                ) {
                    Text("Apply Radius")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRadiusDialogForIncident = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Escalate to Authorities Dialog
    if (showEscalateDialogForIncident != null) {
        val inc = showEscalateDialogForIncident!!
        AlertDialog(
            onDismissRequest = { showEscalateDialogForIncident = null },
            title = { Text("Escalate to Emergency Authorities", fontWeight = FontWeight.Bold, color = DeepNavy) },
            text = {
                Column {
                    Text(
                        text = "Simulated Emergency Dispatch Bridge:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Transmitting geospatial telemetry to Emergency Response 112\n• Forwarding incident: ${inc.hazardType.displayName}\n• Radius: ${inc.radiusMeters.toInt()}m\n• Affected Citizens: 2 inside geofence",
                        fontSize = 12.sp,
                        color = NeutralDark,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEscalateDialogForIncident = null },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text("Transmit to 112 Dispatch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEscalateDialogForIncident = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = NeutralMedium,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}
