/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.libremobileos.sidebar.ui.sidebar

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.compose.expressive.rememberExpressiveHaptics
import com.android.axion.compose.preferences.ClickablePreference
import com.android.axion.compose.preferences.MainSwitchPreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.preferences.PreferenceTokens
import com.android.axion.compose.preferences.SwitchPreference
import com.android.axion.compose.preferences.preferencePosition
import com.android.axion.compose.scaffold.AxionScaffold
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.bean.SidebarAppInfo
import com.libremobileos.sidebar.service.SidebarMonitorService

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SidebarSettingsPage(
    viewModel: SidebarSettingsViewModel,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = rememberExpressiveHaptics()
    var mainChecked by rememberSaveable { mutableStateOf(viewModel.getSidebarEnabled()) }
    var autoEnableChecked by rememberSaveable {
        mutableStateOf(viewModel.getAutoEnableSelectedAppsEnabled())
    }
    var predictedChecked by rememberSaveable {
        mutableStateOf(viewModel.getPredictedAppsEnabled())
    }
    val sidebarApps by viewModel.appListFlow.collectAsState()

    val openCustomization = {
        haptics.performConfirm()
        context.startActivity(Intent(context, SidebarCustomizationActivity::class.java))
    }

    AxionScaffold(
        title = stringResource(R.string.sidebar_label),
        onBackClick = onBack,
        collapsedByDefault = false,
        actions = {
            FilledTonalIconButton(
                onClick = openCustomization,
                shape = IconButtonDefaults.smallRoundShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.sidebar_customization_title),
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(PreferenceTokens.ItemSpacing),
        ) {
            item(key = "main_switch") {
                MainSwitchPreference(
                    title = stringResource(R.string.enable_sideline),
                    summary = stringResource(R.string.sidebar_summary),
                    checked = mainChecked,
                    enabled = viewModel.isEnabled,
                    onCheckedChange = { enabled ->
                        mainChecked = enabled
                        viewModel.setSidebarEnabled(enabled)
                        val intent = Intent(context, SidebarMonitorService::class.java)
                        if (enabled || autoEnableChecked) {
                            context.startService(intent)
                        } else {
                            context.stopService(intent)
                        }
                    },
                )
            }

            item(key = "behavior_group") {
                PreferenceGroup(
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    item {
                        ClickablePreference(
                            title = stringResource(R.string.sidebar_customization_title),
                            summary = stringResource(R.string.sidebar_section_layout),
                            icon = Icons.Rounded.Tune,
                            enabled = viewModel.isEnabled,
                            onClick = openCustomization,
                        )
                    }

                    item {
                        SwitchPreference(
                            title = stringResource(R.string.sidebar_auto_enable_selected_apps),
                            summary = stringResource(R.string.sidebar_auto_enable_selected_apps_summary),
                            icon = Icons.Rounded.ViewSidebar,
                            checked = autoEnableChecked,
                            enabled = viewModel.isEnabled,
                            onCheckedChange = { enabled ->
                                autoEnableChecked = enabled
                                viewModel.setAutoEnableSelectedAppsEnabled(enabled)
                                val intent = Intent(context, SidebarMonitorService::class.java)
                                if (enabled || mainChecked) {
                                    context.startService(intent)
                                } else {
                                    context.stopService(intent)
                                }
                            },
                        )
                    }

                    if (autoEnableChecked) {
                        item {
                            ClickablePreference(
                                title = stringResource(R.string.sidebar_per_app_config),
                                summary = stringResource(R.string.sidebar_per_app_config_summary),
                                icon = Icons.Rounded.Apps,
                                enabled = viewModel.isEnabled,
                                onClick = {
                                    haptics.performConfirm()
                                    context.startActivity(
                                        Intent(context, SidebarPerAppConfigActivity::class.java)
                                    )
                                },
                            )
                        }
                    }

                    if (mainChecked) {
                        item {
                            SwitchPreference(
                                title = stringResource(R.string.sidebar_predicted_apps),
                                summary = stringResource(R.string.sidebar_predicted_apps_summary),
                                icon = Icons.Rounded.AutoAwesome,
                                checked = predictedChecked,
                                enabled = viewModel.isEnabled,
                                onCheckedChange = { enabled ->
                                    predictedChecked = enabled
                                    viewModel.setPredictedAppsEnabled(enabled)
                                },
                            )
                        }
                    }
                }
            }

            if (mainChecked && sidebarApps.isNotEmpty()) {
                item(key = "pinned_apps_header") {
                    Text(
                        text = stringResource(R.string.sidebar_app_setting_label),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = PreferenceTokens.CategoryPaddingStart,
                                top = PreferenceTokens.CategoryPaddingTop,
                                bottom = PreferenceTokens.CategoryPaddingBottom,
                            ),
                    )
                }

                itemsIndexed(
                    items = sidebarApps,
                    key = { _, app -> "${app.packageName}/${app.activityName}/${app.userId}" },
                ) { index, appInfo ->
                    SidebarAppListItem(
                        appInfo = appInfo,
                        position = preferencePosition(index, sidebarApps.size),
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                viewModel.addSidebarApp(appInfo)
                            } else {
                                viewModel.deleteSidebarApp(appInfo)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SidebarAppListItem(
    appInfo: SidebarAppInfo,
    position: com.android.axion.compose.preferences.PreferencePosition =
        com.android.axion.compose.preferences.PreferencePosition.Single,
    onCheckedChange: (Boolean) -> Unit,
) {
    var appChecked by rememberSaveable(appInfo.packageName, appInfo.activityName, appInfo.userId) {
        mutableStateOf(appInfo.isSidebarApp)
    }

    SwitchPreference(
        title = appInfo.label,
        summary = appInfo.packageName,
        iconDrawable = appInfo.icon,
        checked = appChecked,
        position = position,
        onCheckedChange = { next ->
            appChecked = next
            appInfo.isSidebarApp = next
            onCheckedChange(next)
        },
    )
}
