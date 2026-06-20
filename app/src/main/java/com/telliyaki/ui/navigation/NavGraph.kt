package com.telliyaki.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.telliyaki.blockeditor.BlockWorkspaceViewModel
import com.telliyaki.data.BlocklyCommand
import com.telliyaki.network.TelloUdpClient
import com.telliyaki.storage.ProgramStorage
import com.telliyaki.ui.screen.BlockEditorScreen
import com.telliyaki.ui.screen.ExecutionScreen
import com.telliyaki.ui.screen.PreviewScreen
import com.telliyaki.ui.screen.ProgramListScreen
import com.telliyaki.ui.screen.StartScreen
import com.telliyaki.ui.viewmodel.ExecutionViewModel
import com.telliyaki.validator.ProgramValidator

object Routes {
    const val START = "start"
    const val PROGRAM_LIST = "programList"
    const val BLOCKLY = "blockly/{programName}"
    const val PREVIEW = "preview"
    const val EXECUTION = "execution"

    fun blockly(programName: String) = "blockly/$programName"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val validator = remember { ProgramValidator() }
    val telloClient = remember { TelloUdpClient() }

    NavHost(
        navController = navController,
        startDestination = Routes.START,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        composable(route = Routes.START) {
            StartScreen(
                onStartClick = {
                    navController.navigate(Routes.PROGRAM_LIST) {
                        popUpTo(Routes.START) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Routes.PROGRAM_LIST) {
            val context = LocalContext.current
            val storage = remember { ProgramStorage(context) }

            ProgramListScreen(
                storage = storage,
                onBackClick = { navController.popBackStack() },
                onProgramSelect = { programName ->
                    navController.navigate(Routes.blockly(programName))
                }
            )
        }

        composable(
            route = Routes.BLOCKLY,
            arguments = listOf(navArgument("programName") { type = NavType.StringType })
        ) { backStackEntry ->
            val programName = backStackEntry.arguments?.getString("programName") ?: "プログラム"
            val context = LocalContext.current
            val storage = remember { ProgramStorage(context) }
            val blockWorkspaceViewModel: BlockWorkspaceViewModel = viewModel()

            BlockEditorScreen(
                viewModel = blockWorkspaceViewModel,
                storage = storage,
                validator = validator,
                telloClient = telloClient,
                programName = programName,
                onBackClick = { navController.popBackStack() },
                onPreviewClick = { commands ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("commands", commands)
                    navController.navigate(Routes.PREVIEW)
                },
                onExecuteClick = { commands ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("commands", commands)
                    navController.navigate(Routes.EXECUTION)
                }
            )
        }

        composable(route = Routes.PREVIEW) {
            val commands = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<BlocklyCommand>>("commands")
                ?: emptyList()

            PreviewScreen(
                commands = commands,
                onBackClick = { navController.popBackStack() },
                onExecuteClick = {
                    // Save commands before navigating to Execution
                    navController.currentBackStackEntry?.savedStateHandle?.set("commands", commands)
                    navController.navigate(Routes.EXECUTION)
                }
            )
        }

        composable(route = Routes.EXECUTION) {
            val commands = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<BlocklyCommand>>("commands")
                ?: emptyList()
            val executionViewModel: ExecutionViewModel = viewModel()

            ExecutionScreen(
                commands = commands,
                executionViewModel = executionViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
