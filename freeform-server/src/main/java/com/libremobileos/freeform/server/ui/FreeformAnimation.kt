package com.libremobileos.freeform.server.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.view.animation.PathInterpolator

object FreeformAnimation {
    private val EXPRESSIVE_SPATIAL_INTERPOLATOR = PathInterpolator(0.2f, 0f, 0f, 1f)

    fun moveInScreenAnimator(
        start: Int,
        end: Int,
        dur: Long,
        moveX: Boolean,
        window: FreeformWindow
    ) {
        val layout = window.freeformLayout ?: return

        if (!ValueAnimator.areAnimatorsEnabled()) {
            window.windowManager.updateViewLayout(
                layout,
                window.windowParams.apply {
                    if (moveX) x = end else y = end
                }
            )
            return
        }

        AnimatorSet().apply {
            play(
                ValueAnimator.ofInt(start, end).apply {
                    interpolator = EXPRESSIVE_SPATIAL_INTERPOLATOR
                    addUpdateListener {
                        window.windowManager.updateViewLayout(
                            layout,
                            window.windowParams.apply {
                                if (moveX) x = it.animatedValue as Int
                                else y = it.animatedValue as Int
                            }
                        )
                    }
                }
            )
            duration = dur
            start()
        }
    }

    fun toFullScreen(window: FreeformWindow, dur: Long, listener: Animator.AnimatorListener) {
        val layout = window.freeformLayout ?: return
        val rootView = window.freeformRootView ?: return

        if (!ValueAnimator.areAnimatorsEnabled()) {
            window.windowManager.updateViewLayout(
                layout,
                window.windowParams.apply {
                    x = 0
                    y = 0
                }
            )
            rootView.layoutParams = rootView.layoutParams.apply {
                width = window.defaultDisplayWidth
                height = window.defaultDisplayHeight
            }
            val immediate = ValueAnimator.ofInt(0, 1).apply { duration = 0L }
            listener.onAnimationStart(immediate)
            listener.onAnimationEnd(immediate)
            return
        }

        val xAnim = ValueAnimator.ofInt(window.windowParams.x, 0).apply {
            interpolator = EXPRESSIVE_SPATIAL_INTERPOLATOR
            addUpdateListener {
                window.windowManager.updateViewLayout(
                    layout,
                    window.windowParams.apply {
                        x = it.animatedValue as Int
                    }
                )
            }
        }
        val yAnim = ValueAnimator.ofInt(window.windowParams.y, 0).apply {
            interpolator = EXPRESSIVE_SPATIAL_INTERPOLATOR
            addUpdateListener {
                window.windowManager.updateViewLayout(
                    layout,
                    window.windowParams.apply {
                        y = it.animatedValue as Int
                    }
                )
            }
        }
        val widthAnim = ValueAnimator.ofInt(window.freeformConfig.width, window.defaultDisplayWidth).apply {
            interpolator = EXPRESSIVE_SPATIAL_INTERPOLATOR
            addUpdateListener {
                rootView.layoutParams = rootView.layoutParams.apply {
                    width = it.animatedValue as Int
                }
            }
        }
        val heightAnim = ValueAnimator.ofInt(window.freeformConfig.height, window.defaultDisplayHeight).apply {
            interpolator = EXPRESSIVE_SPATIAL_INTERPOLATOR
            addUpdateListener {
                rootView.layoutParams = rootView.layoutParams.apply {
                    height = it.animatedValue as Int
                }
            }
        }

        AnimatorSet().apply {
            playTogether(xAnim, yAnim, widthAnim, heightAnim)
            duration = dur
            addListener(listener)
            start()
        }
    }
}
