package com.libremobileos.freeform.server.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.util.Slog
import android.view.Display
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.libremobileos.freeform.server.LMOFreeformServiceHolder
import com.libremobileos.freeform.server.SystemServiceHolder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val HANDLE_SPRING_INTERPOLATOR = OvershootInterpolator(1.6f)

private fun morphIndicator(
    target: View?,
    active: Boolean,
    activeScaleX: Float = 1.35f,
    activeScaleY: Float = 1.25f,
    restingAlpha: Float = 0.88f,
) {
    val view = target ?: return
    view.animate().cancel()
    val targetScaleX = if (active) activeScaleX else 1f
    val targetScaleY = if (active) activeScaleY else 1f
    val targetAlpha = if (active) 1f else restingAlpha
    if (!ValueAnimator.areAnimatorsEnabled()) {
        view.scaleX = targetScaleX
        view.scaleY = targetScaleY
        view.alpha = targetAlpha
    } else {
        view.animate()
            .scaleX(targetScaleX)
            .scaleY(targetScaleY)
            .alpha(targetAlpha)
            .setDuration(if (active) 160L else 220L)
            .setInterpolator(HANDLE_SPRING_INTERPOLATOR)
            .start()
    }
}

class MoveTouchListener(
    private val window: FreeformWindow
) : View.OnTouchListener {
    private var startX = 0.0f
    private var startY = 0.0f
    private var initialRawX = 0.0f
    private var initialRawY = 0.0f
    private var hasDragged = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val layout = window.freeformLayout ?: return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                initialRawX = event.rawX
                initialRawY = event.rawY
                hasDragged = false
                v.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
                morphIndicator(window.topDragHandleView, active = true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!hasDragged &&
                    (abs(event.rawX - initialRawX) > 6f || abs(event.rawY - initialRawY) > 6f)
                ) {
                    hasDragged = true
                }
                window.windowManager.updateViewLayout(layout, window.windowParams.apply {
                    x = (x + event.rawX - startX).roundToInt()
                    y = (y + event.rawY - startY).roundToInt()
                })
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                morphIndicator(window.topDragHandleView, active = false)
                if (hasDragged && event.actionMasked == MotionEvent.ACTION_UP) {
                    v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }
                window.makeSureFreeformInScreen()
            }
        }
        return true
    }
}

class LeftViewClickListener(private val window: FreeformWindow) : View.OnClickListener {
    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        window.close()
    }
}

/**
 * Maximize freeform window to full screen display
 */
class MaximizeClickListener(private val window: FreeformWindow) : View.OnClickListener {
    companion object {
        private const val TAG = "LMOFreeform/TouchListener"
    }

    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        if (null != window.freeformTaskStackListener) {
            if (window.freeformTaskStackListener!!.taskId == -1) {
                Slog.e(TAG, "taskId is -1, can`t move")
                return
            }
            runCatching {
                SystemServiceHolder.activityTaskManager.moveRootTaskToDisplay(
                    window.freeformTaskStackListener!!.taskId,
                    Display.DEFAULT_DISPLAY
                )
            }
        }
    }
}

/**
 * Pin / minimize freeform window into floating hang-up mini-card
 */
class PinClickListener(private val window: FreeformWindow) : View.OnClickListener {
    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        window.handler.post {
            window.handleHangUp()
        }
    }
}

class RightViewClickListener(private val displayId: Int) : View.OnClickListener {
    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        LMOFreeformServiceHolder.back(displayId)
    }
}

class ScaleTouchListener(
    private val window: FreeformWindow,
    private val isRight: Boolean = true
) : View.OnTouchListener {
    private var startX = 0.0f
    private var startY = 0.0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val rootView = window.freeformRootView ?: return true
        val veilView = window.veilView ?: return true
        val freeformView = window.freeformView ?: return true
        val gripIndicator = if (isRight) {
            window.rightScaleIndicatorView
        } else {
            window.leftScaleIndicatorView
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                v.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
                morphIndicator(gripIndicator, active = true, activeScaleX = 1.45f, activeScaleY = 1.3f, restingAlpha = 1f)
                rootView.visibility = View.INVISIBLE
                veilView.visibility = View.VISIBLE
            }
            MotionEvent.ACTION_MOVE -> {
                rootView.layoutParams = rootView.layoutParams.apply {
                    val xDelta = if (isRight) (event.rawX - startX) else (startX - event.rawX)
                    val yDelta = event.rawY - startY
                    width = max(25, (rootView.width + xDelta).roundToInt())
                    height = max(25, (rootView.height + yDelta).roundToInt())
                    if (width > height) {
                        if (xDelta < 0) width = height
                        else height = width
                    }
                }
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                morphIndicator(gripIndicator, active = false, restingAlpha = 1f)
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                freeformView.surfaceTexture?.let { surfaceTexture ->
                    window.freeformConfig.width = rootView.layoutParams.width
                    window.freeformConfig.height = rootView.layoutParams.height
                    window.handler.post { window.makeSureFreeformInScreen() }
                    window.measureScale()
                    LMOFreeformServiceHolder.resizeFreeform(
                        window,
                        window.freeformConfig.freeformWidth,
                        window.freeformConfig.freeformHeight,
                        window.freeformConfig.densityDpi
                    )
                    surfaceTexture.setDefaultBufferSize(
                        window.freeformConfig.freeformWidth,
                        window.freeformConfig.freeformHeight
                    )
                    val unveilDelay = if (ValueAnimator.areAnimatorsEnabled()) 220L else 0L
                    window.handler.postDelayed({
                        rootView.visibility = View.VISIBLE
                        veilView.visibility = View.GONE
                    }, unveilDelay)
                }
            }
        }
        return true
    }
}

class HangUpGestureListener(private val window: FreeformWindow) : SimpleOnGestureListener() {
    private var startX = 0
    private var startY = 0

    override fun onDown(e: MotionEvent): Boolean {
        startX = window.windowParams.x
        startY = window.windowParams.y
        return super.onDown(e)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        window.handler.post { window.handleHangUp() }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!isValidMotionEvent(e1) || !isValidMotionEvent(e2)) {
            return true
        }

        val e1RawX = e1?.rawX ?: 0f
        val e1RawY = e1?.rawY ?: 0f

        if (!isValidCoordinate(e1RawX) || !isValidCoordinate(e1RawY) ||
            !isValidCoordinate(e2.rawX) || !isValidCoordinate(e2.rawY)
        ) {
            return true
        }

        val newX = (startX + e2.rawX - e1RawX).roundToInt()
        val newY = (startY + e2.rawY - e1RawY).roundToInt()

        try {
            window.handler.post {
                val layout = window.freeformLayout ?: return@post
                window.windowManager.updateViewLayout(layout, window.windowParams.apply {
                    x = newX
                    y = newY
                })
            }
        } catch (_: Exception) {}
        return true
    }

    fun isValidMotionEvent(event: MotionEvent?): Boolean {
        return event != null &&
            !event.rawX.isNaN() &&
            !event.rawY.isNaN() &&
            event.rawX.isFinite() &&
            event.rawY.isFinite()
    }

    fun isValidCoordinate(coordinate: Float): Boolean {
        return !coordinate.isNaN() && coordinate.isFinite()
    }
}
