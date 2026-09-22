package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.data.model.Incident
import com.example.ui.theme.*
import com.example.util.GeoUtils

@Composable
fun CriticalAlertScreen(
    incident: Incident,
    distanceMeters: Double,
    onViewMap: () -> Unit,
    onAcknowledge: () -> Unit
) {
    // Pulsing animation for emergency urgency
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A).copy(alpha = 0.96f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, DangerRed, RoundedCornerShape(24.dp))
                .testTag("critical_alert_card"),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DangerRed)
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🚨 CRITICAL ALERT — DANGER ZONE",
                        color = PureWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Hazard Icon with Pulse
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(DangerRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = incident.hazardType.emoji,
                        fontSize = (44 * pulseScale).sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = incident.hazardType.displayName.uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DangerRed,
                    textAlign = TextAlign.Center
                )

                // Distance & Confidence Row
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DangerRedContainer
                    ) {
                        Text(
                            text = "${GeoUtils.formatDistance(distanceMeters)} AWAY",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = DangerRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(incident.confidence.colorHex).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = incident.confidence.label.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color(incident.confidence.colorHex),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // WHAT HAPPENED Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceLight
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "WHAT HAPPENED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeutralMedium,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = incident.description,
                            fontSize = 15.sp,
                            color = NeutralDark,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RECOMMENDED ACTION Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmberContainer
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECOMMENDED ACTION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = incident.hazardType.defaultAction,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeutralDark,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = onViewMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("view_live_map_alert_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Place, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VIEW LIVE MAP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onAcknowledge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("acknowledge_alert_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeutralDark)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACKNOWLEDGE & DISMISS",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
