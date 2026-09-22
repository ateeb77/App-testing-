package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.CriticalAlertScreen
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.util.GeoUtils

enum class NavigationTab(val route: String, val label: String) {
    HOME("home", "Home"),
    MAP("map", "Map"),
    REPORT("report", "Report"),
    ALERTS("alerts", "Alerts"),
    ADMIN("admin", "Admin"),
    PROFILE("profile", "Profile")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JagrukTheme {
                JagrukApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JagrukApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var showEmergencyHelpSheet by remember { mutableStateOf(false) }
    var showPresentationMode by remember { mutableStateOf(false) }

    val criticalAlert by viewModel.criticalAlertIncident.collectAsState()
    val userLat by viewModel.userLat.collectAsState()
    val userLon by viewModel.userLon.collectAsState()
    val connectivity by viewModel.connectivityStatus.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    // Request permissions on launch
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JAGRUK",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = DeepNavy,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(connectivity.colorHex))
                        )
                    }
                },
                actions = {
                    // Admin Command Center Shortcut
                    IconButton(
                        onClick = {
                            currentTab = if (currentTab == NavigationTab.ADMIN) NavigationTab.HOME else NavigationTab.ADMIN
                        },
                        modifier = Modifier.testTag("top_bar_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Command Center",
                            tint = if (currentTab == NavigationTab.ADMIN) DangerRed else DeepNavy
                        )
                    }

                    // Emergency 112 Help Shortcut
                    FilledTonalButton(
                        onClick = { showEmergencyHelpSheet = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WarningAmberContainer,
                            contentColor = WarningAmber
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("top_bar_help_button")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("112", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = PureWhite,
                contentColor = DeepNavy
            ) {
                NavigationBarItem(
                    selected = currentTab == NavigationTab.HOME,
                    onClick = { currentTab = NavigationTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == NavigationTab.MAP,
                    onClick = { currentTab = NavigationTab.MAP },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Live Map") },
                    label = { Text("Map", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_map")
                )
                NavigationBarItem(
                    selected = currentTab == NavigationTab.REPORT,
                    onClick = { currentTab = NavigationTab.REPORT },
                    icon = { Icon(Icons.Default.AddAlert, contentDescription = "Report Danger", tint = DangerRed) },
                    label = { Text("Report", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed) },
                    modifier = Modifier.testTag("nav_report")
                )
                NavigationBarItem(
                    selected = currentTab == NavigationTab.ALERTS,
                    onClick = { currentTab = NavigationTab.ALERTS },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("Alerts", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_alerts")
                )
                NavigationBarItem(
                    selected = currentTab == NavigationTab.PROFILE,
                    onClick = { currentTab = NavigationTab.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onReportDangerClick = { currentTab = NavigationTab.REPORT },
                        onViewLiveMapClick = { currentTab = NavigationTab.MAP },
                        onAlertHistoryClick = { currentTab = NavigationTab.ALERTS },
                        onEmergencyHelpClick = { showEmergencyHelpSheet = true },
                        onOpenProfileClick = { currentTab = NavigationTab.PROFILE }
                    )
                }
                NavigationTab.MAP -> {
                    LiveMapScreen(
                        viewModel = viewModel,
                        onReportClick = { currentTab = NavigationTab.REPORT }
                    )
                }
                NavigationTab.REPORT -> {
                    ReportDangerScreen(
                        viewModel = viewModel,
                        onReportSubmitted = { currentTab = NavigationTab.MAP },
                        onCancel = { currentTab = NavigationTab.HOME }
                    )
                }
                NavigationTab.ALERTS -> {
                    AlertsScreen(
                        viewModel = viewModel,
                        onViewOnMap = { currentTab = NavigationTab.MAP }
                    )
                }
                NavigationTab.ADMIN -> {
                    AdminCommandCenterScreen(
                        viewModel = viewModel,
                        onOpenPresentationMode = { showPresentationMode = true }
                    )
                }
                NavigationTab.PROFILE -> {
                    ProfileScreen(
                        viewModel = viewModel
                    )
                }
            }

            // Real-Time Critical Alert Full-Screen Overlay (When User is Inside Danger Zone!)
            if (criticalAlert != null) {
                val inc = criticalAlert!!
                val dist = GeoUtils.calculateDistanceMeters(
                    userLat, userLon,
                    inc.latitude, inc.longitude
                )
                CriticalAlertScreen(
                    incident = inc,
                    distanceMeters = dist,
                    onViewMap = {
                        viewModel.dismissCriticalAlert()
                        currentTab = NavigationTab.MAP
                    },
                    onAcknowledge = {
                        viewModel.acknowledgeAlert(inc.id)
                    }
                )
            }
        }
    }

    // Emergency Help Bottom Sheet
    if (showEmergencyHelpSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEmergencyHelpSheet = false },
            containerColor = PureWhite
        ) {
            EmergencyHelpSheet(onDismiss = { showEmergencyHelpSheet = false })
        }
    }

    // Judges Presentation Mode
    if (showPresentationMode) {
        Dialog(
            onDismissRequest = { showPresentationMode = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PresentationModeScreen(
                viewModel = viewModel,
                onClose = { showPresentationMode = false }
            )
        }
    }
}
