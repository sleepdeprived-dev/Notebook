package com.david.notebook.ui.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.notebook.R
import com.david.notebook.data.tests.AssessmentKind
import com.david.notebook.data.tests.AssessmentWithSubject
import com.david.notebook.data.todo.Todo
import com.david.notebook.data.todo.TodoWithSubject
import com.david.notebook.ui.components.EmptyState
import com.david.notebook.ui.components.MenuAction
import com.david.notebook.ui.components.NotebookTopBar
import com.david.notebook.ui.components.OverflowMenu
import com.david.notebook.ui.components.SubjectFilter
import com.david.notebook.ui.components.SwipeToDelete
import com.david.notebook.ui.components.formatTime
import com.david.notebook.ui.components.rememberAskForReminders
import com.david.notebook.ui.components.subjectsIn
import com.david.notebook.ui.tests.KindChip
import com.david.notebook.ui.tests.countdown
import com.david.notebook.ui.tests.displayTitle
import com.david.notebook.ui.tests.label
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Which task the editor sheet is open for: a new one, or an existing one. */
private sealed interface Editing {
    data object New : Editing
    data class Existing(val todo: Todo) : Editing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onOpenTests: () -> Unit,
    viewModel: TodoViewModel = viewModel(factory = TodoViewModel.Factory),
) {
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val comingUp by viewModel.comingUp.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Editing?>(null) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    var subjectFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    val askForReminders = rememberAskForReminders()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    val filterOptions = subjectsIn(
        todos.orEmpty().map { it.todo.subjectId to it.subjectName } +
            comingUp.map { it.assessment.subjectId to it.subjectName },
        { it.first },
        { it.second },
    )
    // A filter on a subject that's no longer in the list shows all.
    val filter = subjectFilter?.takeIf { id -> filterOptions.any { it.id == id } }
    val visible = todos.orEmpty().filter { filter == null || it.todo.subjectId == filter }
    val tests = comingUp.filter { filter == null || it.assessment.subjectId == filter }
    val open = visible.filterNot { it.todo.done }
    val completed = visible.filter { it.todo.done }
    val today = LocalDate.now().toEpochDay()

    fun deleteWithUndo(todo: Todo) {
        viewModel.delete(todo)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.todo_deleted),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(todo)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NotebookTopBar(
                title = stringResource(R.string.tab_todo),
                large = true,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (completed.isNotEmpty()) {
                        OverflowMenu(
                            listOf(
                                MenuAction(stringResource(R.string.todo_clear_completed), Icons.Filled.Delete) {
                                    viewModel.clearCompleted()
                                },
                            ),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.todo_new)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                expanded = fabExpanded,
                onClick = { editing = Editing.New },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            todos == null -> Unit
            todos.orEmpty().isEmpty() && comingUp.isEmpty() -> EmptyState(
                icon = R.drawable.ic_check_circle,
                title = stringResource(R.string.empty_todo_title),
                message = stringResource(R.string.empty_todo),
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
                if (tests.isNotEmpty()) {
                    item(key = "coming-up") {
                        Text(
                            stringResource(R.string.todo_coming_up),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .animateItem()
                                .padding(start = 4.dp, top = 4.dp),
                        )
                    }
                    items(tests, key = { "test-${it.assessment.id}" }) { test ->
                        ComingUpCard(test, today, onClick = onOpenTests, modifier = Modifier.animateItem())
                    }
                    item(key = "tasks-header") {
                        Text(
                            stringResource(R.string.todo_tasks),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .animateItem()
                                .padding(start = 4.dp, top = 12.dp),
                        )
                    }
                }
                if (open.isEmpty()) {
                    item(key = "all-done") {
                        Text(
                            stringResource(R.string.todo_all_done),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
                items(open, key = { it.todo.id }) { item ->
                    SwipeToDelete(onDelete = { deleteWithUndo(item.todo) }, modifier = Modifier.animateItem()) {
                        TaskCard(
                            item = item,
                            onToggle = { viewModel.setDone(item.todo, it) },
                            onClick = { editing = Editing.Existing(item.todo) },
                        )
                    }
                }
                if (completed.isNotEmpty()) {
                    item(key = "completed-header") {
                        CompletedHeader(
                            count = completed.size,
                            expanded = showCompleted,
                            onToggle = { showCompleted = !showCompleted },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (showCompleted) {
                        items(completed, key = { it.todo.id }) { item ->
                            SwipeToDelete(onDelete = { deleteWithUndo(item.todo) }, modifier = Modifier.animateItem()) {
                                TaskCard(
                                    item = item,
                                    onToggle = { viewModel.setDone(item.todo, it) },
                                    onClick = { editing = Editing.Existing(item.todo) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (val e = editing) {
        null -> Unit
        Editing.New -> TaskSheet(
            existing = null,
            subjects = subjects,
            onSave = { title, subjectId, due, time ->
                viewModel.save(null, title, subjectId, due, time)
                editing = null
                if (due != null) askForReminders()
            },
            onDelete = null,
            onDismiss = { editing = null },
        )
        is Editing.Existing -> TaskSheet(
            existing = e.todo,
            subjects = subjects,
            onSave = { title, subjectId, due, time ->
                viewModel.save(e.todo, title, subjectId, due, time)
                editing = null
                if (due != null) askForReminders()
            },
            onDelete = { editing = null; deleteWithUndo(e.todo) },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun TaskCard(item: TodoWithSubject, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val todo = item.todo
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
        ) {
            Checkbox(checked = todo.done, onCheckedChange = onToggle)
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                // A study task's test already says which subject it's for.
                val test = item.testKind?.let { kind ->
                    item.testName ?: stringResource(AssessmentKind.entries.find { it.name == kind }?.label() ?: R.string.kind_exam)
                }
                if (todo.dueDate != null || item.subjectName != null || test != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        todo.dueDate?.let { DueLabel(it, todo.time, done = todo.done) }
                        when {
                            test != null -> SubjectLabel(test, R.drawable.ic_assignment, Modifier.weight(1f, fill = false))
                            item.subjectName != null -> SubjectLabel(item.subjectName, R.drawable.ic_school, Modifier.weight(1f, fill = false))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DueLabel(epochDay: Long, time: Int?, done: Boolean) {
    val due = dueText(epochDay, LocalDate.now())
    val color = when {
        done -> MaterialTheme.colorScheme.onSurfaceVariant
        due.overdue -> MaterialTheme.colorScheme.error
        due.today -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_event), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            time?.let { "${due.text} · ${formatTime(it)}" } ?: due.text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

@Composable
private fun SubjectLabel(name: String, icon: Int, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A test in the next week: kind, name, countdown. Tapping opens the Tests tab. */
@Composable
private fun ComingUpCard(item: AssessmentWithSubject, today: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val days = (item.assessment.date - today).toInt()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            KindChip(item.assessment.assessmentKind())
            Spacer(Modifier.width(12.dp))
            Text(
                displayTitle(item),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                countdown(days),
                style = MaterialTheme.typography.labelLarge,
                color = if (days <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CompletedHeader(count: Int, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            stringResource(R.string.todo_completed, count),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
