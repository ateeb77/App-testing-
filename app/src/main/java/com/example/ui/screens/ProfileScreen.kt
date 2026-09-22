package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DemoLocation
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel
) {
    val userRole by viewModel.userRole.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val userLat by viewModel.userLat.collectAsState()
    val userLon by viewModel.userLon.collectAsState()
    val locationLabel by viewModel.locationLabel.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrateEnabled by viewModel.vibrateEnabled.collectAsState()
    val syncChannel by viewModel.syncChannel.collectAsState()
    val trustLevel by viewModel.trustLevel.collectAsState()

    var customChannelInput by remember(syncChannel) { mutableStateOf(syncChannel) }
    var showChannelDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
            .testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // User Profile Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SlateNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Citizen Responder",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            text = "Current Role: ${userRole.title}",
                            fontSize = 13.sp,
                            color = SlateNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Trust Level Bar
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Trust Score: $trustLevel%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = { trustLevel / 100f },
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SafeGreen,
                                trackColor = NeutralLight
                            )
                        }
                    }
                }
            }
        }

        // Switch Active Device Role
        item {
            Text(
                text = "DEVICE ROLE (FOR TWO-PHONE DEMO)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateNavy,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (role in UserRole.entries) {
                    val isSelected = role == userRole
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SlateNavy else PureWhite,
                        onClick = { viewModel.setUserRole(role) },
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = role.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PureWhite else DeepNavy
                                )
                                Text(
                                    text = role.description,
                                    fontSize = 12.sp,
                                    color = if (isSelected) PureWhite.copy(alpha = 0.8f) else NeutralMedium
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentTeal)
                            }
                        }
                    }
                }
            }
        }

        // Demo Location System Control Panel
        item {
            Text(
                text = "DEMO LOCATION SYSTEM",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateNavy,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Simulated GPS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Switch(
                            checked = isDemoMode,
                            onCheckedChange = { viewModel.toggleDemoMode(it) }
                        )
                    }

                    Text(
                        text = "Current: $locationLabel (${String.format("%.4f", userLat)}, ${String.format("%.4f", userLon)})",
                        fontSize = 12.sp,
                        color = NeutralMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.setDemoLocation(DemoLocation.REPORTER_DEFAULT) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SET REPORTER LOCATION (21.1458, 79.0882)", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.setDemoLocation(DemoLocation.RESIDENT_DEFAULT) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SET RESIDENT LOCATION (21.1480, 79.0900 - Inside 420m)", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.setDemoLocation(DemoLocation.OUTSIDE_DEFAULT) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SET OUTSIDE-ZONE LOCATION (21.1800, 79.1200 - Safe)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Real-time Internet Sync Channel Setup (Two Phones)
        item {
            Text(
                text = "REAL-TIME INTERNET SYNC CHANNEL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateNavy,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connected Sync Room: $syncChannel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Both Phone A and Phone B should connect to this channel to exchange incidents in real-time over the internet.",
                        fontSize = 12.sp,
                        color = NeutralMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showChannelDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Sync Channel Room Code", fontSize = 12.sp)
                    }
                }
            }
        }

        // Emergency Alert Audio & Haptics
        item {
            Text(
                text = "ALERT SOUND & NOTIFICATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateNavy,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Emergency Chime Sound", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { viewModel.setSoundEnabled(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration Alert", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Switch(
                            checked = vibrateEnabled,
                            onCheckedChange = { viewModel.setVibrateEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.testEmergencyChime() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test 3-Tone Emergency Chime", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notification behavior depends on your device and operating-system permissions.",
                        fontSize = 11.sp,
                        color = NeutralMedium
                    )
                }
            }
        }

        // About & Public Safety Notice
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "JAGRUK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepNavy
                    )
                    Text(
                        text = "Know danger. Nearby. In time.",
                        fontSize = 12.sp,
                        color = NeutralMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Jagruk is a citizen-powered, AI-assisted hyperlocal emergency intelligence platform. It helps communities report emerging hazards, corroborates nearby reports, creates dynamic danger zones, and delivers actionable alerts to people who may be affected. Jagruk is designed to complement official emergency-warning and response systems.",
                        fontSize = 11.sp,
                        color = NeutralDark,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Change Sync Channel Dialog
    if (showChannelDialog) {
        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            title = { Text("Change Mesh Sync Room", fontWeight = FontWeight.Bold, color = DeepNavy) },
            text = {
                Column {
                    Text(
                        text = "Enter a shared room code to synchronize Phone A and Phone B together:",
                        fontSize = 12.sp,
                        color = NeutralDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customChannelInput,
                        onValueChange = { customChannelInput = it },
                        singleLine = true,
                        placeholder = { Text("e.g. hackathon_team_room_1") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customChannelInput.isNotBlank()) {
                            viewModel.setSyncChannel(customChannelInput)
                        }
                        showChannelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChannelDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
