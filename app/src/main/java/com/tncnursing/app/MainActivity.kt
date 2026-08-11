package com.tncnursing.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tncnursing.app.ui.components.BottomNavigationBar
import com.tncnursing.app.ui.components.TopHeaderBar
import com.tncnursing.app.ui.navigation.Screen
import com.tncnursing.app.ui.screens.BookmarksScreen
import com.tncnursing.app.ui.screens.CourseDetailScreen
import com.tncnursing.app.ui.screens.CoursesScreen
import com.tncnursing.app.ui.screens.EnotesScreen
import com.tncnursing.app.ui.screens.HomeScreen
import com.tncnursing.app.ui.screens.LeaderboardScreen
import com.tncnursing.app.ui.screens.PdfViewerScreen
import com.tncnursing.app.ui.screens.ProfileScreen
import com.tncnursing.app.ui.screens.QuizListScreen
import com.tncnursing.app.ui.screens.QuizResultScreen
import com.tncnursing.app.ui.screens.QuizTakeScreen
import com.tncnursing.app.ui.screens.VideoPlayerScreen
import com.tncnursing.app.ui.screens.VideosScreen
import com.tncnursing.app.ui.theme.TncNursingTheme
import com.tncnursing.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            TncNursingTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val userProfile by viewModel.userProfile.collectAsState()

                // Routes that show top/bottom bars
                val mainTabRoutes = listOf(
                    Screen.Home.route,
                    Screen.Courses.route,
                    Screen.Videos.route,
                    Screen.Enotes.route,
                    Screen.QuizList.route,
                    Screen.Leaderboard.route,
                    Screen.Profile.route,
                    Screen.Bookmarks.route
                )

                val showBars = currentRoute in mainTabRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (showBars) {
                            TopHeaderBar(
                                totalXp = userProfile?.totalXp ?: 50,
                                streakDays = userProfile?.currentStreak ?: 1,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = { viewModel.toggleTheme() },
                                onBookmarksClick = { navController.navigate(Screen.Bookmarks.route) },
                                onLeaderboardClick = { navController.navigate(Screen.Leaderboard.route) }
                            )
                        }
                    },
                    bottomBar = {
                        if (showBars) {
                            BottomNavigationBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToCourses = { navController.navigate(Screen.Courses.route) },
                                onNavigateToVideos = { navController.navigate(Screen.Videos.route) },
                                onNavigateToEnotes = { navController.navigate(Screen.Enotes.route) },
                                onNavigateToQuizzes = { navController.navigate(Screen.QuizList.route) },
                                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                                onCourseClick = { courseRowId ->
                                    navController.navigate(Screen.CourseDetail.createRoute(courseRowId))
                                },
                                onSessionClick = { session ->
                                    if (session.type == "pdf" || session.pdfUrl != null) {
                                        navController.navigate(Screen.PdfViewer.createRoute(session.rowId))
                                    } else {
                                        navController.navigate(Screen.VideoPlayer.createRoute(session.rowId))
                                    }
                                }
                            )
                        }

                        composable(Screen.Courses.route) {
                            CoursesScreen(
                                viewModel = viewModel,
                                onCourseClick = { courseRowId ->
                                    navController.navigate(Screen.CourseDetail.createRoute(courseRowId))
                                }
                            )
                        }

                        composable(
                            route = Screen.CourseDetail.route,
                            arguments = listOf(navArgument("courseRowId") { type = NavType.StringType })
                        ) { backStack ->
                            val courseRowId = backStack.arguments?.getString("courseRowId") ?: ""
                            CourseDetailScreen(
                                courseRowId = courseRowId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onSessionClick = { session ->
                                    if (session.type == "pdf" || session.pdfUrl != null) {
                                        navController.navigate(Screen.PdfViewer.createRoute(session.rowId))
                                    } else {
                                        navController.navigate(Screen.VideoPlayer.createRoute(session.rowId))
                                    }
                                }
                            )
                        }

                        composable(Screen.Videos.route) {
                            VideosScreen(
                                viewModel = viewModel,
                                onSessionClick = { session ->
                                    navController.navigate(Screen.VideoPlayer.createRoute(session.rowId))
                                }
                            )
                        }

                        composable(
                            route = Screen.VideoPlayer.route,
                            arguments = listOf(navArgument("sessionRowId") { type = NavType.StringType })
                        ) { backStack ->
                            val sessionRowId = backStack.arguments?.getString("sessionRowId") ?: ""
                            VideoPlayerScreen(
                                sessionRowId = sessionRowId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Enotes.route) {
                            EnotesScreen(
                                viewModel = viewModel,
                                onSessionClick = { session ->
                                    navController.navigate(Screen.PdfViewer.createRoute(session.rowId))
                                }
                            )
                        }

                        composable(
                            route = Screen.PdfViewer.route,
                            arguments = listOf(navArgument("sessionRowId") { type = NavType.StringType })
                        ) { backStack ->
                            val sessionRowId = backStack.arguments?.getString("sessionRowId") ?: ""
                            PdfViewerScreen(
                                sessionRowId = sessionRowId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.QuizList.route) {
                            QuizListScreen(
                                viewModel = viewModel,
                                onQuizClick = { examId ->
                                    navController.navigate(Screen.QuizTake.createRoute(examId))
                                }
                            )
                        }

                        composable(
                            route = Screen.QuizTake.route,
                            arguments = listOf(navArgument("examId") { type = NavType.StringType })
                        ) { backStack ->
                            val examId = backStack.arguments?.getString("examId") ?: ""
                            QuizTakeScreen(
                                examId = examId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onQuizSubmitted = { id ->
                                    navController.navigate(Screen.QuizResult.createRoute(id)) {
                                        popUpTo(Screen.QuizList.route)
                                    }
                                }
                            )
                        }

                        composable(
                            route = Screen.QuizResult.route,
                            arguments = listOf(navArgument("examId") { type = NavType.StringType })
                        ) { backStack ->
                            val examId = backStack.arguments?.getString("examId") ?: ""
                            QuizResultScreen(
                                examId = examId,
                                viewModel = viewModel,
                                onDoneClick = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Leaderboard.route) {
                            LeaderboardScreen(viewModel = viewModel)
                        }

                        composable(Screen.Bookmarks.route) {
                            BookmarksScreen(
                                viewModel = viewModel,
                                onCourseClick = { courseRowId ->
                                    navController.navigate(Screen.CourseDetail.createRoute(courseRowId))
                                },
                                onSessionClick = { session ->
                                    if (session.type == "pdf" || session.pdfUrl != null) {
                                        navController.navigate(Screen.PdfViewer.createRoute(session.rowId))
                                    } else {
                                        navController.navigate(Screen.VideoPlayer.createRoute(session.rowId))
                                    }
                                }
                            )
                        }

                        composable(Screen.Profile.route) {
                            ProfileScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
