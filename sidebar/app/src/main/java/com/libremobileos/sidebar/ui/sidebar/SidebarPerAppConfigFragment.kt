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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
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
    val sharedPrefs = context.getSharedPreferences(SidebarApplication.CONFIG, Context.MODE_PRIVATE)

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = loadInstalledApps(context)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.sidebar_auto_enable_selected_apps_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(android.R.string.search_go)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = null,
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
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
