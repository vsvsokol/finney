package ru.finney.pet.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import ru.finney.pet.FinneyApplication

/**
 * Спросить разрешение на уведомления (Android 13+) — один раз, когда питомец уже есть:
 * ребёнок понимает, о ком будут напоминания. Отказ ничего не ломает — просто без уведомлений.
 */
@Composable
fun AskNotificationsOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val prefs = (context.applicationContext as FinneyApplication).container.notificationPrefs
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted || prefs.permissionAsked()) return@LaunchedEffect
        prefs.setPermissionAsked()
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
