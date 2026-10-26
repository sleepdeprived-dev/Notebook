package com.david.notebook.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.david.notebook.R

/** The four bottom-bar destinations, in the order they appear. */
enum class Tab(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    Schedule("schedule", R.string.tab_schedule, R.drawable.ic_event),
    Courses("courses", R.string.tab_courses, R.drawable.ic_school),
    Tests("tests", R.string.tab_tests, R.drawable.ic_assignment),
    Todo("todo", R.string.tab_todo, R.drawable.ic_check_circle),
}
