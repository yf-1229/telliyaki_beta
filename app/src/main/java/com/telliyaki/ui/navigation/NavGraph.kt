package com.telliyaki.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = "homeScreen",
        modifier = modifier
    ) {
        composable(
            route = StartScreen.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            StartScreen(
                navigateBack = { navController.popBackStack() },
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(
            BlockDestination.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            BlockScreen(
                navigateToNodeAddFast = { navController.navigate(NodeAddFastDestination.route) },
                navigateToNodeAdd = { navController.navigate(NodeAddDestination.route) },
                navigateToNodeEdit = { navController.navigate("${NodeEditDestination.route}/$it") },
                navController = navController
            )
        }

        composable(
            TerminalDestination.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            PreviewScreen(
                navigateToNodeAdd = { navController.navigate(NodeAddNotToDoDestination.route) },
                navigateToNodeEdit = { navController.navigate("${NodeEditDestination.route}/$it") },
                navController = navController
            )
        }

        // TODO Add Settings
    }
}
