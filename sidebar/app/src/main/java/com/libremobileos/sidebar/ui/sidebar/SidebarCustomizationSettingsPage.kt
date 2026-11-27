/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.libremobileos.sidebar.ui.sidebar

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.compose.expressive.ExpressiveMorphButton
import com.android.axion.compose.expressive.ExpressiveSegmentOption
import com.android.axion.compose.expressive.ExpressiveSegmentedButtonRow
import com.android.axion.compose.preferences.LocalPreferencePosition
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.preferences.PreferenceTokens
import com.android.axion.compose.preferences.SliderPreference
import com.android.axion.compose.preferences.SwitchPreference
import com.android.axion.compose.preferences.preferenceShape
import com.android.axion.compose.scaffold.AxionScaffold
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.service.SidebarService
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SidebarCustomizationSettingsPage(
    sharedPrefs: SharedPreferences,
    onBack: () -> Unit = {},
    onSettingChanged: () -> Unit = {},
) {
    var transparency by remember {
        mutableFloatStateOf(sharedPrefs.getFloat(SidebarService.SLIDER_TRANSPARENCY, 0.80f))
    }
    var sliderLength by remember {
        mutableIntStateOf(sharedPrefs.getInt(SidebarService.SLIDER_LENGTH, 200))
    }
    var sliderWidth by remember {
        mutableIntStateOf(sharedPrefs.getInt(SidebarService.SLIDER_WIDTH, 100))
    }
    var position by remember {
        mutableIntStateOf(sharedPrefs.getInt(SidebarService.SIDELINE_POSITION_X, 1))
    }
    var columnCount by remember {
        mutableIntStateOf(sharedPrefs.getInt(SidebarService.SIDEBAR_COLUMNS, 1))
    }
    var iconSize by remember {
        mutableIntStateOf(sharedPrefs.getInt(SidebarService.SIDEBAR_ICON_SIZE, 42))
    }
    var iconPadding by remember {
        mutableIntStateOf(sharedPrefs.getInt(SidebarService.SIDEBAR_ICON_PADDING, 6))
    }
    var columnSpacing by remember {
        mutableIntStateOf(sharedPrefs.getInt(SidebarService.SIDEBAR_COLUMN_SPACING, 4))
    }
    var cornerRadius by remember {
        mutableFloatStateOf(sharedPrefs.getFloat(SidebarService.SIDEBAR_CORNER_RADIUS, 28f))
    }
    var backgroundTransparency by remember {
        mutableFloatStateOf(
            sharedPrefs.getFloat(SidebarService.SIDEBAR_BACKGROUND_TRANSPARENCY, 0.84f)
        )
    }
    var showShadow by remember {
        mutableStateOf(sharedPrefs.getBoolean(SidebarService.SIDEBAR_SHOW_SHADOW, true))
    }
    var tapToOpen by remember {
        mutableStateOf(sharedPrefs.getBoolean(SidebarService.SIDEBAR_TAP_TO_OPEN, false))
    }
    var swipeToOpen by remember {
        mutableStateOf(sharedPrefs.getBoolean(SidebarService.SIDEBAR_SWIPE_TO_OPEN, true))
    }
    var hideOnGameSpace by remember {
        mutableStateOf(sharedPrefs.getBoolean(SidebarService.SIDEBAR_HIDE_ON_GAMESPACE, false))
    }

    AxionScaffold(
        title = stringResource(R.string.sidebar_customization_title),
        onBackClick = onBack,
        collapsedByDefault = false,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "slider_section") {
                PreferenceGroup(
                    title = stringResource(R.string.sidebar_section_slider),
                ) {
                    item {
                        SliderPreference(
                            title = stringResource(R.string.sidebar_transparency_value, transparency)
                                .substringBefore(":"),
                            summary = "",
                            value = transparency,
                            onValueChange = { next ->
                                val rounded = (next * 20f).roundToInt() / 20f
                                transparency = rounded
                                sharedPrefs.edit()
                                    .putFloat(SidebarService.SLIDER_TRANSPARENCY, rounded)
                                    .apply()
                                onSettingChanged()
                            },
                            onValueChangeFinished = {},
                            onReset = {
                                transparency = 0.80f
                                sharedPrefs.edit()
                                    .putFloat(SidebarService.SLIDER_TRANSPARENCY, 0.80f)
                                    .apply()
                                onSettingChanged()
                            },
                            valueRange = 0.10f..1.00f,
                            steps = 17,
                            displayValue = "${(transparency * 100).roundToInt()}%",
                        )
                    }

                    item {
                        SliderPreference(
                            title = stringResource(R.string.sidebar_length_value, sliderLength)
                                .substringBefore(":"),
                            summary = "",
                            value = sliderLength.toFloat(),
                            onValueChange = { next ->
                                val stepped = ((next - 100f) / 20f).roundToInt() * 20 + 100
                                sliderLength = stepped.coerceIn(100, 500)
                                sharedPrefs.edit()
                                    .putInt(SidebarService.SLIDER_LENGTH, sliderLength)
                                    .apply()
                                onSettingChanged()
                            },
                            onValueChangeFinished = {},
                            onReset = {
                                sliderLength = 200
                                sharedPrefs.edit()
                                    .putInt(SidebarService.SLIDER_LENGTH, 200)
                                    .apply()
                                onSettingChanged()
                            },
                            valueRange = 100f..500f,
                            steps = 19,
                            displayValue = "$sliderLength px",
                        )
                    }

                    item {
                        SliderPreference(
                            title = stringResource(R.string.sidebar_width_value, sliderWidth)
                                .substringBefore(":"),
                            summary = "",
                            value = sliderWidth.toFloat(),
                            onValueChange = { next ->
                                val stepped = ((next - 20f) / 10f).roundToInt() * 10 + 20
                                sliderWidth = stepped.coerceIn(20, 200)
                                sharedPrefs.edit()
                                    .putInt(SidebarService.SLIDER_WIDTH, sliderWidth)
                                    .apply()
                                onSettingChanged()
                            },
                            onValueChangeFinished = {},
                            onReset = {
                                sliderWidth = 100
                                sharedPrefs.edit()
                                    .putInt(SidebarService.SLIDER_WIDTH, 100)
                                    .apply()
                                onSettingChanged()
                            },
                            valueRange = 20f..200f,
                            steps = 17,
                            displayValue = "$sliderWidth px",
                        )
                    }

                    item {
                        SegmentedCardItem(
                            title = stringResource(R.string.sidebar_position_label),
                        ) {
                            ExpressiveSegmentedButtonRow(
                                options = listOf(
                                    ExpressiveSegmentOption(
                                        value = -1,
                                        label = stringResource(R.string.sidebar_left),
                                    ),
                                    ExpressiveSegmentOption(
                                        value = 1,
                                        label = stringResource(R.string.sidebar_right),
                                    ),
                                ),
                                selectedValue = position,
                                onValueSelected = { selected ->
                                    position = selected
                                    sharedPrefs.edit()
                                        .putInt(SidebarService.SIDELINE_POSITION_X, selected)
                                        .apply()
                                    onSettingChanged()
                                },
                            )
                        }
                    }
                }
            }

            item(key = "layout_section") {
                PreferenceGroup(
                    title = stringResource(R.string.sidebar_section_layout),
                ) {
                    item {
                        val colOptions = (1..3).map { count ->
                            ExpressiveSegmentOption(
                                value = count,
                                label = pluralStringResource(
                                    R.plurals.sidebar_columns_value,
                                    count,
                                    count,
                                ),
                            )
                        }
                        SegmentedCardItem(
                            title = pluralStringResource(
                                R.plurals.sidebar_columns_value,
                                columnCount,
                                columnCount,
                            ),
                        ) {
                            ExpressiveSegmentedButtonRow(
                                options = colOptions,
                                selectedValue = columnCount,
                                onValueSelected = { selected ->
                                    columnCount = selected
                                    sharedPrefs.edit()
                                        .putInt(SidebarService.SIDEBAR_COLUMNS, selected)
                                        .apply()
                                    onSettingChanged()
                                },
                            )
                        }
                    }

                    item {
                        SliderPreference(
                            title = stringResource(R.string.sidebar_icon_size_value, iconSize)
                                .substringBefore(":"),
                            summary = "",
                            value = iconSize.toFloat(),
                            onValueChange = { next ->
                                iconSize = next.roundToInt().coerceIn(30, 80)
                                sharedPrefs.edit()
                                    .putInt(SidebarService.SIDEBAR_ICON_SIZE, iconSize)
                                    .apply()
                                onSettingChanged()
                            },
                            onValueChangeFinished = {},
                            onReset = {
                                iconSize = 42
                                sharedPrefs.edit()
                                    .putInt(SidebarService.SIDEBAR_ICON_SIZE, 42)
                                    .apply()
                                onSettingChanged()
                            },
                            valueRange = 30f..80f,
                            steps = 9,
                            displayValue = "$iconSize dp",
                        )
                    }

                    item {
                        SliderPreference(
                            title = stringResource(R.string.sidebar_icon_padding_value, iconPadding)
                                .substringBefore(":"),
                            summary = stringResource(R.string.sidebar_icon_padding_summary),
                            value = iconPadding.toFloat(),
                            onValueChange = { next ->
                                iconPadding = next.roundToInt().coerceIn(4, 20)
                                sharedPrefs.edit()
                                    .putInt(SidebarService.SIDEBAR_ICON_PADDING, iconPadding)
                                    .apply()
                                onSettingChanged()
                            },
                            onValueChangeFinished = {},
                            onReset = {
                                iconPadding = 6
                                sharedPrefs.edit()
                                    .putInt(SidebarService.SIDEBAR_ICON_PADDING, 6)
                                    .apply()
                                onSettingChanged()
                            },
                            valueRange = 4f..20f,
                            steps = 15,
                            displayValue = "$iconPadding dp",
                        )
                    }

                    if (columnCount > 1) {
                        item {
                            SliderPreference(
                                title = stringResource(
                                    R.string.sidebar_column_spacing_value,
                                    columnSpacing,
                                ).substringBefore(":"),
                                summary = stringResource(R.string.sidebar_column_spacing_summary),
                                value = columnSpacing.toFloat(),
                                onValueChange = { next ->
                                    columnSpacing = next.roundToInt().coerceIn(1, 12)
                                    sharedPrefs.edit()
                                        .putInt(SidebarService.SIDEBAR_COLUMN_SPACING, columnSpacing)
                                        .apply()
                                    onSettingChanged()
                                },
                                onValueChangeFinished = {},
                                onReset = {
                                    columnSpacing = 4
                                    sharedPrefs.edit()
                                        .putInt(SidebarService.SIDEBAR_COLUMN_SPACING, 4)
                                        .apply()
                                    onSettingChanged()
                                },
                                valueRange = 1f..12f,
                                steps = 10,
                                displayValue = "$columnSpacing dp",
                            )
                        }
                    }

                    item {
                        SliderPreference(
                            title = stringResource(
                                R.string.sidebar_corner_radius_value,
                                cornerRadius.roundToInt(),
                            ).substringBefore(":"),
                            summary = "",
                            value = cornerRadius,
                            onValueChange = { next ->
                                cornerRadius = next.roundToInt().toFloat().coerceIn(0f, 36f)
                                sharedPrefs.edit()
                                    .putFloat(SidebarService.SIDEBAR_CORNER_RADIUS, cornerRadius)
                                    .apply()
                                onSettingChanged()
                            },
                            onValueChangeFinished = {},
                            onReset = {
                                cornerRadius = 28f
                                sharedPrefs.edit()
                                    .putFloat(SidebarService.SIDEBAR_CORNER_RADIUS, 28f)
                                    .apply()
                                onSettingChanged()
                            },
                            valueRange = 0f..36f,
                            steps = 17,
                            displayValue = "${cornerRadius.roundToInt()} dp",
                        )
                    }

                    item {
                        SliderPreference(
                            title = stringResource(
                                R.string.sidebar_bg_transparency_value,
                                backgroundTransparency,
                            ).substringBefore(":"),
                            summary = "",
                            value = backgroundTransparency,
                            onValueChange = { next ->
                                val rounded = (next * 20f).roundToInt() / 20f
                                backgroundTransparency = rounded.coerceIn(0.10f, 1.00f)
                                sharedPrefs.edit()
                                    .putFloat(
                                        SidebarService.SIDEBAR_BACKGROUND_TRANSPARENCY,
                                        backgroundTransparency,
                                    )
                                    .apply()
                                onSettingChanged()
                            },
                            onValueChangeFinished = {},
                            onReset = {
                                backgroundTransparency = 0.84f
                                sharedPrefs.edit()
                                    .putFloat(SidebarService.SIDEBAR_BACKGROUND_TRANSPARENCY, 0.84f)
                                    .apply()
                                onSettingChanged()
                            },
                            valueRange = 0.10f..1.00f,
                            steps = 17,
                            displayValue = String.format(
                                Locale.US,
                                "%d%%",
                                (backgroundTransparency * 100).roundToInt(),
                            ),
                        )
                    }
                }
            }

            item(key = "visual_and_misc_section") {
                PreferenceGroup(
                    title = stringResource(R.string.sidebar_section_visual),
                ) {
                    item {
                        SwitchPreference(
                            title = stringResource(R.string.sidebar_drop_shadow_title),
                            summary = stringResource(R.string.sidebar_drop_shadow_summary),
                            checked = showShadow,
                            onCheckedChange = { next ->
                                showShadow = next
                                sharedPrefs.edit()
                                    .putBoolean(SidebarService.SIDEBAR_SHOW_SHADOW, next)
                                    .apply()
                                onSettingChanged()
                            },
                        )
                    }

                    item {
                        SwitchPreference(
                            title = stringResource(R.string.sidebar_tap_to_open_title),
                            summary = stringResource(R.string.sidebar_tap_to_open_summary),
                            checked = tapToOpen,
                            onCheckedChange = { next ->
                                tapToOpen = next
                                sharedPrefs.edit()
                                    .putBoolean(SidebarService.SIDEBAR_TAP_TO_OPEN, next)
                                    .apply()
                                onSettingChanged()
                            },
                        )
                    }

                    item {
                        SwitchPreference(
                            title = stringResource(R.string.sidebar_swipe_to_open_title),
                            summary = stringResource(R.string.sidebar_swipe_to_open_summary),
                            checked = swipeToOpen,
                            onCheckedChange = { next ->
                                swipeToOpen = next
                                sharedPrefs.edit()
                                    .putBoolean(SidebarService.SIDEBAR_SWIPE_TO_OPEN, next)
                                    .apply()
                                onSettingChanged()
                            },
                        )
                    }

                    item {
                        SwitchPreference(
                            title = stringResource(R.string.sidebar_hide_on_gamespace_title),
                            summary = stringResource(R.string.sidebar_hide_on_gamespace_summary),
                            checked = hideOnGameSpace,
                            onCheckedChange = { next ->
                                hideOnGameSpace = next
                                sharedPrefs.edit()
                                    .putBoolean(SidebarService.SIDEBAR_HIDE_ON_GAMESPACE, next)
                                    .apply()
                                onSettingChanged()
                            },
                        )
                    }
                }
            }

            item(key = "reset_button") {
                Spacer(modifier = Modifier.height(4.dp))
                ExpressiveMorphButton(
                    onClick = {
                        transparency = 0.80f
                        sliderLength = 200
                        sliderWidth = 100
                        position = 1
                        columnCount = 1
                        iconSize = 42
                        iconPadding = 6
                        columnSpacing = 4
                        cornerRadius = 28f
                        backgroundTransparency = 0.84f
                        showShadow = true
                        tapToOpen = false
                        swipeToOpen = true
                        hideOnGameSpace = false

                        sharedPrefs.edit()
                            .putFloat(SidebarService.SLIDER_TRANSPARENCY, transparency)
                            .putInt(SidebarService.SLIDER_LENGTH, sliderLength)
                            .putInt(SidebarService.SLIDER_WIDTH, sliderWidth)
                            .putInt(SidebarService.SIDELINE_POSITION_X, position)
                            .putInt(SidebarService.SIDEBAR_COLUMNS, columnCount)
                            .putInt(SidebarService.SIDEBAR_ICON_SIZE, iconSize)
                            .putInt(SidebarService.SIDEBAR_ICON_PADDING, iconPadding)
                            .putInt(SidebarService.SIDEBAR_COLUMN_SPACING, columnSpacing)
                            .putFloat(SidebarService.SIDEBAR_CORNER_RADIUS, cornerRadius)
                            .putFloat(
                                SidebarService.SIDEBAR_BACKGROUND_TRANSPARENCY,
                                backgroundTransparency,
                            )
                            .putBoolean(SidebarService.SIDEBAR_SHOW_SHADOW, showShadow)
                            .putBoolean(SidebarService.SIDEBAR_TAP_TO_OPEN, tapToOpen)
                            .putBoolean(SidebarService.SIDEBAR_SWIPE_TO_OPEN, swipeToOpen)
                            .putBoolean(SidebarService.SIDEBAR_HIDE_ON_GAMESPACE, hideOnGameSpace)
                            .apply()
                        onSettingChanged()
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.sidebar_reset_defaults))
                }
            }
        }
    }
}

@Composable
private fun SegmentedCardItem(
    title: String,
    content: @Composable () -> Unit,
) {
    val shape = preferenceShape(LocalPreferencePosition.current)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(
                horizontal = PreferenceTokens.PaddingHorizontal,
                vertical = PreferenceTokens.PaddingVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}
