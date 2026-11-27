@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.libremobileos.sidebar.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.axion.compose.expressive.ExpressiveBlurDefaults
import com.android.axion.compose.expressive.isExpressiveReducedMotionEnabled
import com.android.axion.compose.expressive.isExpressiveWindowBlurSupported
import com.android.axion.compose.expressive.rememberExpressiveBlurActive
import com.android.axion.compose.expressive.rememberExpressiveReducedMotion
import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import com.libremobileos.sidebar.app.SidebarApplication
import com.libremobileos.sidebar.bean.AppInfo
import com.libremobileos.sidebar.ui.theme.SidebarTheme
import com.libremobileos.sidebar.utils.Logger
import kotlin.math.roundToInt

/**
 * Floating M3 Expressive sidebar dock window controller with rounded Kawase backdrop blur,
 * spring expand/collapse physics, and reduced-motion (`ANIMATOR_DURATION_SCALE == 0f`) support.
 */
class SidebarView(
    private val context: Context,
    private val viewModel: ServiceViewModel,
    private val callback: Callback
) : SavedStateRegistryOwner {

    private var lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var composeView: View
    private var blurDrawable: BackgroundBlurDrawable? = null
    private var sidebarPositionX = 1
    private var sidebarPositionY = 0
    private var isShowing = false
    private var isDismissing = false

    private val sidebarDismissingState = mutableStateOf(false)
    private val sidebarPositionXState = mutableIntStateOf(1)

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutParams = LayoutParams()
    private val logger = Logger(TAG)
    private val handler = Handler(Looper.getMainLooper())
    private val detachFallbackRunnable = Runnable { detachViewImmediate() }

    private val sharedPrefs by lazy {
        context.getSharedPreferences(SidebarApplication.CONFIG, Context.MODE_PRIVATE)
    }

    companion object {
        private const val OFFSET_X = 90
        private const val INITIAL_SLIDE_OFFSET_PX = 180f
        private const val COLLAPSED_SCALE = 0.90f
        private const val DEFAULT_CORNER_RADIUS_DP = 28f
        private const val DETACH_FALLBACK_TIMEOUT_MS = 320L
        private const val PACKAGE = "com.libremobileos.freeform"
        private const val ACTION = "com.libremobileos.freeform.START_FREEFORM"
        private const val TAG = "SidebarView"
    }

    init {
        savedStateRegistryController.performRestore(null)
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            handler.post(block)
        }
    }

    private fun launchAppInFreeform(appInfo: AppInfo) {
        val intent = Intent(ACTION).apply {
            setPackage(PACKAGE)
            putExtra("packageName", appInfo.packageName)
            putExtra("activityName", appInfo.activityName)
            putExtra("userId", appInfo.userId)
        }
        context.sendBroadcastAsUser(intent, UserHandle(UserHandle.USER_CURRENT))
        removeView()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showView() {
        runOnMain {
            if (isShowing && !isDismissing) return@runOnMain
            if (isShowing && isDismissing) {
                detachViewImmediate(notifyCallback = false)
            }

            if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
                lifecycleRegistry = LifecycleRegistry(this)
            }

            sidebarDismissingState.value = false
            isDismissing = false

            layoutParams.apply {
                type = LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                privateFlags = LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY or
                    LayoutParams.PRIVATE_FLAG_SYSTEM_APPLICATION_OVERLAY
                format = PixelFormat.RGBA_8888
                windowAnimations = 0
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }

            applySidebarPositionToLayoutParams()
            initComposeView()

            val reducedMotion = isExpressiveReducedMotionEnabled(context)
            if (reducedMotion) {
                composeView.translationX = 0f
                composeView.scaleX = 1f
                composeView.scaleY = 1f
                composeView.alpha = 1f
            } else {
                composeView.translationX = sidebarPositionX * INITIAL_SLIDE_OFFSET_PX
                composeView.scaleX = COLLAPSED_SCALE
                composeView.scaleY = COLLAPSED_SCALE
                composeView.alpha = 0f
            }

            composeView.setOnTouchListener { _, event ->
                logger.d("composeView: $event")
                if (
                    event.action == MotionEvent.ACTION_OUTSIDE ||
                    event.action == MotionEvent.ACTION_UP
                ) {
                    removeView()
                    true
                } else {
                    false
                }
            }
            composeView.isFocusableInTouchMode = true
            composeView.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    removeView()
                    true
                } else {
                    false
                }
            }

            runCatching {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                windowManager.addView(composeView, layoutParams)
                isShowing = true
                attachInitialPreDrawSetup(reducedMotion = reducedMotion)
            }.onFailure {
                logger.e("failed to add sidebar view: ", it)
            }
        }
    }

    fun removeView(force: Boolean = false) {
        runOnMain {
            if (!isShowing) {
                handler.removeCallbacks(detachFallbackRunnable)
                isDismissing = false
                sidebarDismissingState.value = false
                return@runOnMain
            }

            logger.d("removeView(force=$force)")
            if (force || isExpressiveReducedMotionEnabled(context)) {
                detachViewImmediate()
                return@runOnMain
            }

            if (isDismissing) return@runOnMain
            isDismissing = true
            sidebarDismissingState.value = true
            handler.removeCallbacks(detachFallbackRunnable)
            handler.postDelayed(detachFallbackRunnable, DETACH_FALLBACK_TIMEOUT_MS)
        }
    }

    private fun detachViewImmediate(notifyCallback: Boolean = true) {
        handler.removeCallbacks(detachFallbackRunnable)
        if (!isShowing) {
            isDismissing = false
            sidebarDismissingState.value = false
            return
        }

        clearBlurDrawable()
        runCatching {
            if (
                lifecycleRegistry.currentState != Lifecycle.State.DESTROYED &&
                lifecycleRegistry.currentState != Lifecycle.State.INITIALIZED
            ) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
        }
        runCatching {
            windowManager.removeViewImmediate(composeView)
        }.onFailure {
            logger.e("failed to remove sidebar view: $it")
        }
        isShowing = false
        isDismissing = false
        sidebarDismissingState.value = false
        if (notifyCallback) {
            callback.onRemove()
        }
    }

    fun updateSidebarPosition() {
        runOnMain {
            applySidebarPositionToLayoutParams()
            if (isShowing) {
                runCatching {
                    windowManager.updateViewLayout(composeView, layoutParams)
                }.onFailure { e ->
                    logger.e("failed to updateViewLayout: ", e)
                }
            }
        }
    }

    private fun applySidebarPositionToLayoutParams() {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val isPortrait =
            context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val sidebarHeight = if (isPortrait) {
            screenHeight / 3
        } else {
            (screenHeight * 0.8f).roundToInt()
        }

        sidebarPositionX = sharedPrefs.getInt(SidebarService.SIDELINE_POSITION_X, 1)
        sidebarPositionXState.intValue = sidebarPositionX
        sidebarPositionY = if (isPortrait) {
            sharedPrefs.getInt(SidebarService.SIDELINE_POSITION_Y_PORTRAIT, -screenHeight / 6)
        } else {
            0
        }

        layoutParams.apply {
            width = LayoutParams.WRAP_CONTENT
            height = sidebarHeight
            x = sidebarPositionX * (screenWidth / 2 - OFFSET_X)
            y = sidebarPositionY
        }

        logger.d(
            "updateSidebarPosition: posX=$sidebarPositionX posY=$sidebarPositionY" +
                " lp.x=${layoutParams.x} lp.y=${layoutParams.y} height=${layoutParams.height}"
        )
    }

    private fun attachInitialPreDrawSetup(reducedMotion: Boolean) {
        if (!::composeView.isInitialized) return
        val observer = composeView.viewTreeObserver
        if (!observer.isAlive) return
        observer.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val liveObserver = composeView.viewTreeObserver
                    if (liveObserver.isAlive) {
                        liveObserver.removeOnPreDrawListener(this)
                    } else if (observer.isAlive) {
                        observer.removeOnPreDrawListener(this)
                    }
                    if (!isShowing) return true
                    val w = composeView.width
                    val h = composeView.height
                    if (w > 0 && h > 0) {
                        composeView.pivotX = if (sidebarPositionX >= 0) w.toFloat() else 0f
                        composeView.pivotY = h * 0.5f
                    }
                    syncBlurDrawable(
                        blurActive = isExpressiveWindowBlurSupported(context),
                        alphaFraction = if (reducedMotion) 1f else composeView.alpha,
                    )
                    return true
                }
            },
        )
    }

    private fun resolveCornerRadiusPx(): Float {
        val cornerRadiusDp = sharedPrefs.getFloat(
            SidebarService.SIDEBAR_CORNER_RADIUS,
            DEFAULT_CORNER_RADIUS_DP,
        ).coerceIn(0f, 36f)
        return cornerRadiusDp * context.resources.displayMetrics.density
    }

    private fun syncBlurDrawable(blurActive: Boolean, alphaFraction: Float) {
        if (!::composeView.isInitialized || !isShowing) return
        if (!blurActive) {
            clearBlurDrawable()
            return
        }
        val radiusPx = (
            ExpressiveBlurDefaults.DefaultWindowBlurRadiusPx * alphaFraction.coerceIn(0f, 1f)
            ).roundToInt()
        if (radiusPx <= 0) {
            blurDrawable?.setBlurRadius(0)
            blurDrawable?.setVisible(false, false)
            return
        }
        val blur = blurDrawable ?: runCatching {
            composeView.viewRootImpl?.createBackgroundBlurDrawable()
        }.getOrNull()?.also { created ->
            blurDrawable = created
            composeView.background = created
        } ?: return

        blur.setVisible(true, false)
        blur.setAlpha(255)
        blur.setColor(Color.TRANSPARENT)
        blur.setCornerRadius(resolveCornerRadiusPx())
        blur.setBlurRadius(radiusPx)
        if (composeView.background !== blur) {
            composeView.background = blur
        }
    }

    private fun clearBlurDrawable() {
        blurDrawable?.let { blur ->
            runCatching {
                blur.setVisible(false, false)
                blur.setBlurRadius(0)
            }
        }
        if (::composeView.isInitialized && composeView.background === blurDrawable) {
            composeView.background = null
        }
        blurDrawable = null
    }

    private fun initComposeView() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@SidebarView)
            setViewTreeSavedStateRegistryOwner(this@SidebarView)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SidebarTheme(applySystemBars = false) {
                    SidebarDockAnimatedHost()
                }
            }
        }
    }

    @Composable
    private fun SidebarDockAnimatedHost() {
        val dismissing by sidebarDismissingState
        val posX by sidebarPositionXState
        val reducedMotion = rememberExpressiveReducedMotion()
        val blurActive = rememberExpressiveBlurActive()

        var entered by remember { mutableStateOf(reducedMotion) }
        LaunchedEffect(Unit) {
            entered = true
        }

        LaunchedEffect(dismissing, reducedMotion) {
            if (dismissing && reducedMotion) {
                detachViewImmediate()
            }
        }

        val visible = entered && !dismissing
        val spatialSpec = if (reducedMotion) {
            snap()
        } else {
            MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        }
        val effectsSpec = if (reducedMotion) {
            snap()
        } else {
            MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        }

        val slideOffset by animateFloatAsState(
            targetValue = if (visible) 0f else posX * INITIAL_SLIDE_OFFSET_PX,
            animationSpec = spatialSpec,
            label = "sidebar_slide",
            finishedListener = {
                if (dismissing) {
                    handler.post { detachViewImmediate() }
                }
            },
        )
        val dockScale by animateFloatAsState(
            targetValue = if (visible) 1f else COLLAPSED_SCALE,
            animationSpec = spatialSpec,
            label = "sidebar_scale",
        )
        val dockAlpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = effectsSpec,
            label = "sidebar_alpha",
        )

        SideEffect {
            if (::composeView.isInitialized && isShowing) {
                val w = composeView.width
                val h = composeView.height
                if (w > 0 && h > 0) {
                    composeView.pivotX = if (posX >= 0) w.toFloat() else 0f
                    composeView.pivotY = h * 0.5f
                }
                composeView.translationX = slideOffset
                composeView.scaleX = dockScale
                composeView.scaleY = dockScale
                val clampedAlpha = dockAlpha.coerceIn(0f, 1f)
                composeView.alpha = clampedAlpha
                syncBlurDrawable(
                    blurActive = blurActive,
                    alphaFraction = clampedAlpha,
                )
            }
        }

        SidebarComposeView(
            viewModel = viewModel,
            launchApp = { launchAppInFreeform(it) },
            closeSidebar = { removeView() },
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth(),
        )
    }

    interface Callback {
        fun onRemove()
    }
}
