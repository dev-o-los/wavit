package com.wavit.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wavit.android.ui.player.PlayerScreen
import com.wavit.android.ui.player.PlayerViewModel
import com.wavit.android.ui.search.SearchScreen
import com.wavit.android.ui.search.SearchViewModel

@Composable
fun WavitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Search.route,
        modifier = modifier
    ) {
        composable(Screen.Search.route) {
            val searchViewModel: SearchViewModel = viewModel()
            SearchScreen(
                viewModel = searchViewModel,
                onSongSelected = { song ->
                    navController.navigate(
                        Screen.Player.createRoute(
                            videoId = song.videoId,
                            title = song.title,
                            artist = song.artist,
                            thumbnailUrl = song.thumbnailUrl
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("artist") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("thumbnailUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId").orEmpty()
            val title = Screen.Player.decodeParam(backStackEntry.arguments?.getString("title"))
            val artist = Screen.Player.decodeParam(backStackEntry.arguments?.getString("artist"))
            val thumbnailUrl = Screen.Player.decodeParam(backStackEntry.arguments?.getString("thumbnailUrl"))
                .takeIf { it.isNotBlank() }

            // Trigger autoplay upon arrival with selected track
            LaunchedEffect(videoId) {
                if (videoId.isNotBlank()) {
                    playerViewModel.loadAndPlay(
                        videoId = videoId,
                        title = title,
                        artist = artist,
                        thumbnailUrl = thumbnailUrl
                    )
                }
            }

            PlayerScreen(
                viewModel = playerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

