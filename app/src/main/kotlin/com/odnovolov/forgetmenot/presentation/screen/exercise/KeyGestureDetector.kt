package com.odnovolov.forgetmenot.presentation.screen.exercise

import com.odnovolov.forgetmenot.presentation.screen.exercise.KeyGestureDetector.Gesture.*
import com.odnovolov.forgetmenot.presentation.screen.exercise.KeyGestureDetector.SpeedOptimization.*
import kotlinx.coroutines.*

class KeyGestureDetector(
    var detectSinglePress: Boolean = false,
    var detectDoublePress: Boolean = false,
    var detectLongPress: Boolean = false,
    private val coroutineScope: CoroutineScope,
    private val onGestureDetect: (Gesture) -> Unit
) {
    private var longPressDetectorJob: Job? = null
    private var singlePressDetectorJob: Job? = null
    private var isPressed: Boolean = false
    private val speedOptimization: SpeedOptimization
        get() = when {
            !detectSinglePress
                    && !detectDoublePress
                    && !detectLongPress -> {
                DO_NOT_DETECT
            }
            detectSinglePress
                    && !detectDoublePress
                    && !detectLongPress -> {
                FIXATION_SINGLE_PRESS_ON_KEY_PRESSED
            }
            detectSinglePress
                    && !detectDoublePress
                    && detectLongPress -> {
                FIXATION_SINGLE_PRESS_ON_KEY_RELEASED
            }
            !detectSinglePress
                    && !detectDoublePress
                    && detectLongPress -> {
                FIXATION_LONG_PRESS_ON_KEY_PRESSED
            }
            else -> {
                NO_OPTIMIZATION
            }
        }

    fun dispatchKeyEvent(isPressed: Boolean) {
        if (this.isPressed == isPressed) return else this.isPressed = isPressed
        if (isPressed) onKeyPressed() else onKeyReleased()
    }

    private fun onKeyPressed() {
        when (speedOptimization) {
            DO_NOT_DETECT -> {
                // nothing happens
            }
            FIXATION_SINGLE_PRESS_ON_KEY_PRESSED -> {
                onGestureDetect.invoke(SINGLE_PRESS)
            }
            FIXATION_SINGLE_PRESS_ON_KEY_RELEASED -> {
                launchLongPressDetector()
            }
            FIXATION_LONG_PRESS_ON_KEY_PRESSED -> {
                onGestureDetect.invoke(LONG_PRESS)
            }
            NO_OPTIMIZATION -> {
                if (singlePressDetectorJob.isActive()) {
                    singlePressDetectorJob!!.cancel()
                    onGestureDetect.invoke(DOUBLE_PRESS)
                } else {
                    launchLongPressDetector()
                }
            }
        }
    }

    private fun onKeyReleased() {
        when (speedOptimization) {
            DO_NOT_DETECT -> {
                // nothing happens
            }
            FIXATION_SINGLE_PRESS_ON_KEY_PRESSED -> {
                // nothing happens
            }
            FIXATION_SINGLE_PRESS_ON_KEY_RELEASED -> {
                if (longPressDetectorJob.isActive()) {
                    longPressDetectorJob!!.cancel()
                    onGestureDetect.invoke(SINGLE_PRESS)
                }
            }
            FIXATION_LONG_PRESS_ON_KEY_PRESSED -> {
                // nothing happens
            }
            NO_OPTIMIZATION -> {
                if (longPressDetectorJob.isActive()) {
                    longPressDetectorJob!!.cancel()
                    launchSinglePressDetector()
                }
            }
        }
    }

    private fun launchLongPressDetector() {
        longPressDetectorJob = coroutineScope.launch {
            delay(LONG_PRESS_DURATION)
            if (isActive) {
                if (detectLongPress) onGestureDetect.invoke(LONG_PRESS)
            }
        }
    }

    private fun launchSinglePressDetector() {
        singlePressDetectorJob = coroutineScope.launch {
            delay(MAX_DOUBLE_PRESS_INTERVAL)
            if (isActive) {
                if (detectSinglePress) onGestureDetect(SINGLE_PRESS)
            }
        }
    }

    private fun Job?.isActive() = this != null && this.isActive

    private enum class SpeedOptimization {
        DO_NOT_DETECT,
        FIXATION_SINGLE_PRESS_ON_KEY_PRESSED,
        FIXATION_SINGLE_PRESS_ON_KEY_RELEASED,
        FIXATION_LONG_PRESS_ON_KEY_PRESSED,
        NO_OPTIMIZATION
    }

    enum class Gesture {
        SINGLE_PRESS,
        DOUBLE_PRESS,
        LONG_PRESS
    }

    private companion object {
        const val MAX_DOUBLE_PRESS_INTERVAL = 300L
        const val LONG_PRESS_DURATION = 300L
    }
}