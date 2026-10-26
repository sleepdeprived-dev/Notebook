package com.david.notebook.ui.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.notebook.R
import com.david.notebook.data.courses.Tag
import com.david.notebook.data.courses.TagColor
import com.david.notebook.data.courses.TagOwner
import com.david.notebook.data.courses.tagColor
import com.david.notebook.ui.theme.chipColors
import com.david.notebook.ui.theme.dot

@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier) {
    val (container, content) = tag.tagColor().chipColors()
    Surface(shape = RoundedCornerShape(8.dp), color = container, contentColor = content, modifier = modifier) {
        Text(
            tag.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagRow(tags: List<Tag>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        tags.forEach { TagChip(it) }
    }
}

private sealed interface TagEdit {
    data object New : TagEdit
    data class Existing(val tag: Tag) : TagEdit
}

/**
 * Bottom sheet for choosing which tags [owner] has. The tag list is shared by all
 * subjects and folders, so tags can also be created, recoloured and deleted here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsSheet(owner: TagOwner, title: String, onDismiss: () -> Unit) {
    val viewModel: TagsViewModel = viewModel(key = "tags-$owner", factory = TagsViewModel.factory(owner))
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TagEdit?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(if (tags.isEmpty()) R.string.tags_none_yet else R.string.tags_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            tags.forEach { tag ->
                val checked = tag.id in selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setTagged(tag, !checked) },
                ) {
                    Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                    // The chip takes what it needs (ellipsizing long names); the rest is
                    // empty space, so the edit button always sits at the right edge.
                    Box(Modifier.weight(1f)) { TagChip(tag) }
                    IconButton(onClick = { editing = TagEdit.Existing(tag) }) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.tag_edit))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = { editing = TagEdit.New }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tag_new))
            }
        }
    }

    when (val e = editing) {
        null -> Unit
        TagEdit.New -> TagEditDialog(
            title = stringResource(R.string.tag_new),
            initialName = "",
            initialColor = TagColor.Blue,
            onSave = { name, color -> viewModel.create(name, color); editing = null },
            onDelete = null,
            onDismiss = { editing = null },
        )
        is TagEdit.Existing -> TagEditDialog(
            title = stringResource(R.string.tag_edit),
            initialName = e.tag.name,
            initialColor = e.tag.tagColor(),
            onSave = { name, color -> viewModel.update(e.tag, name, color); editing = null },
            onDelete = { viewModel.delete(e.tag); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditDialog(
    title: String,
    initialName: String,
    initialColor: TagColor,
    onSave: (String, TagColor) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var color by rememberSaveable { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TagColor.entries.forEach { option ->
                        ColorDot(option.dot, selected = option == color, onClick = { color = option })
                    }
                }
                if (name.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    TagChip(Tag(name = name.trim(), color = color.name))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, color) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color),
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
