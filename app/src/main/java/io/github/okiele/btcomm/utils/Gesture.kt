package io.github.okiele.btcomm.utils

import androidx.annotation.IntDef

const val UNKNOWN = 0
const val CONFIRM = 1
const val LEFT = 2
const val RIGHT = 3
const val TOP = 4
const val BOTTOM = 5

@IntDef(UNKNOWN, CONFIRM, LEFT, RIGHT, TOP, BOTTOM)
@Retention(AnnotationRetention.SOURCE)
annotation class GestureType

data class Gesture(
    @GestureType val type: Int,
    val date: Long = System.currentTimeMillis() / 1000
)
