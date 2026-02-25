package com.andrin.examcountdown

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.andrin.examcountdown.ui.ExamCountdownScreen
import com.andrin.examcountdown.ui.ExamViewModel
import com.andrin.examcountdown.ui.HomeTab
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : FragmentActivity() {
    private val startTabRoute = mutableStateOf(HomeTab.EXAMS.route)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateStartTabFromIntent()
        requestNotificationPermissionIfNeeded()
        setContent {
            val examViewModel: ExamViewModel = viewModel()
            val accessibilityModeEnabled by examViewModel.accessibilityModeEnabled.collectAsStateWithLifecycle()
            ExamCountdownTheme(accessibilityMode = accessibilityModeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExamCountdownScreen(
                        initialTab = HomeTab.fromRoute(startTabRoute.value),
                        viewModel = examViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateStartTabFromIntent()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun updateStartTabFromIntent() {
        val route = intent?.getStringExtra(EXTRA_OPEN_TAB)
        if (!route.isNullOrBlank()) {
            startTabRoute.value = route
        }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
    }
}
