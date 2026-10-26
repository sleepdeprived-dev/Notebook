package com.david.notebook.ui.tests

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
fun TestsScreen() {
    Scaffold(topBar = { NotebookTopBar(stringResource(R.string.tab_tests), large = true) }) { padding ->
        EmptyState(
            icon = R.drawable.ic_assignment,
            title = stringResource(R.string.empty_tests_title),
            message = stringResource(R.string.empty_tests),
            modifier = Modifier.padding(padding),
        )
    }
}
