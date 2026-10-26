package com.david.notebook.ui.courses

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.notebook.R
import com.david.notebook.data.courses.SubjectItem
import com.david.notebook.data.courses.Tag
import com.david.notebook.data.courses.TagOwner
import com.david.notebook.data.courses.tagColor
import com.david.notebook.ui.components.ConfirmDeleteDialog
import com.david.notebook.ui.components.EmptyState
import com.david.notebook.ui.components.MenuAction
import com.david.notebook.ui.components.NameDialog
import com.david.notebook.ui.components.NotebookTopBar
import com.david.notebook.ui.components.OverflowMenu
import com.david.notebook.ui.theme.dot

private sealed interface SubjectDialog {
    data object Create : SubjectDialog
    data class Rename(val subject: SubjectItem) : SubjectDialog
    data class Delete(val subject: SubjectItem) : SubjectDialog
    data class Tags(val subject: SubjectItem) : SubjectDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    onOpenSubject: (Long) -> Unit,
    viewModel: CoursesViewModel = viewModel(factory = CoursesViewModel.Factory),
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SubjectDialog?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // The system photo picker: no permission needed, shows only images.
    var logoFor by rememberSaveable { mutableStateOf<Long?>(null) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = logoFor
        if (uri != null && id != null) viewModel.setLogo(id, uri)
        logoFor = null
    }

    LaunchedEffect(message) {
        message?.let {
            viewModel.messageShown()
            snackbarHostState.showSnackbar(context.getString(it))
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NotebookTopBar(
                title = stringResource(R.string.tab_courses),
                large = true,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.subject_new)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                expanded = fabExpanded,
                onClick = { dialog = SubjectDialog.Create },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val list = subjects
        when {
            list == null -> Unit
            list.isEmpty() && filter == null -> EmptyState(
                icon = R.drawable.ic_school,
                title = stringResource(R.string.empty_courses_title),
                message = stringResource(R.string.empty_courses),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp, // room for the FAB
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tags.isNotEmpty()) {
                    item(key = "filter") {
                        TagFilterRow(tags = tags, selected = filter, onSelect = viewModel::setFilter)
                    }
                }
                if (list.isEmpty()) {
                    item(key = "no-match") {
                        Text(
                            stringResource(R.string.filter_no_match),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 32.dp),
                        )
                    }
                }
                items(list, key = { it.summary.id }) { subject ->
                    val id = subject.summary.id
                    SubjectCard(
                        subject = subject,
                        onClick = { onOpenSubject(id) },
                        actions = listOf(
                            pinAction(subject.summary.pinned) { viewModel.setPinned(id, !subject.summary.pinned) },
                            MenuAction(stringResource(R.string.tags_edit), ImageVector.vectorResource(R.drawable.ic_label)) {
                                dialog = SubjectDialog.Tags(subject)
                            },
                            MenuAction(
                                stringResource(if (subject.logo == null) R.string.logo_set else R.string.logo_change),
                                ImageVector.vectorResource(R.drawable.ic_image),
                            ) {
                                logoFor = id
                                logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        ) + listOfNotNull(
                            subject.logo?.let {
                                MenuAction(stringResource(R.string.logo_remove), Icons.Filled.Delete) { viewModel.removeLogo(id) }
                            },
                        ) + listOf(
                            MenuAction(stringResource(R.string.rename), Icons.Filled.Edit) { dialog = SubjectDialog.Rename(subject) },
                            MenuAction(stringResource(R.string.delete), Icons.Filled.Delete) { dialog = SubjectDialog.Delete(subject) },
                        ),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        SubjectDialog.Create -> NameDialog(
            title = stringResource(R.string.subject_new),
            label = stringResource(R.string.subject_name),
            confirmLabel = stringResource(R.string.create),
            onConfirm = { viewModel.createSubject(it); dialog = null },
            onDismiss = { dialog = null },
        )
        is SubjectDialog.Rename -> NameDialog(
            title = stringResource(R.string.subject_rename),
            label = stringResource(R.string.subject_name),
            confirmLabel = stringResource(R.string.save),
            initial = d.subject.summary.name,
            onConfirm = { viewModel.renameSubject(d.subject.summary.id, it); dialog = null },
            onDismiss = { dialog = null },
        )
        is SubjectDialog.Delete -> {
            val files = d.subject.summary.fileCount
            ConfirmDeleteDialog(
                title = stringResource(R.string.delete_named, d.subject.summary.name),
                text = pluralStringResource(R.plurals.subject_delete_text, files, files),
                onConfirm = { viewModel.deleteSubject(d.subject.summary.id); dialog = null },
                onDismiss = { dialog = null },
            )
        }
        is SubjectDialog.Tags -> TagsSheet(
            owner = TagOwner.OfSubject(d.subject.summary.id),
            title = stringResource(R.string.tags_for, d.subject.summary.name),
            onDismiss = { dialog = null },
        )
    }
}

/** "Pin" or "Unpin", for the ⋮ menus. */
@Composable
fun pinAction(pinned: Boolean, onClick: () -> Unit) = MenuAction(
    stringResource(if (pinned) R.string.unpin else R.string.pin),
    ImageVector.vectorResource(R.drawable.ic_pin),
    onClick,
)

/** Small pin shown next to the name of pinned items. */
@Composable
fun PinnedMark(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_pin),
        contentDescription = stringResource(R.string.pinned),
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(16.dp),
    )
}

@Composable
private fun TagFilterRow(tags: List<Tag>, selected: Long?, onSelect: (Long) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tags, key = { it.id }) { tag ->
            FilterChip(
                selected = tag.id == selected,
                onClick = { onSelect(tag.id) },
                label = { Text(tag.name) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(tag.tagColor().dot, CircleShape),
                    )
                },
            )
        }
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectItem,
    onClick: () -> Unit,
    actions: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val summary = subject.summary
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
        ) {
            SubjectAvatar(summary.name, subject.logo)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        summary.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (summary.pinned) {
                        Spacer(Modifier.width(6.dp))
                        PinnedMark()
                    }
                }
                Text(
                    pluralStringResource(R.plurals.folder_count, summary.folderCount, summary.folderCount) +
                        " · " +
                        pluralStringResource(R.plurals.pdf_count, summary.fileCount, summary.fileCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (subject.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    TagRow(subject.tags)
                }
            }
            OverflowMenu(actions)
        }
    }
}
