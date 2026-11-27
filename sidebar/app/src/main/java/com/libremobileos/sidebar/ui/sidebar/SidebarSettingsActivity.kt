package com.libremobileos.sidebar.ui.sidebar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.libremobileos.sidebar.ui.theme.SidebarTheme

class SidebarSettingsActivity : ComponentActivity() {
    private val viewModel: SidebarSettingsViewModel by viewModels { SidebarSettingsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SidebarTheme {
                SidebarSettingsPage(
                    viewModel = viewModel,
                    onBack = { finish() },
                )
            }
        }
    }
}
