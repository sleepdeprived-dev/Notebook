package com.david.notebook.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.david.notebook.NotebookApplication
import com.david.notebook.data.reminders.ReminderScheduler

/**
 * Returns a function to call after saving something that gets a reminder. The first
 * time (Android 13+, reminders on, permission missing) it asks for the notification
 * permission; after that it does nothing, so saving never nags.
 */
@Composable
fun rememberAskForReminders(): () -> Unit {
    val context = LocalContext.current
    val settings = (context.applicationContext as NotebookApplication).settings
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return remember(launcher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                settings.settings.value.reminders &&
                !settings.askedForNotifications &&
                !ReminderScheduler.canNotify(context)
            ) {
                settings.askedForNotifications = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/**
 * Returns a function that asks for the notification permission, or, if Android won't
 * ask any more (denied twice, or turned off by hand), opens Notebook's notification
 * settings instead.
 */
@Composable
fun rememberAllowNotifications(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted || !ReminderScheduler.canNotify(context)) openNotificationSettings(context)
    }
    return remember(launcher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(context)
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}
