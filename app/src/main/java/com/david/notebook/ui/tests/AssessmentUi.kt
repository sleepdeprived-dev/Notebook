package com.david.notebook.ui.tests

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.david.notebook.R
import com.david.notebook.data.courses.TagColor
import com.david.notebook.data.tests.AssessmentKind
import com.david.notebook.data.tests.AssessmentWithSubject
import com.david.notebook.ui.theme.chipColors
import java.text.DecimalFormat

// Bits of the Tests tab that the To Do tab shows too (its "Coming up" list and
// study tasks linked to a test).

fun AssessmentKind.label(): Int = when (this) {
    AssessmentKind.Exam -> R.string.kind_exam
    AssessmentKind.Colloquium -> R.string.kind_colloquium
    AssessmentKind.Deadline -> R.string.kind_deadline
}

/** Exam red, colloquium purple, deadline blue: the same palette as tags. */
@Composable
fun KindChip(kind: AssessmentKind) {
    val color = when (kind) {
        AssessmentKind.Exam -> TagColor.Red
        AssessmentKind.Colloquium -> TagColor.Purple
        AssessmentKind.Deadline -> TagColor.Blue
    }
    val (container, content) = color.chipColors()
    Surface(shape = RoundedCornerShape(8.dp), color = container, contentColor = content) {
        Text(
            stringResource(kind.label()),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** The name, or the subject when the name was left empty. */
@Composable
fun displayTitle(item: AssessmentWithSubject): String =
    item.assessment.title.ifBlank { item.subjectName ?: stringResource(item.assessment.assessmentKind().label()) }

@Composable
fun countdown(days: Int): String = when {
    days == 0 -> stringResource(R.string.due_today)
    days == 1 -> stringResource(R.string.due_tomorrow)
    days > 1 -> pluralStringResource(R.plurals.countdown_in_days, days, days)
    days == -1 -> stringResource(R.string.countdown_yesterday)
    else -> pluralStringResource(R.plurals.countdown_days_ago, -days, -days)
}

/** "9.5", "10", "7.33": no trailing zeros, at most two decimals. */
fun formatGrade(grade: Double): String = DecimalFormat("0.##").format(grade)

/** Accepts "9.5" and "9,5"; null for anything that isn't a non-negative number. */
fun parseGrade(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }
