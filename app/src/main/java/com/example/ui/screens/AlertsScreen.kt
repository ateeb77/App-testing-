package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.ReportSource
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.GeoUtils

@Composable
fun AlertsScreen(
    viewModel: MainViewModel,
    onViewOnMap: (Incident) -> Unit
) {
    val allIncidents by viewModel.allIncidents.collectAsState()
    val userLat by viewModel.userLat.collectAsState()
    val userLon by viewModel.userLon.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("ACTIVE", "RESOLVED", "DISMISSED", "OFFICIAL")

    val filteredIncidents = remember(allIncidents, selectedTab) {
        when (selectedTab) {
            0 -> allIncidents.filter { it.status == IncidentStatus.ACTIVE || it.status == IncidentStatus.VERIFIED || it.status == IncidentStatus.UNDER_REVIEW }
            1 -> allIncidents.filter { it.status == IncidentStatus.RESOLVED }
            2 -> allIncidents.filter { it.status == IncidentStatus.DISMISSED }
            3 -> emptyList() // Render simulated official SACHET alerts
            else -> allIncidents
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
            .testTag("alerts_screen")
    ) {
        // Header
        Text(
            text = "Alert Feed & History",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = DeepNavy
        )
        Text(
            text = "Hyperlocal intelligence and community alerts",
            fontSize = 12.sp,
            color = NeutralMedium
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PureWhite,
            contentColor = SlateNavy,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 3) {
            // Simulated Official Alert Feed (SACHET / NDMA Architecture)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = InfoBlueContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SIMULATED OFFICIAL FEED (Future-compatible architecture for NDMA / SACHET integration)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = InfoBlue
                            )
                        }
                    }
                }

                item {
                    OfficialAlertCard(
                        agency = "National Disaster Management Authority (NDMA)",
                        hazard = "Heavy Precipitation & Regional Inundation Warning",
                        level = "ORANGE ALERT",
                        levelColor = WarningAmber,
                        details = "Moderate to intense rainfall expected across regional districts over next 24 hours. Local civic teams activated.",
                        time = "1 hour ago"
                    )
                }

                item {
                    OfficialAlertCard(
                        agency = "Central Water Commission (CWC)",
                        hazard = "River Basin Runoff Advisory",
                        level = "YELLOW ADVISORY",
                        levelColor = Color(0xFFEAB308),
                        details = "Water level in upstream reservoirs at 78% capacity. Controlled release may occur within 12 hours.",
                        time = "3 hours ago"
                    )
                }
            }
        } else if (filteredIncidents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No ${tabTitles[selectedTab].lowercase()} alerts",
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "New alerts broadcast by nearby citizens will appear here in real time.",
                        color = NeutralMedium,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredIncidents) { incident ->
                    val distance = GeoUtils.calculateDistanceMeters(
                        userLat, userLon,
                        incident.latitude, incident.longitude
                    )
                    val isInside = distance <= incident.radiusMeters

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

                            // Status badge & Danger Radius
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (incident.status) {
                                        IncidentStatus.ACTIVE -> SafeGreenContainer
                                        IncidentStatus.VERIFIED -> InfoBlueContainer
                                        IncidentStatus.UNDER_REVIEW -> WarningAmberContainer
                                        IncidentStatus.DISMISSED -> DangerRedContainer
                                        IncidentStatus.RESOLVED -> NeutralLight
                                    }
                                ) {
                                    Text(
                                        text = "Status: ${incident.status.name}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (incident.status) {
                                            IncidentStatus.ACTIVE -> SafeGreen
                                            IncidentStatus.VERIFIED -> InfoBlue
                                            IncidentStatus.UNDER_REVIEW -> WarningAmber
                                            IncidentStatus.DISMISSED -> DangerRed
                                            IncidentStatus.RESOLVED -> NeutralDark
                                        }
                                    )
                                }

                                Text(
                                    text = "Radius: ${incident.radiusMeters.toInt()}m",
                                    fontSize = 11.sp,
                                    color = NeutralMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Community Corroboration Callout
                            if (incident.corroboratingReports > 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceLight,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = SlateNavy, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "COMMUNITY CORROBORATION: ${incident.corroboratingReports} nearby reports within 500m",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateNavy
                                        )
                                    }
                                }
                            }

                            // Action buttons
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.corroborateIncident(incident.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Corroborate", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.reportMistake(incident.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                ) {
                                    Text("Report Mistake", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfficialAlertCard(
    agency: String,
    hazard: String,
    level: String,
    levelColor: Color,
    details: String,
    time: String
) {
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
                Text(
                    text = agency,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeutralMedium
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = levelColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = level,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = levelColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = hazard,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = details,
                fontSize = 13.sp,
                color = NeutralDark,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Issued $time • Official Government Channel",
                fontSize = 11.sp,
                color = NeutralMedium
            )
        }
    }
}
