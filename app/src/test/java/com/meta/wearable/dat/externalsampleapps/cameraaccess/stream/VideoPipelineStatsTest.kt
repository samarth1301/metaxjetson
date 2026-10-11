package com.meta.wearable.dat.externalsampleapps.cameraaccess.stream

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPipelineStatsTest {
  @Test
  fun deliveryDriftIsZeroWhenFramesArriveAtCapturePace() {
    val clock = DeliveryClock()
    assertEquals(0L, clock.driftMs(arrivalMs = 1_000L, ptsUs = 5_000_000L))
    assertEquals(0L, clock.driftMs(arrivalMs = 1_066L, ptsUs = 5_066_000L))
    assertEquals(0L, clock.driftMs(arrivalMs = 1_133L, ptsUs = 5_133_000L))
  }

  @Test
  fun deliveryDriftGrowsWhenFramesQueueBeforeTheApp() {
    val clock = DeliveryClock()
    clock.driftMs(arrivalMs = 1_000L, ptsUs = 0L)
    // Frame captured 66ms after the first one but only reaches the app 566ms after it: 500ms behind.
    assertEquals(500L, clock.driftMs(arrivalMs = 1_566L, ptsUs = 66_000L))
  }

  @Test
  fun resetRestartsTheDriftOrigin() {
    val clock = DeliveryClock()
    clock.driftMs(arrivalMs = 1_000L, ptsUs = 0L)
    clock.reset()
    assertEquals(0L, clock.driftMs(arrivalMs = 9_000L, ptsUs = 40_000_000L))
  }

  @Test
  fun statsReportOncePerIntervalWithAveragesAndMaxima() {
    val lines = mutableListOf<String>()
    val stats = VideoPipelineStats(reportIntervalMs = 1_000L, log = lines::add)

    stats.onFrame(nowMs = 0L, convertMs = 10L, driftMs = 5L)
    stats.onFrame(nowMs = 500L, convertMs = 30L, driftMs = 20L)
    assertEquals(0, lines.size)

    stats.onFrame(nowMs = 1_000L, convertMs = 20L, driftMs = 12L)
    assertEquals(1, lines.size)
    assertEquals(
        "Video pipeline: fps=3.0 convertAvgMs=20 convertMaxMs=30 deliveryDriftMs=12 maxDriftMs=20",
        lines.single(),
    )
  }
}
