package com.david.notebook.ui.courses

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

const val SUBJECT_ID_ARG = "subjectId"
const val FOLDER_ID_ARG = "folderId"

private const val SUBJECTS_ROUTE = "courses/subjects"
private const val SUBJECT_ROUTE = "courses/subject/{$SUBJECT_ID_ARG}"
private const val FOLDER_ROUTE = "courses/folder/{$FOLDER_ID_ARG}"

/**
 * Subjects → one subject's folders → one folder's PDFs, as a nested graph under [route].
 * Nesting is what keeps the Courses tab highlighted on the inner screens, and lets the
 * tab remember which folder was open when you switch tabs and come back.
 */
fun NavGraphBuilder.coursesGraph(route: String, navController: NavHostController) {
    navigation(startDestination = SUBJECTS_ROUTE, route = route) {
        composable(SUBJECTS_ROUTE) {
            CoursesScreen(onOpenSubject = { navController.navigate("courses/subject/$it") })
        }
        composable(
            SUBJECT_ROUTE,
            arguments = listOf(navArgument(SUBJECT_ID_ARG) { type = NavType.LongType }),
        ) {
            SubjectScreen(
                onOpenFolder = { navController.navigate("courses/folder/$it") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            FOLDER_ROUTE,
            arguments = listOf(navArgument(FOLDER_ID_ARG) { type = NavType.LongType }),
        ) {
            FolderScreen(onBack = { navController.popBackStack() })
        }
    }
}
