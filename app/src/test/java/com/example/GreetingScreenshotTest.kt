package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.Confidence
import com.example.data.model.HazardType
import com.example.data.model.Incident
import com.example.data.model.IncidentStatus
import com.example.data.model.ReportSource
import com.example.data.model.Severity
import com.example.ui.components.CriticalAlertScreen
import com.example.ui.theme.JagrukTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleIncident = Incident(
        id = "test_inc_1",
        reporterId = "reporter_test",
        hazardType = HazardType.FLASH_FLOOD,
        description = "Rapid water accumulation covering roadway. High vehicle hazard.",
        latitude = 21.1458,
        longitude = 79.0882,
        radiusMeters = 1000.0,
        severity = Severity.HIGH,
        confidence = Confidence.HIGH,
        status = IncidentStatus.ACTIVE,
        source = ReportSource.COMMUNITY
    )

    composeTestRule.setContent {
      JagrukTheme {
        CriticalAlertScreen(
            incident = sampleIncident,
            distanceMeters = 420.0,
            onViewMap = {},
            onAcknowledge = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
