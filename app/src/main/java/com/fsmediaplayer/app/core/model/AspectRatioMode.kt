package com.fsmediaplayer.app.core.model

import androidx.media3.ui.AspectRatioFrameLayout

enum class AspectRatioMode(val title: String, val resizeMode: Int) {
    FIT("Fit to Screen", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Stretch / Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Crop & Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FIXED_HEIGHT("Fixed Height (16:9)", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT)
}
