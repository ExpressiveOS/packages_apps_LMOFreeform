package com.libremobileos.freeform.server.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONTEXT_IGNORE_SECURITY
import android.content.Context.CONTEXT_INCLUDE_CODE
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.Slog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

@SuppressLint("WrongConstant", "DiscouragedApi")
class RemoteResourceHolder(private val hostContext: Context, private val resPkg: String) {

    private var remoteContext: Context? = null

    companion object {
        private const val TAG = "LMOFreeform/RemoteResourceHolder"
    }

    init {
        refreshContext(hostContext.resources.configuration)
    }

    fun refreshContext(configuration: Configuration? = null) {
        try {
            val base = hostContext.createPackageContext(
                resPkg,
                CONTEXT_INCLUDE_CODE or CONTEXT_IGNORE_SECURITY
            )
            remoteContext = if (configuration != null) {
                base.createConfigurationContext(configuration)
            } else {
                base
            }
        } catch (e: Exception) {
            Slog.e(TAG, "refreshContext failed", e)
        }
    }

    fun getLayout(layoutName: String): ViewGroup? {
        val ctx = remoteContext ?: return null
        return try {
            val freeformLayoutId = ctx.resources.getIdentifier(layoutName, "layout", resPkg)
            val r = LayoutInflater.from(ctx).inflate(freeformLayoutId, null, false)
            if (null == r) Slog.e(TAG, "can not find layout $layoutName")
            r as ViewGroup
        } catch (e: Exception) {
            Slog.e(TAG, "getLayout", e)
            null
        }
    }

    fun getDrawable(drawableName: String): Drawable? {
        val ctx = remoteContext ?: return null
        return try {
            val id = ctx.resources.getIdentifier(drawableName, "drawable", resPkg)
            if (id != 0) ctx.getDrawable(id) else null
        } catch (e: Exception) {
            Slog.e(TAG, "getDrawable($drawableName)", e)
            null
        }
    }

    fun getColor(colorName: String): Int? {
        val ctx = remoteContext ?: return null
        return try {
            val id = ctx.resources.getIdentifier(colorName, "color", resPkg)
            if (id != 0) ctx.getColor(id) else null
        } catch (e: Exception) {
            Slog.e(TAG, "getColor($colorName)", e)
            null
        }
    }

    fun <T : View> getLayoutChildViewByTag(layout: ViewGroup, tagName: String): T? {
        return try {
            val r = layout.findViewWithTag<T>(tagName)
            if (null == r) Slog.e(TAG, "can not find tag $tagName in $layout")
            r
        } catch (e: Exception) {
            Slog.e(TAG, "getLayoutChildViewByTag", e)
            null
        }
    }
}
