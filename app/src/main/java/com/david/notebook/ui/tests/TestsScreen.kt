package com.david.notebook.ui.tests

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.notebook.R
import com.david.notebook.data.tests.Assessment
import com.david.notebook.data.tests.AssessmentKind
import com.david.notebook.data.tests.AssessmentWithSubject
import com.david.notebook.ui.components.EmptyState
import com.david.notebook.ui.components.MenuAction
import com.david.notebook.ui.components.NotebookTopBar
import com.david.notebook.ui.components.OverflowMenu
import com.david.notebook.ui.components.SubjectFilter
import com.david.notebook.ui.components.SwipeToDelete
import com.david.notebook.ui.components.rememberAskForReminders
import com.david.notebook.ui.components.subjectsIn
import com.david.notebook.ui.components.formatTime
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private sealed interface Editing {
    data object New : Editing
    data class Existing(val assessment: Assessment) : Editing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestsScreen(viewModel: TestsViewModel = viewModel(factory = TestsViewModel.Factory)) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Editing?>(null) }
    var showPast by rememberSaveable { mutableStateOf(false) }
    var subjectFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    val askForReminders = rememberAskForReminders()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    val today = LocalDate.now().toEpochDay()
    val filterOptions = subjectsIn(items.orEmpty(), { it.assessment.subjectId }, { it.subjectName })
    // A filter on a subject that's no longer in the list (its last test was deleted) shows all.
    val filter = subjectFilter?.takeIf { id -> filterOptions.any { it.id == id } }
    val visible = items.orEmpty().filter { filter == null || it.assessment.subjectId == filter }
    val upcoming = visible.filter { it.assessment.date >= today }
    // Most recent first: last week's exam matters more than one from last semester.
    val past = visible.filter { it.assessment.date < today }.reversed()

    fun deleteWithUndo(assessment: Assessment) {
        viewModel.delete(assessment)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.test_deleted),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(assessment)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NotebookTopBar(
                title = stringResource(R.string.tab_tests),
                large = true,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (past.isNotEmpty()) {
                        OverflowMenu(
                            listOf(MenuAction(stringResource(R.string.test_clear_past), Icons.Filled.Delete) { viewModel.clearPast() }),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.test_new)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                expanded = fabExpanded,
                onClick = { editing = Editing.New },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            items == null -> Unit
            items.orEmpty().isEmpty() -> EmptyState(
                icon = R.drawable.ic_assignment,
                title = stringResource(R.string.empty_tests_title),
                message = stringResource(R.string.empty_tests),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filterOptions.size >= 2) {
                    item(key = "filter") {
                        SubjectFilter(filterOptions, selected = filter, onSelect = { subjectFilter = it })
                    }
                }
                if (upcoming.isEmpty()) {
                    item(key = "nothing-upcoming") {
                        Text(
                            stringResource(R.string.test_nothing_upcoming),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
                upcoming.firstOrNull()?.let { next ->
                    item(key = next.assessment.id) {
                        SwipeToDelete(
                            onDelete = { deleteWithUndo(next.assessment) },
                            modifier = Modifier.animateItem(),
                            shape = RoundedCornerShape(28.dp),
                        ) {
                            NextUpCard(next, today, onClick = { editing = Editing.Existing(next.assessment) })
                        }
                    }
                }
                items(upcoming.drop(1), key = { it.assessment.id }) { item ->
                    SwipeToDelete(onDelete = { deleteWithUndo(item.assessment) }, modifier = Modifier.animateItem()) {
                        AssessmentCard(item, today, onClick = { editing = Editing.Existing(item.assessment) })
                    }
                }
                if (past.isNotEmpty()) {
                    item(key = "past-header") {
                        SectionHeader(
                            text = stringResource(R.string.test_past, past.size),
                            expanded = showPast,
                            onToggle = { showPast = !showPast },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (showPast) {
                        if (past.any { it.assessment.grade != null }) {
                            item(key = "grades") {
                                GradesCard(past, Modifier.animateItem())
                            }
                        }
                        items(past, key = { it.assessment.id }) { item ->
                            SwipeToDelete(onDelete = { deleteWithUndo(item.assessment) }, modifier = Modifier.animateItem()) {
                                AssessmentCard(item, today, onClick = { editing = Editing.Existing(item.assessment) })
                            }
                        }
                    }
                }
            }
        }
    }

    when (val e = editing) {
        null -> Unit
        Editing.New -> AssessmentSheet(
            existing = null,
            subjects = subjects,
            onSave = { title, kind, subjectId, date, time, notes, grade ->
                viewModel.save(null, title, kind, subjectId, date, time, notes, grade)
                editing = null
                askForReminders()
            },
            onDelete = null,
            onAddStudyTask = null,
            onDismiss = { editing = null },
        )
        is Editing.Existing -> AssessmentSheet(
            existing = e.assessment,
            subjects = subjects,
            onSave = { title, kind, subjectId, date, time, notes, grade ->
                viewModel.save(e.assessment, title, kind, subjectId, date, time, notes, grade)
                editing = null
            },
            onDelete = { editing = null; deleteWithUndo(e.assessment) },
            onAddStudyTask = if (e.assessment.date >= today) {
                {
                    val subjectName = items.orEmpty().find { it.assessment.id == e.assessment.id }?.subjectName
                    val name = studyName(context, e.assessment, subjectName)
                    viewModel.addStudyTask(e.assessment, context.getString(R.string.study_for, name))
                    editing = null
                    askForReminders()
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(context.getString(R.string.study_task_added))
                    }
                }
            } else {
                null
            },
            onDismiss = { editing = null },
        )
    }
}

/** The closest upcoming item, large, with the countdown as the headline. */
@Composable
private fun NextUpCard(item: AssessmentWithSubject, today: Long, onClick: () -> Unit) {
    val a = item.assessment
    val days = (a.date - today).toInt()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.test_next_up),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(countdown(days), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    displayTitle(item),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                KindChip(a.assessmentKind())
            }
            Text(
                whenLine(a) + (subtitleSubject(item)?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (a.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(a.notes, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AssessmentCard(item: AssessmentWithSubject, today: Long, onClick: () -> Unit) {
    val a = item.assessment
    val days = (a.date - today).toInt()
    val isPast = days < 0
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            DateTile(a.date, dimmed = isPast)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        displayTitle(item),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    KindChip(a.assessmentKind())
                }
                val details = listOfNotNull(a.time?.let { formatTime(it) }, subtitleSubject(item))
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (a.notes.isNotBlank()) {
                    Text(
                        a.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(countdown(days), style = MaterialTheme.typography.labelLarge, color = countdownColor(days))
            }
            a.grade?.let { grade ->
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Text(
                        formatGrade(grade),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** Day number over short month, like a page torn from a desk calendar. */
@Composable
private fun DateTile(epochDay: Long, dimmed: Boolean) {
    val date = LocalDate.ofEpochDay(epochDay)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (dimmed) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(56.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge)
            Text(
                date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Average grade per subject, and overall, over the graded past entries. */
@Composable
private fun GradesCard(past: List<AssessmentWithSubject>, modifier: Modifier = Modifier) {
    val graded = past.filter { it.assessment.grade != null }
    val perSubject = graded.filter { it.subjectName != null }
        .groupBy { it.subjectName!! }
        .mapValues { (_, list) -> list.mapNotNull { it.assessment.grade }.average() }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    val overall = graded.mapNotNull { it.assessment.grade }.average()
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.grades_average), style = MaterialTheme.typography.titleSmall)
            perSubject.forEach { (name, average) -> AverageRow(name, average, bold = false) }
            // One subject: its row already is the overall average.
            if (perSubject.size != 1 || graded.any { it.subjectName == null }) {
                AverageRow(stringResource(R.string.grades_overall), overall, bold = true)
            }
        }
    }
}

@Composable
private fun AverageRow(label: String, average: Double, bold: Boolean) {
    val style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(formatGrade(average), style = style)
    }
}

/** What a study task calls a test: its name, or "Algebra · Exam" when it has none. */
private fun studyName(context: Context, a: Assessment, subjectName: String?): String {
    val kind = context.getString(a.assessmentKind().label())
    return a.title.ifBlank { subjectName?.let { "$it · $kind" } ?: kind }
}

/** The subject, unless it's already being used as the title. */
private fun subtitleSubject(item: AssessmentWithSubject): String? =
    item.subjectName?.takeIf { item.assessment.title.isNotBlank() }

/** "Friday, 31 Oct · 10:00". */
@Composable
private fun whenLine(a: Assessment): String {
    val date = LocalDate.ofEpochDay(a.date)
    val day = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    return a.time?.let { "$day · ${formatTime(it)}" } ?: day
}

/** Red within 3 days, accent within a week, quiet after that (and for past ones). */
@Composable
private fun countdownColor(days: Int): Color = when {
    days < 0 -> MaterialTheme.colorScheme.onSurfaceVariant
    days <= 3 -> MaterialTheme.colorScheme.error
    days <= 7 -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
