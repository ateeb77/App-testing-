package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.HazardType
import com.example.util.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Jagruk", appName)
  }

  @Test
  fun `verify haversine distance calculation and danger geofence`() {
    // Reporter location A
    val repLat = 21.1458
    val repLon = 79.0882

    // Resident location B (inside 1000m flood zone)
    val resLat = 21.1480
    val resLon = 79.0900

    val distance = GeoUtils.calculateDistanceMeters(repLat, repLon, resLat, resLon)
    // Distance should be approximately 300-500 meters
    assertTrue("Distance should be around 300-500m", distance in 250.0..550.0)

    val isInside = GeoUtils.isInsideGeofence(resLat, resLon, repLat, repLon, 1000.0)
    assertTrue("Resident must be inside 1000m danger geofence", isInside)

    // Outside location C (~4.7km away)
    val outLat = 21.1800
    val outLon = 79.1200
    val isOutside = GeoUtils.isInsideGeofence(outLat, outLon, repLat, repLon, 1000.0)
    assertFalse("Outside user must NOT be inside 1000m danger geofence", isOutside)
  }

  @Test
  fun `verify MainViewModel initializes cleanly without exceptions`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.MainViewModel(app)
    assertEquals(com.example.data.model.UserRole.RESIDENT, vm.userRole.value)
    assertTrue("Demo mode should be active by default", vm.isDemoMode.value)
  }
}
