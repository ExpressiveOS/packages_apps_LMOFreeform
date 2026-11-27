package com.libremobileos.sidebar.ui.all_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.axion.compose.expressive.ExpressiveEmptyState
import com.android.axion.compose.expressive.ExpressiveSearchBar
import com.android.axion.compose.expressive.ExpressiveWavyCircularProgress
import com.android.axion.compose.expressive.rememberExpressiveHaptics
import com.android.axion.compose.expressive.rememberExpressiveReducedMotion
import com.android.axion.compose.scaffold.AxionScaffold
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.bean.AppInfo

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AllAppGridView(
    viewModel: AllAppViewModel,
    onClick: (AppInfo) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val appList by viewModel.appListFlow.collectAsState()
    val isLoading by viewModel.isLoadingFlow.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredApps = remember(appList, searchQuery) {
        if (searchQuery.isBlank()) {
            appList
        } else {
            appList.filter { app ->
                app.label.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AxionScaffold(
        title = stringResource(R.string.sidebar_label),
        onBackClick = onBack,
        collapsedByDefault = true,
        modifier = modifier,
    ) { paddingValues ->
        val startPad = paddingValues.calculateStartPadding(layoutDirection) + 16.dp
        val endPad = paddingValues.calculateEndPadding(layoutDirection) + 16.dp
        val bottomPad = paddingValues.calculateBottomPadding() + 24.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .imePadding(),
        ) {
            ExpressiveSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.sidebar_search_apps_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPad, end = endPad, top = 8.dp, bottom = 8.dp),
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
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 84.dp),
                        contentPadding = PaddingValues(
                            start = startPad,
                            top = 8.dp,
                            end = endPad,
                            bottom = bottomPad,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = filteredApps,
                            key = { "${it.packageName}/${it.activityName}/${it.userId}" },
                        ) { appInfo ->
                            AllAppGridItem(
                                appInfo = appInfo,
                                onClick = onClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AllAppGridItem(
    appInfo: AppInfo,
    onClick: (AppInfo) -> Unit,
) {
    val reducedMotion = rememberExpressiveReducedMotion()
    val haptics = rememberExpressiveHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.94f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            MaterialTheme.motionScheme.fastSpatialSpec()
        },
        label = "AllAppGridItemScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
            ) {
                haptics.performConfirm()
                onClick(appInfo)
            }
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Image(
            painter = rememberDrawablePainter(appInfo.icon),
            contentDescription = appInfo.label,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = appInfo.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
