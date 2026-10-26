package com.david.notebook.ui.schedule

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.notebook.R
import com.david.notebook.ui.components.EmptyState
import com.david.notebook.ui.components.NotebookTopBar

/** MIME types the picker offers: the timetable as a PDF or as a photo/screenshot. */
private val ScheduleTypes = arrayOf("application/pdf", "image/*")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onOpenSettings: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }

    // Storage Access Framework picker: no storage permission needed, the user grants this one file.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            NotebookTopBar(
                title = stringResource(R.string.tab_schedule),
                actions = {
                    if (state.file != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.schedule_replace)) },
                                    onClick = {
                                        menuOpen = false
                                        picker.launch(ScheduleTypes)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.schedule_remove)) },
                                    onClick = {
                                        menuOpen = false
                                        confirmRemove = true
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.loading && !state.importing) {
                val hasSchedule = state.file != null
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(if (hasSchedule) R.string.schedule_change else R.string.schedule_import)) },
                    icon = { Icon(if (hasSchedule) Icons.Filled.Edit else Icons.Filled.Add, contentDescription = null) },
                    onClick = { picker.launch(ScheduleTypes) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        val file = state.file
        when {
            state.loading -> Unit
            state.importing -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            file == null -> EmptyState(
                icon = R.drawable.ic_event,
                title = stringResource(R.string.empty_schedule_title),
                message = stringResource(R.string.empty_schedule),
                modifier = modifier,
            )
            else -> ScheduleViewer(file, modifier)
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.schedule_remove_title)) },
            text = { Text(stringResource(R.string.schedule_remove_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.remove()
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
