package com.livemedica.helix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. All navigation happens in Compose; this class only owns window-level
 * concerns — edge-to-edge insets and attaching the theme.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Called before super so the decor is configured for edge-to-edge from the first frame,
        // avoiding a visible inset jump on cold start.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: HelixAppViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            HelixTheme(themePreference = settings.themePreference) {
                HelixApp(viewModel = viewModel)
            }
        }
    }
}
