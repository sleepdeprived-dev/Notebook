package com.david.notebook.ui.tests

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.david.notebook.R
import com.david.notebook.data.tests.Assessment
import com.david.notebook.data.tests.AssessmentKind
import com.david.notebook.ui.components.PickDateDialog
import com.david.notebook.ui.components.PickTimeDialog
import com.david.notebook.ui.components.SubjectChips
import com.david.notebook.ui.components.SubjectOption
import com.david.notebook.ui.components.formatDate
import com.david.notebook.ui.components.formatTime
import java.time.LocalDate

/**
 * Add or edit an exam, colloquium or deadline. A date is required; the name may be
 * left empty when a subject is picked ("Algebra · Exam" says it all). Once the day has
 * come, a grade can be entered. [onAddStudyTask] (upcoming ones only) puts a "Study
 * for …" task in To Do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentSheet(
    existing: Assessment?,
    subjects: List<SubjectOption>,
    onSave: (title: String, kind: AssessmentKind, subjectId: Long?, date: Long, time: Int?, notes: String, grade: Double?) -> Unit,
    onDelete: (() -> Unit)?,
    onAddStudyTask: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(existing?.assessmentKind() ?: AssessmentKind.Exam) }
    var subjectId by rememberSaveable { mutableStateOf(existing?.subjectId) }
    var date by rememberSaveable { mutableStateOf(existing?.date) }
    var time by rememberSaveable { mutableStateOf(existing?.time) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var gradeText by rememberSaveable { mutableStateOf(existing?.grade?.let(::formatGrade).orEmpty()) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    val chosenDate = date
    val today = LocalDate.now().toEpochDay()
    val gradable = chosenDate != null && chosenDate <= today
    val gradeOk = !gradable || gradeText.isBlank() || parseGrade(gradeText) != null
    val canSave = chosenDate != null && (title.isNotBlank() || subjectId != null) && gradeOk

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Text(
                stringResource(if (existing == null) R.string.test_new else R.string.test_edit),
                style = MaterialTheme.typography.titleLarge,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssessmentKind.entries.forEach { option ->
                    FilterChip(
                        selected = kind == option,
                        onClick = { kind = option },
                        label = { Text(stringResource(option.label())) },
                    )
                }
            }

            SheetLabel(stringResource(R.string.todo_subject))
            SubjectChips(subjects, selected = subjectId, onSelect = { subjectId = it })

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = {
                    Text(stringResource(if (subjectId != null) R.string.test_title_optional else R.string.test_title))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            SheetLabel(stringResource(R.string.test_when))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (chosenDate == null) {
                    FilterChip(
                        selected = false,
                        onClick = { pickingDate = true },
                        label = { Text(stringResource(R.string.due_pick)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_event), null, Modifier.size(18.dp)) },
                    )
                } else {
                    InputChip(
                        selected = true,
                        onClick = { pickingDate = true },
                        label = { Text(formatDate(chosenDate)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_event), null, Modifier.size(18.dp)) },
                    )
                }
                val chosenTime = time
                if (chosenTime == null) {
                    FilterChip(
                        selected = false,
                        onClick = { pickingTime = true },
                        label = { Text(stringResource(R.string.add_time)) },
                    )
                } else {
                    InputChip(
                        selected = true,
                        onClick = { pickingTime = true },
                        label = { Text(formatTime(chosenTime)) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.no_time),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { time = null },
                            )
                        },
                    )
                }
            }

            if (gradable) {
                OutlinedTextField(
                    value = gradeText,
                    onValueChange = { gradeText = it },
                    label = { Text(stringResource(R.string.test_grade)) },
                    isError = !gradeOk,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.test_notes)) },
                minLines = 2,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (onAddStudyTask != null) {
                OutlinedButton(onClick = onAddStudyTask) {
                    Icon(painterResource(R.drawable.ic_check_circle), null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.test_add_study_task))
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
                Button(
                    onClick = {
                        if (chosenDate != null) {
                            onSave(title, kind, subjectId, chosenDate, time, notes, if (gradable) parseGrade(gradeText) else null)
                        }
                    },
                    enabled = canSave,
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }

    if (pickingDate) {
        PickDateDialog(initial = date, onPick = { date = it }, onClear = null, onDismiss = { pickingDate = false })
    }
    if (pickingTime) {
        PickTimeDialog(
            initial = time,
            onPick = { time = it },
            onClear = if (time != null) ({ time = null }) else null,
            onDismiss = { pickingTime = false },
        )
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
