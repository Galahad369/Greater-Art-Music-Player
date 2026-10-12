package com.local.listentomusic.data

enum class AmbientMotion { OFF, SUBTLE, NORMAL }
internal fun ambientIntensityFromStorage(value: Float?): Float =
    value?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: .5f
internal fun ambientMotionFromStorage(value: String?): AmbientMotion =
    AmbientMotion.entries.firstOrNull { it.name == value } ?: AmbientMotion.SUBTLE
