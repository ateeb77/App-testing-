package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun EmergencyHelpSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    fun dial(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite)
            .padding(24.dp)
            .testTag("emergency_help_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚨", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EMERGENCY SERVICES (INDIA)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepNavy
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary National Emergency Service: 112
        Button(
            onClick = { dial("112") },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("dial_112_button"),
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(24.dp), tint = PureWhite)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "CALL 112 (NATIONAL EMERGENCY)",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = PureWhite
                )
                Text(
                    text = "All-in-one Emergency Response Support System (ERSS)",
                    fontSize = 10.sp,
                    color = PureWhite.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Direct Service Lines: Police 100, Fire 101, Ambulance 102
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EmergencyServiceButton(
                icon = "👮",
                title = "POLICE",
                number = "100",
                color = SlateNavy,
                modifier = Modifier.weight(1f),
                onClick = { dial("100") }
            )
            EmergencyServiceButton(
                icon = "🚒",
                title = "FIRE",
                number = "101",
                color = DangerRed,
                modifier = Modifier.weight(1f),
                onClick = { dial("101") }
            )
            EmergencyServiceButton(
                icon = "🚑",
                title = "MEDICAL",
                number = "102",
                color = SafeGreen,
                modifier = Modifier.weight(1f),
                onClick = { dial("102") }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Government Integration Disclosure
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceVariantLight
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "OFFICIAL SYSTEMS COMPLEMENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
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

@Composable
fun EmergencyServiceButton(
    icon: String,
    title: String,
    number: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = SurfaceLight,
        onClick = onClick,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeutralDark
            )
            Text(
                text = number,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}
