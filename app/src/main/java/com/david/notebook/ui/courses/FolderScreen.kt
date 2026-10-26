package com.david.notebook.ui.courses

import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.notebook.R
import com.david.notebook.data.courses.CourseFile
import com.david.notebook.ui.components.ConfirmDeleteDialog
import com.david.notebook.ui.components.EmptyState
import com.david.notebook.ui.components.MenuAction
import com.david.notebook.ui.components.NameDialog
import com.david.notebook.ui.components.NotebookTopBar
import com.david.notebook.ui.components.OverflowMenu
import kotlinx.coroutines.launch

private sealed interface FileDialog {
    data class Rename(val file: CourseFile) : FileDialog
    data class Delete(val file: CourseFile) : FileDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    onBack: () -> Unit,
    viewModel: FolderViewModel = viewModel(factory = FolderViewModel.Factory),
) {
    val header by viewModel.header.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<FileDialog?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    // Storage Access Framework: no storage permission needed, and several PDFs at once.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importPdfs(uris)
    }

    LaunchedEffect(message) {
        val text = when (val m = message) {
            null -> return@LaunchedEffect
            is FolderMessage.Imported ->
                context.resources.getQuantityString(R.plurals.import_done, m.count, m.count)
            is FolderMessage.ImportFailed ->
                context.resources.getQuantityString(R.plurals.import_failed, m.count, m.count)
            FolderMessage.RenameFailed -> context.getString(R.string.rename_failed)
        }
        viewModel.messageShown()
        snackbarHostState.showSnackbar(text)
    }

    fun open(file: CourseFile) {
        // Hand the PDF to whatever viewer the phone has, via our FileProvider; the
        // read grant lasts only as long as that viewer's task.
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(viewModel.shareableUri(file), "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.no_pdf_viewer)) }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NotebookTopBar(
                title = header?.name.orEmpty(),
                subtitle = header?.subjectName,
                onBack = onBack,
                large = true,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.import_pdf)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                expanded = fabExpanded,
                onClick = { if (!importing) picker.launch(arrayOf("application/pdf")) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            val list = files
            when {
                list == null -> Unit
                list.isEmpty() && !importing -> EmptyState(
                    icon = R.drawable.ic_pdf,
                    title = stringResource(R.string.empty_files_title),
                    message = stringResource(R.string.empty_files),
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
                    items(list, key = { it.id }) { file ->
                        FileRow(
                            file = file,
                            onClick = { open(file) },
                            actions = listOf(
                                pinAction(file.pinned) { viewModel.setPinned(file, !file.pinned) },
                                MenuAction(stringResource(R.string.rename), Icons.Filled.Edit) {
                                    dialog = FileDialog.Rename(file)
                                },
                                MenuAction(stringResource(R.string.delete), Icons.Filled.Delete) {
                                    dialog = FileDialog.Delete(file)
                                },
                            ),
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            if (importing) {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = padding.calculateTopPadding()),
                )
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        is FileDialog.Rename -> NameDialog(
            title = stringResource(R.string.file_rename),
            label = stringResource(R.string.file_name),
            confirmLabel = stringResource(R.string.save),
            initial = d.file.name.removeSuffix(".pdf"),
            onConfirm = { viewModel.renameFile(d.file, it); dialog = null },
            onDismiss = { dialog = null },
        )
        is FileDialog.Delete -> ConfirmDeleteDialog(
            title = stringResource(R.string.delete_named, d.file.name),
            text = stringResource(R.string.file_delete_text),
            onConfirm = { viewModel.deleteFile(d.file); dialog = null },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun FileRow(
    file: CourseFile,
    onClick: () -> Unit,
    actions: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_pdf), contentDescription = null)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        file.name.removeSuffix(".pdf"),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (file.pinned) {
                        Spacer(Modifier.width(6.dp))
                        PinnedMark()
                    }
                }
                Text(
                    Formatter.formatShortFileSize(context, file.sizeBytes) + " · " +
                        DateUtils.formatDateTime(context, file.addedAt, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OverflowMenu(actions)
        }
    }
}
