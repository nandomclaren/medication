package com.medicontrol.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medicontrol.app.ui.addedit.AddEditMedicationScreen
import com.medicontrol.app.ui.backup.BackupScreen
import com.medicontrol.app.ui.home.HomeScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_ADD_EDIT = "add_edit"
private const val ROUTE_BACKUP = "backup"
private const val ARG_MEDICATION_ID = "medicationId"

@Composable
fun MediControlNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                onAddMedication = { navController.navigate("$ROUTE_ADD_EDIT?$ARG_MEDICATION_ID=-1") },
                onEditMedication = { id -> navController.navigate("$ROUTE_ADD_EDIT?$ARG_MEDICATION_ID=$id") },
                onOpenBackup = { navController.navigate(ROUTE_BACKUP) }
            )
        }
        composable(
            route = "$ROUTE_ADD_EDIT?$ARG_MEDICATION_ID={$ARG_MEDICATION_ID}",
            arguments = listOf(navArgument(ARG_MEDICATION_ID) { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(ARG_MEDICATION_ID) ?: -1L
            AddEditMedicationScreen(
                medicationId = id.takeIf { it != -1L },
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(ROUTE_BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
    }
}
