package com.david.notebook.data.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.david.notebook.MainActivity
import com.david.notebook.NotebookApplication
import com.david.notebook.R
import com.david.notebook.data.settings.SettingsRepository
import com.david.notebook.data.tests.AssessmentDao
import com.david.notebook.data.todo.TodoDao
import com.david.notebook.ui.components.formatTime
import com.david.notebook.ui.navigation.Tab
import com.david.notebook.ui.tests.label
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** One notification, shown at [at]. [key] names it across reschedules. */
private data class Reminder(val key: String, val at: LocalDateTime, val title: String, val text: String, val tab: Tab)

/**
 * Turns upcoming tests and due tasks into alarms:
 * - a test: the evening before (19:00) and the morning of (08:00, or an hour before an
 *   earlier start);
 * - a task with a due date: 09:00 that day, or half an hour before its time.
 *
 * [reschedule] cancels everything it set last time and sets it all again, so it can be
 * called after any change. Alarms are exact where Android allows it, otherwise inexact
 * (which can arrive up to an hour late).
 */
class ReminderScheduler(
    private val context: Context,
    private val todos: TodoDao,
    private val assessments: AssessmentDao,
    private val settings: SettingsRepository,
) {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()

    suspend fun reschedule() = mutex.withLock {
        prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().forEach { key ->
            pendingIntent(key, null, PendingIntent.FLAG_NO_CREATE)?.let {
                alarms.cancel(it)
                it.cancel()
            }
        }
        val now = LocalDateTime.now()
        val upcoming = if (settings.settings.value.reminders) build().filter { it.at.isAfter(now) } else emptyList()
        upcoming.forEach { reminder ->
            val millis = reminder.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val intent = pendingIntent(reminder.key, reminder, PendingIntent.FLAG_UPDATE_CURRENT) ?: return@forEach
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
            }
        }
        prefs.edit { putStringSet(KEY_SCHEDULED, upcoming.map { it.key }.toSet()) }
    }

    private suspend fun build(): List<Reminder> {
        val today = LocalDate.now().toEpochDay()
        val reminders = mutableListOf<Reminder>()

        assessments.upcoming(today).forEach { item ->
            val a = item.assessment
            val kind = context.getString(a.assessmentKind().label())
            val name = a.title.ifBlank { item.subjectName ?: kind }
            val text = listOfNotNull(
                name,
                item.subjectName?.takeIf { a.title.isNotBlank() },
                a.time?.let { formatTime(context, it) },
            ).joinToString(" · ")
            val date = LocalDate.ofEpochDay(a.date)
            reminders += Reminder(
                key = "test-${a.id}-eve",
                at = date.minusDays(1).atTime(19, 0),
                title = context.getString(R.string.reminder_test_tomorrow, kind),
                text = text,
                tab = Tab.Tests,
            )
            val morning = a.time?.takeIf { it < 9 * 60 }
                ?.let { date.atStartOfDay().plusMinutes((it - 60).coerceAtLeast(0).toLong()) }
                ?: date.atTime(8, 0)
            reminders += Reminder(
                key = "test-${a.id}-day",
                at = morning,
                title = context.getString(R.string.reminder_test_today, kind),
                text = text,
                tab = Tab.Tests,
            )
        }

        todos.openFrom(today).forEach { todo ->
            val date = LocalDate.ofEpochDay(todo.dueDate ?: return@forEach)
            reminders += Reminder(
                key = "todo-${todo.id}",
                at = todo.time?.let { date.atStartOfDay().plusMinutes((it - 30).coerceAtLeast(0).toLong()) }
                    ?: date.atTime(9, 0),
                title = todo.time?.let { context.getString(R.string.reminder_task_due_at, formatTime(context, it)) }
                    ?: context.getString(R.string.reminder_task_due_today),
                text = todo.title,
                tab = Tab.Todo,
            )
        }
        return reminders
    }

    /** The data URI makes each reminder's intent distinct, so it can be cancelled by key alone. */
    private fun pendingIntent(key: String, reminder: Reminder?, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .setData("notebook://reminder/$key".toUri())
        if (reminder != null) {
            intent.putExtra(EXTRA_TITLE, reminder.title)
                .putExtra(EXTRA_TEXT, reminder.text)
                .putExtra(EXTRA_TAB, reminder.tab.route)
        }
        return PendingIntent.getBroadcast(context, 0, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val ACTION_REMIND = "com.david.notebook.REMIND"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_TAB = "tab"
        private const val KEY_SCHEDULED = "scheduled"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.reminders_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun canNotify(context: Context): Boolean =
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                    )
    }
}

/**
 * Shows a reminder when its alarm goes off, and sets the alarms again after a reboot,
 * an app update or a clock/time zone change (alarms don't survive the first two, and
 * "19:00" means a different instant after the last).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ReminderScheduler.ACTION_REMIND) {
            show(context, intent)
            return
        }
        val app = context.applicationContext as NotebookApplication
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.reminders.reschedule()
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, intent: Intent) {
        if (!ReminderScheduler.canNotify(context)) return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: return
        val text = intent.getStringExtra(ReminderScheduler.EXTRA_TEXT).orEmpty()
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(MainActivity.EXTRA_TAB, intent.getStringExtra(ReminderScheduler.EXTRA_TAB))
        val id = intent.data.toString().hashCode()
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_assignment)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and now.
        }
    }
}
