package com.fsmediaplayer.app.core.model

enum class DecoderMode(val label: String, val description: String) {
    HARDWARE("HW", "Hardware Acceleration (GPU/VPU)"),
    SOFTWARE("SW", "Software Codec Fallback (CPU)")
}
