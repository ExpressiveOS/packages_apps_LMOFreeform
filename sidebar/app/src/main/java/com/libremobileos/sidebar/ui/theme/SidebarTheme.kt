package com.libremobileos.sidebar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.android.axion.compose.theme.AxionTheme

@Composable
fun SidebarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    applySystemBars: Boolean = true,
    content: @Composable () -> Unit,
) {
    AxionTheme(
        darkTheme = darkTheme,
        applySystemBars = applySystemBars,
        content = content,
    )
}
