package com.vyrncore.palestra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.RootViewModel
import com.vyrncore.palestra.ui.navigation.PalestraNavGraph
import com.vyrncore.palestra.ui.theme.PalestraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    private lateinit var rootViewModel: RootViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            rootViewModel = hiltViewModel()
            val themeMode by rootViewModel.themeMode.collectAsStateWithLifecycle()
            val role by rootViewModel.role.collectAsStateWithLifecycle()
            PalestraTheme(themeMode = themeMode, role = role) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PalestraNavGraph(rootViewModel = rootViewModel)
                }
            }
        }
        consumeChatDeepLink(intent)
        consumePasswordRecoveryDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The app was already running (warm start) when a chat notification was tapped -
        // onCreate's setContent already ran, so rootViewModel is ready to receive this directly.
        consumeChatDeepLink(intent)
        consumePasswordRecoveryDeepLink(intent)
    }

    /** Tapping a chat-message notification should open straight into that conversation, not just
     * bring the app to whatever screen it was last on - see [NotificationHelper.openAppPendingIntent]. */
    private fun consumeChatDeepLink(intent: Intent) {
        intent.getStringExtra(EXTRA_OPEN_CHAT_PEER_ID)?.let { peerId ->
            if (::rootViewModel.isInitialized) rootViewModel.requestOpenChat(peerId)
        }
    }

    /** Supabase's verify endpoint redirects here after checking a password-reset link, appending
     * the recovery session as a URL fragment (`#access_token=...&type=recovery`) rather than a
     * query string - fragments are never sent to a server, so this is the only place they can be
     * read. [Uri.getFragment] on the deep-link data URI gives us that raw fragment string. */
    private fun consumePasswordRecoveryDeepLink(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme != "vibefitness" || data.host != "reset-password") return
        val fragment = data.fragment ?: data.encodedQuery ?: return
        val params = fragment.split("&").mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) parts[0] to Uri.decode(parts[1]) else null
        }.toMap()
        val accessToken = params["access_token"]
        if (accessToken != null && params["type"] == "recovery" && ::rootViewModel.isInitialized) {
            rootViewModel.requestPasswordRecovery(accessToken)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_OPEN_CHAT_PEER_ID = "open_chat_peer_id"
    }
}
