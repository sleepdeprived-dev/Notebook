package com.david.notebook.ui.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.david.notebook.R
import com.david.notebook.data.todo.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Add or edit a task: title, optional subject, optional due date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(
    existing: Todo?,
    subjects: List<SubjectOption>,
    onSave: (title: String, subjectId: Long?, dueDate: Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var subjectId by rememberSaveable { mutableStateOf(existing?.subjectId) }
    var dueDate by rememberSaveable { mutableStateOf(existing?.dueDate) }
    var pickingDate by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val today = LocalDate.now().toEpochDay()
    val canSave = title.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Text(
                stringResource(if (existing == null) R.string.todo_new else R.string.todo_edit),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.todo_title)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(title, subjectId, dueDate) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            SheetLabel(stringResource(R.string.todo_due))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = dueDate == today,
                        onClick = { dueDate = if (dueDate == today) null else today },
                        label = { Text(stringResource(R.string.due_today)) },
                    )
                }
                item {
                    FilterChip(
                        selected = dueDate == today + 1,
                        onClick = { dueDate = if (dueDate == today + 1) null else today + 1 },
                        label = { Text(stringResource(R.string.due_tomorrow)) },
                    )
                }
                item {
                    val custom = dueDate?.takeIf { it != today && it != today + 1 }
                    if (custom != null) {
                        InputChip(
                            selected = true,
                            onClick = { pickingDate = true },
                            label = { Text(formatDate(custom)) },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_event), null, Modifier.size(18.dp)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.todo_clear_due),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { dueDate = null },
                                )
                            },
                        )
                    } else {
                        FilterChip(
                            selected = false,
                            onClick = { pickingDate = true },
                            label = { Text(stringResource(R.string.due_pick)) },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_event), null, Modifier.size(18.dp)) },
                        )
                    }
                }
            }

            SheetLabel(stringResource(R.string.todo_subject))
            if (subjects.isEmpty()) {
                Text(
                    stringResource(R.string.todo_no_subjects),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjects, key = { it.id }) { subject ->
                        FilterChip(
                            selected = subjectId == subject.id,
                            onClick = { subjectId = if (subjectId == subject.id) null else subject.id },
                            label = { Text(subject.name) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSave(title, subjectId, dueDate) }, enabled = canSave) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }

    // A brand-new task: put the cursor in the title straight away.
    if (existing == null) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    if (pickingDate) {
        // The picker works in UTC milliseconds at midnight; the task stores a plain day.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (dueDate ?: today).let {
                LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dueDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                Row {
                    if (dueDate != null) {
                        TextButton(onClick = { dueDate = null; pickingDate = false }) {
                            Text(stringResource(R.string.todo_clear_due))
                        }
                    }
                    TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun formatDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** How a due date reads on a task card, relative to [today]. */
data class DueText(val text: String, val overdue: Boolean, val today: Boolean)

@Composable
fun dueText(epochDay: Long, today: LocalDate): DueText {
    val date = LocalDate.ofEpochDay(epochDay)
    val days = epochDay - today.toEpochDay()
    val text = when {
        days < -1 -> pluralStringResource(R.plurals.due_overdue_days, (-days).toInt(), (-days).toInt())
        days == -1L -> stringResource(R.string.due_yesterday)
        days == 0L -> stringResource(R.string.due_today)
        days == 1L -> stringResource(R.string.due_tomorrow)
        // Within the week, the weekday says more than a date: "Friday".
        days < 7 -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("d MMM"))
        else -> formatDate(epochDay)
    }
    return DueText(text, overdue = days < 0, today = days == 0L)
}
