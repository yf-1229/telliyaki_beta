package com.telliyaki.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.telliyaki.blockly.BlocklyBridge
import com.telliyaki.blockly.BlocklyJsonParser
import com.telliyaki.executor.CommandExecutor
import com.telliyaki.network.TelloUdpClient
import com.telliyaki.storage.ProgramStorage
import com.telliyaki.ui.screen.BlocklyScreen
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
    val bridge = remember { BlocklyBridge() }
    val parser = remember { BlocklyJsonParser() }
    val validator = remember { ProgramValidator() }
    val telloClient = remember { TelloUdpClient() }
    val executor = remember { CommandExecutor(telloClient) }
    val executionViewModel = remember { ExecutionViewModel(telloClient) }

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
            val context = androidx.compose.ui.platform.LocalContext.current
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
            val context = androidx.compose.ui.platform.LocalContext.current
            val storage = remember { ProgramStorage(context) }

            BlocklyScreen(
                bridge = bridge,
                storage = storage,
                validator = validator,
                parser = parser,
                onBackClick = { navController.popBackStack() },
                onPreviewClick = { json ->
                    val commands = parser.parse(json)
                    navController.currentBackStackEntry?.savedStateHandle?.set("commands", commands)
                    navController.navigate(Routes.PREVIEW)
                },
                onExecuteClick = { json ->
                    val commands = parser.parse(json)
                    navController.currentBackStackEntry?.savedStateHandle?.set("commands", commands)
                    navController.navigate(Routes.EXECUTION)
                }
            )
        }

        composable(route = Routes.PREVIEW) {
            val commands = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<com.telliyaki.data.BlocklyCommand>>("commands")
                ?: emptyList()

            PreviewScreen(
                commands = commands,
                onBackClick = { navController.popBackStack() },
                onExecuteClick = {
                    navController.navigate(Routes.EXECUTION) {
                        popUpTo(Routes.BLOCKLY)
                    }
                }
            )
        }

        composable(route = Routes.EXECUTION) {
            val commands = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<com.telliyaki.data.BlocklyCommand>>("commands")
                ?: emptyList()

            ExecutionScreen(
                commands = commands,
                executionViewModel = executionViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
