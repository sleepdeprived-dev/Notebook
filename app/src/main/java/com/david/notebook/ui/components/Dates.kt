package com.david.notebook.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.david.notebook.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Dates are stored as plain days (LocalDate.toEpochDay) and times as minutes after
// midnight, so nothing moves when the phone changes time zone. Material's date picker
// speaks UTC milliseconds at midnight; these convert between the two.

fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun pickerMillisToEpochDay(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()

/** "27 Oct 2026", in the phone's language. */
fun formatDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** "14:30" or "2:30 PM", following the phone's 24-hour setting. */
fun formatTime(context: Context, minutes: Int): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern(pattern))
}

@Composable
fun formatTime(minutes: Int): String = formatTime(LocalContext.current, minutes)

/** Material date picker; [onClear] adds a "Remove date" button when given. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDateDialog(
    initial: Long?,
    onPick: (Long) -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.ofEpochDay(initial ?: LocalDate.now().toEpochDay()).toPickerMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(pickerMillisToEpochDay(it)) }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            Row {
                if (onClear != null) {
                    TextButton(onClick = { onClear(); onDismiss() }) { Text(stringResource(R.string.remove_date)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/** Material clock picker in a dialog; [onClear] adds a "No time" button when given. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickTimeDialog(
    initial: Int?,
    onPick: (Int) -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val start = initial ?: (9 * 60)
    val state = rememberTimePickerState(
        initialHour = start / 60,
        initialMinute = start % 60,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPick(state.hour * 60 + state.minute); onDismiss() }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            Row {
                if (onClear != null) {
                    TextButton(onClick = { onClear(); onDismiss() }) { Text(stringResource(R.string.no_time)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
