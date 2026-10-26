package com.david.notebook.ui.todo

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.david.notebook.R
import com.david.notebook.ui.components.EmptyState
import com.david.notebook.ui.components.NotebookTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen() {
    Scaffold(topBar = { NotebookTopBar(stringResource(R.string.tab_todo), large = true) }) { padding ->
        EmptyState(
            icon = R.drawable.ic_check_circle,
            title = stringResource(R.string.empty_todo_title),
            message = stringResource(R.string.empty_todo),
            modifier = Modifier.padding(padding),
        )
    }
}
