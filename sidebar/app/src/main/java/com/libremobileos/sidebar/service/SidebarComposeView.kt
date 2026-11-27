package com.libremobileos.sidebar.service

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.compose.expressive.ExpressiveBlurSurface
import com.android.axion.compose.expressive.ExpressiveMorphContainer
import com.android.axion.compose.expressive.ExpressiveShapePreset
import com.android.axion.compose.expressive.rememberExpressiveHaptics
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.app.SidebarApplication
import com.libremobileos.sidebar.bean.AppInfo

private const val MIN_RAIL_TARGET_DP = 48

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SidebarComposeView(
    viewModel: ServiceViewModel,
    launchApp: (AppInfo) -> Unit,
    closeSidebar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences(SidebarApplication.CONFIG, Context.MODE_PRIVATE)
    }

    var prefsRevision by remember { mutableIntStateOf(0) }
    DisposableEffect(sharedPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            prefsRevision++
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val columnCount = remember(prefsRevision) {
        sharedPrefs.getInt(SidebarService.SIDEBAR_COLUMNS, 1).coerceIn(1, 3)
    }
    val iconSize = remember(prefsRevision) {
        sharedPrefs.getInt(SidebarService.SIDEBAR_ICON_SIZE, 42).coerceIn(28, 80)
    }
    val iconPadding = remember(prefsRevision) {
        sharedPrefs.getInt(SidebarService.SIDEBAR_ICON_PADDING, 6).coerceIn(2, 20)
    }
    val columnSpacing = remember(prefsRevision) {
        sharedPrefs.getInt(SidebarService.SIDEBAR_COLUMN_SPACING, 4).coerceIn(1, 16)
    }
    val cornerRadius = remember(prefsRevision) {
        sharedPrefs.getFloat(SidebarService.SIDEBAR_CORNER_RADIUS, 28f).coerceIn(0f, 36f)
    }
    val backgroundTransparency = remember(prefsRevision) {
        sharedPrefs.getFloat(
            SidebarService.SIDEBAR_BACKGROUND_TRANSPARENCY,
            0.84f,
        ).coerceIn(0.1f, 1.0f)
    }
    val showShadow = remember(prefsRevision) {
        sharedPrefs.getBoolean(SidebarService.SIDEBAR_SHOW_SHADOW, true)
    }

    val sidebarAppList by viewModel.sidebarAppListFlow.collectAsState()
    val railShape = RoundedCornerShape(cornerRadius.dp)

    val surfaceModifier = if (showShadow) {
        modifier.shadow(
            elevation = 10.dp,
            shape = railShape,
            ambientColor = Color.Black.copy(alpha = 0.22f),
            spotColor = Color.Black.copy(alpha = 0.32f),
        )
    } else {
        modifier
    }

    ExpressiveBlurSurface(
        modifier = surfaceModifier,
        shape = railShape,
        tintColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        scrimAlpha = backgroundTransparency,
        contentAlignment = Alignment.Center,
    ) {
        if (columnCount == 1) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SidebarAllAppsPill(
                    appInfo = viewModel.allAppActivity,
                    iconSize = iconSize,
                    iconPadding = iconPadding,
                    launchApp = launchApp,
                )

                Spacer(modifier = Modifier.height(6.dp))
                RailPillDivider()
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = true),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(
                        items = sidebarAppList,
                        key = { "${it.packageName}/${it.activityName}/${it.userId}" },
                    ) { app ->
                        SidebarAppItem(
                            appInfo = app,
                            iconSize = iconSize,
                            iconPadding = iconPadding,
                            launchApp = launchApp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                RailPillDivider()
                Spacer(modifier = Modifier.height(6.dp))

                SidebarSettingsPill(
                    iconSize = iconSize,
                    iconPadding = iconPadding,
                    viewModel = viewModel,
                    closeSidebar = closeSidebar,
                )
            }
        } else {
            val allItems = buildList {
                add(SidebarItem.AllApps(viewModel.allAppActivity))
                addAll(sidebarAppList.map { SidebarItem.App(it) })
                add(SidebarItem.Settings)
            }
            val tileSlotDp = (iconSize + iconPadding * 2).coerceAtLeast(MIN_RAIL_TARGET_DP)
            val gridWidth = (
                tileSlotDp * columnCount +
                    columnSpacing * (columnCount - 1) +
                    16
                ).dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(columnSpacing.dp),
                verticalArrangement = Arrangement.spacedBy(columnSpacing.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(gridWidth),
            ) {
                items(allItems) { item ->
                    when (item) {
                        is SidebarItem.AllApps -> SidebarAllAppsPill(
                            appInfo = item.appInfo,
                            iconSize = iconSize,
                            iconPadding = iconPadding,
                            launchApp = launchApp,
                        )
                        is SidebarItem.App -> SidebarAppItem(
                            appInfo = item.appInfo,
                            iconSize = iconSize,
                            iconPadding = iconPadding,
                            launchApp = launchApp,
                        )
                        is SidebarItem.Settings -> SidebarSettingsPill(
                            iconSize = iconSize,
                            iconPadding = iconPadding,
                            viewModel = viewModel,
                            closeSidebar = closeSidebar,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RailPillDivider() {
    Box(
        modifier = Modifier
            .width(22.dp)
            .height(2.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    )
}

@Composable
private fun SidebarAllAppsPill(
    appInfo: AppInfo,
    iconSize: Int,
    iconPadding: Int,
    launchApp: (AppInfo) -> Unit,
) {
    val haptics = rememberExpressiveHaptics()
    val containerSize = (iconSize + iconPadding * 2).coerceAtLeast(MIN_RAIL_TARGET_DP).dp

    ExpressiveMorphContainer(
        active = false,
        inactivePreset = ExpressiveShapePreset.SQUIRCLE,
        activePreset = ExpressiveShapePreset.PILL,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        hapticOnClick = false,
        onClick = {
            haptics.performConfirm()
            launchApp(appInfo)
        },
        modifier = Modifier.size(containerSize),
    ) {
        Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = appInfo.label.ifBlank { stringResource(R.string.app_name) },
            modifier = Modifier.size((iconSize - 14).coerceAtLeast(20).dp),
        )
    }
}

@Composable
private fun SidebarAppItem(
    appInfo: AppInfo,
    iconSize: Int,
    iconPadding: Int,
    launchApp: (AppInfo) -> Unit,
) {
    val haptics = rememberExpressiveHaptics()
    val containerSize = (iconSize + iconPadding * 2).coerceAtLeast(MIN_RAIL_TARGET_DP).dp

    ExpressiveMorphContainer(
        active = false,
        inactivePreset = ExpressiveShapePreset.SQUIRCLE,
        activePreset = ExpressiveShapePreset.PILL,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
        hapticOnClick = false,
        onClick = {
            haptics.performConfirm()
            launchApp(appInfo)
        },
        modifier = Modifier.size(containerSize),
    ) {
        Image(
            painter = rememberDrawablePainter(appInfo.icon),
            contentDescription = appInfo.label,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}

@Composable
private fun SidebarSettingsPill(
    iconSize: Int,
    iconPadding: Int,
    viewModel: ServiceViewModel,
    closeSidebar: () -> Unit,
) {
    val haptics = rememberExpressiveHaptics()
    val containerSize = (iconSize + iconPadding * 2).coerceAtLeast(MIN_RAIL_TARGET_DP).dp

    ExpressiveMorphContainer(
        active = false,
        inactivePreset = ExpressiveShapePreset.SQUIRCLE,
        activePreset = ExpressiveShapePreset.PILL,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        hapticOnClick = false,
        onClick = {
            haptics.performConfirm()
            viewModel.openSidebarSettings()
            closeSidebar()
        },
        modifier = Modifier.size(containerSize),
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.sidebar_settings_description),
            modifier = Modifier.size((iconSize - 16).coerceAtLeast(20).dp),
        )
    }
}

private sealed class SidebarItem {
    data class AllApps(val appInfo: AppInfo) : SidebarItem()
    data class App(val appInfo: AppInfo) : SidebarItem()
    data object Settings : SidebarItem()
}
