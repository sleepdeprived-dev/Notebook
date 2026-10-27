package com.david.notebook.ui.settings

import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import java.io.File
import com.david.notebook.data.update.Release
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.notebook.R
import com.david.notebook.data.settings.Accent
import com.david.notebook.data.settings.ThemeMode
import com.david.notebook.ui.components.NotebookTopBar
import com.david.notebook.ui.theme.swatchFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val update by viewModel.update.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NotebookTopBar(
                title = stringResource(R.string.settings),
                onBack = onBack,
                large = true,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SettingsCard(
                title = stringResource(R.string.settings_theme),
                caption = stringResource(R.string.settings_theme_caption),
            ) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            label = {
                                Text(
                                    stringResource(
                                        when (mode) {
                                            ThemeMode.System -> R.string.theme_system
                                            ThemeMode.Light -> R.string.theme_light
                                            ThemeMode.Dark -> R.string.theme_dark
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            SettingsCard(
                title = stringResource(R.string.settings_accent),
                caption = if (Accent.dynamicSupported) stringResource(R.string.settings_accent_caption) else null,
            ) {
                AccentPicker(selected = settings.accent, onSelect = viewModel::setAccent)
            }

            SettingsCard(
                title = stringResource(R.string.updates),
                caption = stringResource(R.string.updates_current, viewModel.currentVersion),
            ) {
                UpdateSection(
                    state = update,
                    onCheck = viewModel::checkForUpdate,
                    onDownload = viewModel::download,
                    onInstall = { apk -> context.startActivity(viewModel.installIntent(apk)) },
                    onOpenPage = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    caption: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(selected: Accent, onSelect: (Accent) -> Unit) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        Accent.available.forEach { accent ->
            Swatch(
                color = swatchFor(accent, dark, context),
                label = stringResource(accentLabel(accent)),
                selected = accent == selected,
                onClick = { onSelect(accent) },
            )
        }
    }
}

@Composable
private fun Swatch(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
    ) {
        // Selected: a ring in the text colour around the swatch, plus a check mark.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .then(
                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    else Modifier,
                )
                .padding(if (selected) 6.dp else 4.dp)
                .clip(CircleShape)
                .background(color),
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun accentLabel(accent: Accent) = when (accent) {
    Accent.Dynamic -> R.string.accent_dynamic
    Accent.Red -> R.string.accent_red
    Accent.Orange -> R.string.accent_orange
    Accent.Yellow -> R.string.accent_yellow
    Accent.Green -> R.string.accent_green
    Accent.Blue -> R.string.accent_blue
    Accent.Purple -> R.string.accent_purple
}

@Composable
private fun UpdateSection(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (Release) -> Unit,
    onInstall: (File) -> Unit,
    onOpenPage: (String) -> Unit,
) {
    val context = LocalContext.current
    // Open the installer as soon as the download finishes; the button stays for a retry.
    if (state is UpdateState.ReadyToInstall) {
        LaunchedEffect(state.apk) { onInstall(state.apk) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            UpdateState.Idle -> FilledTonalButton(onClick = onCheck) { Text(stringResource(R.string.update_check)) }
            UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.update_checking), style = MaterialTheme.typography.bodyLarge)
            }
            UpdateState.UpToDate -> {
                Text(stringResource(R.string.update_latest), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onCheck) { Text(stringResource(R.string.update_check_again)) }
            }
            UpdateState.CheckFailed -> {
                Text(
                    stringResource(R.string.update_check_failed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onCheck) { Text(stringResource(R.string.try_again)) }
            }
            is UpdateState.Available -> {
                val release = state.release
                Text(
                    stringResource(R.string.update_available, release.version),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val notes = plainNotes(release.notes)
                if (notes.isNotBlank()) {
                    Text(
                        notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(onClick = { onDownload(release) }) {
                    Text(
                        stringResource(
                            R.string.update_download,
                            Formatter.formatShortFileSize(context, release.apkSize),
                        ),
                    )
                }
                TextButton(onClick = { onOpenPage(release.pageUrl) }) { Text(stringResource(R.string.update_view_github)) }
            }
            is UpdateState.Downloading -> {
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.update_downloading, (state.progress * 100).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is UpdateState.ReadyToInstall -> {
                Button(onClick = { onInstall(state.apk) }) {
                    Text(stringResource(R.string.update_install, state.release.version))
                }
                Text(
                    stringResource(R.string.update_install_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is UpdateState.DownloadFailed -> {
                Text(
                    stringResource(R.string.update_download_failed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Row {
                    TextButton(onClick = { onDownload(state.release) }) { Text(stringResource(R.string.try_again)) }
                    TextButton(onClick = { onOpenPage(state.release.pageUrl) }) {
                        Text(stringResource(R.string.update_view_github))
                    }
                }
            }
        }
    }
}

/** Release notes are Markdown; show them as plain text (no #, **, backticks). */
private fun plainNotes(markdown: String): String =
    markdown.lineSequence()
        .map { it.trim().removePrefix("###").removePrefix("##").removePrefix("#").trim() }
        .map { it.replace("**", "").replace("`", "").replace(Regex("^[-*] "), "• ") }
        .map { it.replace(Regex("(?<![\\w*])[*_]([^*_]+)[*_](?![\\w*])"), "$1") } // *italics*
        .joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
