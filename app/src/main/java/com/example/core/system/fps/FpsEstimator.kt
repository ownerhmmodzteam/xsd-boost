package com.example.core.system.fps

import android.view.Choreographer

class FpsEstimator {
    private var frameCount = 0
    private var lastFpsTimestamp = 0L
    private var currentFps = 60f
    private var isTracking = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isTracking) return
            frameCount++
            val nowMs = frameTimeNanos / 1_000_000
            if (lastFpsTimestamp == 0L) {
                lastFpsTimestamp = nowMs
            } else {
                val delta = nowMs - lastFpsTimestamp
                if (delta >= 1000) {
                    currentFps = (frameCount * 1000f) / delta
                    frameCount = 0
                    lastFpsTimestamp = nowMs
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        if (isTracking) return
        isTracking = true
        frameCount = 0
        lastFpsTimestamp = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        isTracking = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun getCurrentFps(): Float = currentFps
}
