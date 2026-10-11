package com.meta.wearable.dat.externalsampleapps.cameraaccess.stream

/**
 * Measures how far frames fall behind the glasses' capture clock before the app sees them.
 *
 * Frame timestamps advance at capture pace. If arrival times advance faster than timestamps, frames
 * are waiting somewhere in the SDK or transport before they reach the app. A drift that keeps
 * growing means that backlog. A flat drift with slow conversion means the phone's own pipeline is
 * the limit.
 */
internal class DeliveryClock {
  private var originArrivalMs = -1L
  private var originPtsUs = -1L

  fun reset() {
    originArrivalMs = -1L
    originPtsUs = -1L
  }

  /** Milliseconds this frame arrived later than its timestamp implies, relative to the first frame. */
  fun driftMs(arrivalMs: Long, ptsUs: Long): Long {
    if (originArrivalMs < 0) {
      originArrivalMs = arrivalMs
      originPtsUs = ptsUs
    }
    return (arrivalMs - originArrivalMs) - (ptsUs - originPtsUs) / 1_000L
  }
}

/** Aggregates per-frame timings and emits one summary line per [reportIntervalMs]. */
internal class VideoPipelineStats(
    private val reportIntervalMs: Long = 2_000L,
    private val log: (String) -> Unit,
) {
  private var windowStartMs = -1L
  private var frames = 0
  private var convertTotalMs = 0L
  private var convertMaxMs = 0L
  private var driftLastMs = 0L
  private var driftMaxMs = 0L

  fun reset() {
    windowStartMs = -1L
    frames = 0
    convertTotalMs = 0L
    convertMaxMs = 0L
    driftLastMs = 0L
    driftMaxMs = 0L
  }

  fun onFrame(nowMs: Long, convertMs: Long, driftMs: Long) {
    if (windowStartMs < 0) windowStartMs = nowMs
    frames++
    convertTotalMs += convertMs
    convertMaxMs = maxOf(convertMaxMs, convertMs)
    driftLastMs = driftMs
    driftMaxMs = maxOf(driftMaxMs, driftMs)

    val elapsedMs = nowMs - windowStartMs
    if (elapsedMs < reportIntervalMs) return

    val fps = frames * 1_000.0 / elapsedMs
    log(
        "Video pipeline: fps=${"%.1f".format(fps)} convertAvgMs=${convertTotalMs / frames} " +
            "convertMaxMs=$convertMaxMs deliveryDriftMs=$driftLastMs maxDriftMs=$driftMaxMs"
    )
    reset()
  }
}
