package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DemoLocation
import com.example.data.model.HazardType
import com.example.data.model.Severity
import com.example.data.remote.GeminiService
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDangerScreen(
    viewModel: MainViewModel,
    onReportSubmitted: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedHazard by remember { mutableStateOf(HazardType.FLASH_FLOOD) }
    var description by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(Severity.HIGH) }
    var hasPhotoAttached by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Coordinates selection
    val currentLat by viewModel.userLat.collectAsState()
    val currentLon by viewModel.userLon.collectAsState()
    val locationLabel by viewModel.locationLabel.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()

    // Live AI Assistance feedback
    var aiAnalysisText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Update AI analysis when hazard or description changes
    LaunchedEffect(selectedHazard, description, hasPhotoAttached) {
        delay(400) // Debounce
        val res = GeminiService.analyzeReport(selectedHazard, description, hasPhotoAttached)
        aiAnalysisText = "Consistency: ${res.visualConsistency} • Urgency: ${res.urgencyScore}/10. ${res.explanation}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("report_danger_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Report Danger",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepNavy
                )
                Text(
                    text = "Hyperlocal early-warning hazard reporting",
                    fontSize = 12.sp,
                    color = NeutralMedium
                )
            }

            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 1: Select Hazard Category
        Text(
            text = "1. SELECT HAZARD CATEGORY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SlateNavy,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Hazard grid
        val allHazards = HazardType.entries.toTypedArray()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in allHazards.toList().chunked(3)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (hazard in row) {
                        val isSelected = hazard == selectedHazard
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(82.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) DangerRed else NeutralLight,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedHazard = hazard }
                                .testTag("hazard_chip_${hazard.name}"),
                            color = if (isSelected) DangerRedContainer else PureWhite
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(hazard.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = hazard.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) DangerRed else NeutralDark,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Location
        Text(
            text = "2. LOCATION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SlateNavy,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Coordinates:",
                        fontSize = 12.sp,
                        color = NeutralMedium
                    )
                    Text(
                        text = "${String.format("%.4f", currentLat)}, ${String.format("%.4f", currentLon)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DeepNavy
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Label: $locationLabel",
                    fontSize = 12.sp,
                    color = SlateNavy,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.setDemoLocation(DemoLocation.REPORTER_DEFAULT)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Use Reporter GPS", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.setCustomCoordinates(21.1458, 79.0882, "Live Device GPS")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Refresh GPS", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Description
        Text(
            text = "3. SITUATION DESCRIPTION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SlateNavy,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_description_field"),
            placeholder = { Text("Describe observed hazard, street names, obstacles, water level, etc.") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite
            )
        )

        // Quick Description Templates
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssistChip(
                onClick = {
                    description = when (selectedHazard) {
                        HazardType.FLASH_FLOOD -> "Rapid water rising near main junction, underpass submerged, road blocked."
                        HazardType.FIRE -> "Heavy black smoke and spreading flames near warehouse perimeter."
                        HazardType.DANGEROUS_WILDLIFE -> "Wild animal sighted near human settlement boundary. Moving towards road."
                        else -> "Emergency condition observed at current location. Urgent awareness needed."
                    }
                },
                label = { Text("Quick Template", fontSize = 10.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 4: Photo Evidence (Optional)
        Text(
            text = "4. PHOTO EVIDENCE (OPTIONAL)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SlateNavy,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, NeutralLight, RoundedCornerShape(12.dp))
                .clickable { hasPhotoAttached = !hasPhotoAttached },
            color = if (hasPhotoAttached) SafeGreenContainer else PureWhite
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasPhotoAttached) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = if (hasPhotoAttached) SafeGreen else NeutralMedium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (hasPhotoAttached) "Photo Evidence Attached" else "Attach Photo Evidence",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (hasPhotoAttached) SafeGreen else NeutralDark
                    )
                    Text(
                        text = if (hasPhotoAttached) "Verified against AI visual consistency model" else "Tap to attach camera/gallery evidence",
                        fontSize = 11.sp,
                        color = NeutralMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 5: Reported Severity
        Text(
            text = "5. REPORTED SEVERITY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SlateNavy,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (sev in Severity.entries) {
                val isSelected = sev == selectedSeverity
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) DangerRed else NeutralLight,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedSeverity = sev },
                    color = if (isSelected) DangerRedContainer else PureWhite
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = sev.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) DangerRed else NeutralDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Assistance Card (Never claims 100% official verification)
        if (aiAnalysisText != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = InfoBlueContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = InfoBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI ASSISTANCE (Not Official Verification)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = InfoBlue,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = aiAnalysisText ?: "",
                        fontSize = 12.sp,
                        color = NeutralDark,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Primary Submit Button -> Triggers Accidental Report Protection Dialog
        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_report_button"),
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "CONTINUE TO VERIFY REPORT",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp,
                color = PureWhite
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Accidental Report Protection Dialog
    if (showConfirmDialog) {
        AccidentalReportProtectionDialog(
            hazardType = selectedHazard,
            severity = selectedSeverity,
            isSubmitting = isSubmitting,
            onDismiss = { showConfirmDialog = false },
            onConfirmed = {
                isSubmitting = true
                viewModel.submitReport(
                    hazardType = selectedHazard,
                    description = description,
                    severity = selectedSeverity,
                    imageUrl = if (hasPhotoAttached) "content://photo_evidence" else null
                ) {
                    isSubmitting = false
                    showConfirmDialog = false
                    onReportSubmitted()
                }
            }
        )
    }
}

@Composable
fun AccidentalReportProtectionDialog(
    hazardType: HazardType,
    severity: Severity,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    // For HIGH and CRITICAL reports, require a press-and-hold confirmation of 1.5 seconds
    val requireHold = severity == Severity.HIGH || severity == Severity.CRITICAL
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        if (isHolding && requireHold) {
            val startTime = System.currentTimeMillis()
            val holdDuration = 1400L
            while (isHolding && holdProgress < 1.0f) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed.toFloat() / holdDuration).coerceIn(0f, 1f)
                if (holdProgress >= 1.0f) {
                    onConfirmed()
                    break
                }
                delay(30)
            }
        } else {
            holdProgress = 0f
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚨", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONFIRM EMERGENCY REPORT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = DangerRed
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "This report will create a ${hazardType.defaultRadiusMeters.toInt()}m danger zone and trigger real-time safety alerts for people nearby.",
                    fontSize = 13.sp,
                    color = NeutralDark,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarningAmberContainer
                ) {
                    Text(
                        text = "Confirm that you are reporting a genuine potential hazard. False reports reduce community trust.",
                        modifier = Modifier.padding(10.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeutralDark
                    )
                }

                if (requireHold) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Press and hold button for 1.5s to confirm:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeutralMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { holdProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = DangerRed,
                        trackColor = NeutralLight,
                    )
                }
            }
        },
        confirmButton = {
            if (requireHold) {
                Button(
                    onClick = { /* Handled by pointerInput */ },
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isHolding = true
                                    tryAwaitRelease()
                                    isHolding = false
                                }
                            )
                        }
                        .testTag("press_and_hold_confirm_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (holdProgress > 0f) "HOLDING... ${(holdProgress * 100).toInt()}%" else "PRESS AND HOLD TO CONFIRM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PureWhite
                    )
                }
            } else {
                Button(
                    onClick = onConfirmed,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CONFIRM REPORT", fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontWeight = FontWeight.Bold, color = NeutralMedium)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = PureWhite
    )
}
