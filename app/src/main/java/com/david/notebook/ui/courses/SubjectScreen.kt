package com.david.notebook.ui.courses

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.david.notebook.data.courses.FolderItem
import com.david.notebook.data.courses.SubjectItem
import com.david.notebook.data.courses.TagOwner
import com.david.notebook.ui.components.ConfirmDeleteDialog
import com.david.notebook.ui.components.EmptyState
import com.david.notebook.ui.components.MenuAction
import com.david.notebook.ui.components.NameDialog
import com.david.notebook.ui.components.NotebookTopBar
import com.david.notebook.ui.components.OverflowMenu

private sealed interface SubjectScreenDialog {
    data object RenameSubject : SubjectScreenDialog
    data object DeleteSubject : SubjectScreenDialog
    data object SubjectTags : SubjectScreenDialog
    data object NewFolder : SubjectScreenDialog
    data class RenameFolder(val folder: FolderItem) : SubjectScreenDialog
    data class DeleteFolder(val folder: FolderItem) : SubjectScreenDialog
    data class FolderTags(val folder: FolderItem) : SubjectScreenDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectScreen(
    onOpenFolder: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SubjectViewModel = viewModel(factory = SubjectViewModel.Factory),
) {
    val subject by viewModel.subject.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SubjectScreenDialog?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = rememberLazyGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setLogo(uri)
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
                title = subject?.summary?.name.orEmpty(),
                onBack = onBack,
                large = true,
                scrollBehavior = scrollBehavior,
                actions = {
                    OverflowMenu(
                        listOf(
                            MenuAction(stringResource(R.string.tags_edit), ImageVector.vectorResource(R.drawable.ic_label)) {
                                dialog = SubjectScreenDialog.SubjectTags
                            },
                            MenuAction(
                                stringResource(if (subject?.logo == null) R.string.logo_set else R.string.logo_change),
                                ImageVector.vectorResource(R.drawable.ic_image),
                            ) {
                                logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        ) + listOfNotNull(
                            subject?.logo?.let {
                                MenuAction(stringResource(R.string.logo_remove), Icons.Filled.Delete) { viewModel.removeLogo() }
                            },
                        ) + listOf(
                            MenuAction(stringResource(R.string.subject_rename), Icons.Filled.Edit) {
                                dialog = SubjectScreenDialog.RenameSubject
                            },
                            MenuAction(stringResource(R.string.subject_delete), Icons.Filled.Delete) {
                                dialog = SubjectScreenDialog.DeleteSubject
                            },
                        ),
                    )
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.folder_new)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                expanded = fabExpanded,
                onClick = { dialog = SubjectScreenDialog.NewFolder },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val list = folders
        val current = subject
        when {
            list == null || current == null -> Unit
            list.isEmpty() -> EmptyState(
                icon = R.drawable.ic_folder,
                title = stringResource(R.string.empty_folders_title),
                message = stringResource(R.string.empty_folders),
                modifier = Modifier.padding(padding),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    SubjectHeader(current)
                }
                items(list, key = { it.summary.id }) { folder ->
                    val id = folder.summary.id
                    FolderTile(
                        folder = folder,
                        onClick = { onOpenFolder(id) },
                        actions = listOf(
                            pinAction(folder.summary.pinned) { viewModel.setFolderPinned(id, !folder.summary.pinned) },
                            MenuAction(stringResource(R.string.tags_edit), ImageVector.vectorResource(R.drawable.ic_label)) {
                                dialog = SubjectScreenDialog.FolderTags(folder)
                            },
                            MenuAction(stringResource(R.string.rename), Icons.Filled.Edit) {
                                dialog = SubjectScreenDialog.RenameFolder(folder)
                            },
                            MenuAction(stringResource(R.string.delete), Icons.Filled.Delete) {
                                dialog = SubjectScreenDialog.DeleteFolder(folder)
                            },
                        ),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    val subjectName = subject?.summary?.name.orEmpty()
    when (val d = dialog) {
        null -> Unit
        SubjectScreenDialog.RenameSubject -> NameDialog(
            title = stringResource(R.string.subject_rename),
            label = stringResource(R.string.subject_name),
            confirmLabel = stringResource(R.string.save),
            initial = subjectName,
            onConfirm = { viewModel.renameSubject(it); dialog = null },
            onDismiss = { dialog = null },
        )
        SubjectScreenDialog.DeleteSubject -> {
            val files = subject?.summary?.fileCount ?: 0
            ConfirmDeleteDialog(
                title = stringResource(R.string.delete_named, subjectName),
                text = pluralStringResource(R.plurals.subject_delete_text, files, files),
                onConfirm = {
                    dialog = null
                    viewModel.deleteSubject()
                    onBack()
                },
                onDismiss = { dialog = null },
            )
        }
        SubjectScreenDialog.SubjectTags -> TagsSheet(
            owner = TagOwner.OfSubject(viewModel.subjectId),
            title = stringResource(R.string.tags_for, subjectName),
            onDismiss = { dialog = null },
        )
        SubjectScreenDialog.NewFolder -> NameDialog(
            title = stringResource(R.string.folder_new),
            label = stringResource(R.string.folder_name),
            confirmLabel = stringResource(R.string.create),
            onConfirm = { viewModel.addFolder(it); dialog = null },
            onDismiss = { dialog = null },
        )
        is SubjectScreenDialog.RenameFolder -> NameDialog(
            title = stringResource(R.string.folder_rename),
            label = stringResource(R.string.folder_name),
            confirmLabel = stringResource(R.string.save),
            initial = d.folder.summary.name,
            onConfirm = { viewModel.renameFolder(d.folder.summary.id, it); dialog = null },
            onDismiss = { dialog = null },
        )
        is SubjectScreenDialog.DeleteFolder -> {
            val files = d.folder.summary.fileCount
            ConfirmDeleteDialog(
                title = stringResource(R.string.delete_named, d.folder.summary.name),
                text = pluralStringResource(R.plurals.folder_delete_text, files, files),
                onConfirm = { viewModel.deleteFolder(d.folder.summary.id); dialog = null },
                onDismiss = { dialog = null },
            )
        }
        is SubjectScreenDialog.FolderTags -> TagsSheet(
            owner = TagOwner.OfFolder(d.folder.summary.id),
            title = stringResource(R.string.tags_for, d.folder.summary.name),
            onDismiss = { dialog = null },
        )
    }
}

/** Logo, counts and tags above the folder grid. */
@Composable
private fun SubjectHeader(subject: SubjectItem) {
    val summary = subject.summary
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        SubjectAvatar(summary.name, subject.logo, size = 64.dp, textStyle = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                pluralStringResource(R.plurals.folder_count, summary.folderCount, summary.folderCount) +
                    " · " +
                    pluralStringResource(R.plurals.pdf_count, summary.fileCount, summary.fileCount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (subject.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TagRow(subject.tags)
            }
        }
    }
}

@Composable
private fun FolderTile(
    folder: FolderItem,
    onClick: () -> Unit,
    actions: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val summary = folder.summary
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_folder), contentDescription = null)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (summary.pinned) PinnedMark(Modifier.padding(top = 18.dp))
                Box(Modifier.padding(top = 4.dp, end = 4.dp)) {
                    OverflowMenu(actions)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                summary.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 16.dp),
            )
            Text(
                pluralStringResource(R.plurals.pdf_count, summary.fileCount, summary.fileCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (folder.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TagRow(folder.tags, Modifier.padding(end = 16.dp))
            }
        }
    }
}
