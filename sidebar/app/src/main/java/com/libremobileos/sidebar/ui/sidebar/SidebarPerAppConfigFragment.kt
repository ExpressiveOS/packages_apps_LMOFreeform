/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.libremobileos.sidebar.ui.sidebar

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.android.axion.compose.expressive.ExpressiveEmptyState
import com.android.axion.compose.expressive.ExpressiveSearchBar
import com.android.axion.compose.expressive.ExpressiveWavyCircularProgress
import com.android.axion.compose.preferences.PreferencePosition
import com.android.axion.compose.preferences.PreferenceTokens
import com.android.axion.compose.preferences.SwitchPreference
import com.android.axion.compose.preferences.preferencePosition
import com.android.axion.compose.scaffold.AxionScaffold
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.app.SidebarApplication
import com.libremobileos.sidebar.ui.theme.SidebarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SidebarPerAppConfigFragment : Fragment() {

    companion object {
        const val PREF_AUTO_APPS = "sidebar_auto_apps"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SidebarTheme {
                    val activity = LocalContext.current as? Activity
                    SidebarPerAppConfigScreen(onBack = { activity?.finish() })
                }
            }
        }
    }
}

@Composable
fun SidebarPerAppConfigContent(onBack: () -> Unit = {}) {
    SidebarPerAppConfigScreen(onBack = onBack)
}

@Composable
fun SidebarPerAppConfigScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences(SidebarApplication.CONFIG, Context.MODE_PRIVATE)
    }

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        apps = loadInstalledApps(context)
        isLoading = false
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.label.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AxionScaffold(
        title = stringResource(R.string.sidebar_per_app_config),
        onBackClick = onBack,
        collapsedByDefault = false,
    ) { paddingValues ->
        val startPad = paddingValues.calculateStartPadding(layoutDirection) + 16.dp
        val endPad = paddingValues.calculateEndPadding(layoutDirection) + 16.dp
        val bottomPad = paddingValues.calculateBottomPadding() + 28.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .imePadding(),
        ) {
            Text(
                text = stringResource(R.string.sidebar_auto_enable_selected_apps_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = startPad + 4.dp,
                    end = endPad + 4.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                ),
            )

            ExpressiveSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.sidebar_search_apps_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPad, end = endPad, top = 4.dp, bottom = 8.dp),
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPad),
                        contentAlignment = Alignment.Center,
                    ) {
                        ExpressiveWavyCircularProgress()
                    }
                }
                filteredApps.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPad),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isSearch = searchQuery.isNotBlank()
                        ExpressiveEmptyState(
                            icon = if (isSearch) Icons.Rounded.SearchOff else Icons.Rounded.Apps,
                            title = stringResource(
                                if (isSearch) R.string.sidebar_empty_search_title
                                else R.string.sidebar_empty_apps_title
                            ),
                            description = if (isSearch) {
                                stringResource(R.string.sidebar_empty_search_summary, searchQuery.trim())
                            } else {
                                stringResource(R.string.sidebar_empty_apps_summary)
                            },
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = startPad,
                            top = 6.dp,
                            end = endPad,
                            bottom = bottomPad,
                        ),
                        verticalArrangement = Arrangement.spacedBy(PreferenceTokens.ItemSpacing),
                    ) {
                        itemsIndexed(
                            items = filteredApps,
                            key = { _, app -> app.packageName },
                        ) { index, appInfo ->
                            SidebarPerAppListItem(
                                appInfo = appInfo,
                                sharedPrefs = sharedPrefs,
                                position = preferencePosition(index, filteredApps.size),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SidebarPerAppListItem(
    appInfo: AppInfo,
    sharedPrefs: SharedPreferences,
    position: PreferencePosition = PreferencePosition.Single,
) {
    val autoApps = sharedPrefs.getStringSet(SidebarPerAppConfigFragment.PREF_AUTO_APPS, emptySet())
        ?: emptySet()
    var isChecked by rememberSaveable(appInfo.packageName) {
        mutableStateOf(autoApps.contains(appInfo.packageName))
    }

    SwitchPreference(
        title = appInfo.label,
        summary = appInfo.packageName,
        iconDrawable = appInfo.icon,
        checked = isChecked,
        position = position,
        onCheckedChange = { newValue ->
            isChecked = newValue
            val currentAutoApps = sharedPrefs
                .getStringSet(SidebarPerAppConfigFragment.PREF_AUTO_APPS, emptySet())
                ?.toMutableSet()
                ?: mutableSetOf()

            if (newValue) {
                currentAutoApps.add(appInfo.packageName)
            } else {
                currentAutoApps.remove(appInfo.packageName)
            }

            sharedPrefs.edit()
                .putStringSet(SidebarPerAppConfigFragment.PREF_AUTO_APPS, currentAutoApps)
                .apply()
        },
    )
}

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
)

private suspend fun loadInstalledApps(context: Context): List<AppInfo> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        apps.asSequence()
            .filter { app ->
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                    app.packageName != context.packageName
            }
            .map { app ->
                AppInfo(
                    label = app.loadLabel(pm).toString(),
                    packageName = app.packageName,
                    icon = app.loadIcon(pm),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
