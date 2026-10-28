package com.david.notebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.david.notebook.R

/** A subject as an editor needs it: just something to pick. */
data class SubjectOption(val id: Long, val name: String)

/** One-of-many subject picker; tapping the selected subject again clears it. */
@Composable
fun SubjectChips(subjects: List<SubjectOption>, selected: Long?, onSelect: (Long?) -> Unit) {
    if (subjects.isEmpty()) {
        Text(
            stringResource(R.string.no_subjects_to_link),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(subjects, key = { it.id }) { subject ->
            FilterChip(
                selected = selected == subject.id,
                onClick = { onSelect(if (selected == subject.id) null else subject.id) },
                label = { Text(subject.name) },
            )
        }
    }
}

/**
 * "All" plus one chip per subject, to narrow a list down to one subject. Shown only
 * when there are at least two subjects to choose between.
 */
@Composable
fun SubjectFilter(subjects: List<SubjectOption>, selected: Long?, onSelect: (Long?) -> Unit, modifier: Modifier = Modifier) {
    if (subjects.size < 2) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        items(subjects, key = { it.id }) { subject ->
            FilterChip(
                selected = selected == subject.id,
                onClick = { onSelect(if (selected == subject.id) null else subject.id) },
                label = { Text(subject.name) },
            )
        }
    }
}

/** The distinct subjects among a list's items, in name order, for [SubjectFilter]. */
fun <T> subjectsIn(items: List<T>, id: (T) -> Long?, name: (T) -> String?): List<SubjectOption> =
    items.mapNotNull { item -> id(item)?.let { i -> name(item)?.let { SubjectOption(i, it) } } }
        .distinctBy { it.id }
        .sortedBy { it.name.lowercase() }
