package com.afrchat.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.afrchat.app.ui.admin.AdminDashboardScreen
import com.afrchat.app.ui.admin.ReportsScreen
import com.afrchat.app.ui.auth.ForgotPasswordScreen
import com.afrchat.app.ui.auth.LoginScreen
import com.afrchat.app.ui.auth.SignUpScreen
import com.afrchat.app.ui.chat.ChatScreen
import com.afrchat.app.ui.group.CreateGroupScreen
import com.afrchat.app.ui.group.GroupInfoScreen
import com.afrchat.app.ui.main.MainScreen
import com.afrchat.app.ui.profile.EditProfileScreen
import com.afrchat.app.ui.profile.ProfileScreen
import com.afrchat.app.ui.search.SearchScreen
import com.afrchat.app.ui.story.CreateStoryScreen
import com.afrchat.app.ui.story.StoryViewerScreen
import com.google.firebase.auth.FirebaseAuth
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AfrChatNavGraph() {
    val navController = rememberNavController()
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val startDestination = if (isLoggedIn) Routes.MAIN else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0)
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGNUP)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Routes.FORGOT_PASSWORD)
                }
            )
        }

        composable(Routes.SIGNUP) {
            SignUpScreen(
                onSignedUp = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0)
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onOpenConversation = { id, name ->
                    navController.navigate(
                        Routes.chat(id, encode(name))
                    )
                },
                onOpenSearch = {
                    navController.navigate(Routes.SEARCH)
                },
                onOpenProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onCreateStory = {
                    navController.navigate(Routes.CREATE_STORY)
                },
                onViewStories = { ownerUid ->
                    navController.navigate(
                        Routes.storyViewer(ownerUid)
                    )
                },
                onCreateGroup = {
                    navController.navigate(Routes.CREATE_GROUP)
                }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenConversation = { id, name ->
                    navController.popBackStack()
                    navController.navigate(
                        Routes.chat(id, encode(name))
                    )
                }
            )
        }

        composable(
            Routes.CHAT,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                },
                navArgument("peerName") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val peerName = decode(
                backStackEntry.arguments
                    ?.getString("peerName")
                    .orEmpty()
            )

            ChatScreen(
                peerName = peerName,
                onBack = {
                    navController.popBackStack()
                },
                onOpenGroupInfo = { groupId ->
                    val conversationId =
                        backStackEntry.arguments
                            ?.getString("conversationId")
                            .orEmpty()

                    navController.navigate(
                        Routes.groupInfo(
                            groupId,
                            conversationId
                        )
                    )
                }
            )
        }

        composable(Routes.CREATE_GROUP) {
            CreateGroupScreen(
                onBack = {
                    navController.popBackStack()
                },
                onGroupCreated = { conversationId ->
                    navController.popBackStack()
                    navController.navigate(
                        Routes.chat(
                            conversationId,
                            encode("Groupe")
                        )
                    )
                }
            )
        }

        composable(
            Routes.GROUP_INFO,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                },
                navArgument("conversationId") {
                    type = NavType.StringType
                }
            )
        ) {
            GroupInfoScreen(
                onBack = {
                    navController.popBackStack()
                },
                onLeft = {
                    navController.popBackStack(
                        Routes.MAIN,
                        false
                    )
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = {
                    navController.popBackStack()
                },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                },
                onOpenAdmin = {
                    navController.navigate(
                        Routes.ADMIN_DASHBOARD
                    )
                },
                onEditProfile = {
                    navController.navigate(
                        Routes.EDIT_PROFILE
                    )
                }
            )
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CREATE_STORY) {
            CreateStoryScreen(
                onPosted = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            Routes.STORY_VIEWER,
            arguments = listOf(
                navArgument("ownerUid") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            StoryViewerScreen(
                ownerUid = backStackEntry.arguments
                    ?.getString("ownerUid")
                    .orEmpty(),
                onClose = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenReports = {
                    navController.navigate(
                        Routes.ADMIN_REPORTS
                    )
                }
            )
        }

        composable(Routes.ADMIN_REPORTS) {
            ReportsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

private fun encode(value: String): String =
    URLEncoder.encode(value, "UTF-8")

private fun decode(value: String): String =
    try {
        URLDecoder.decode(value, "UTF-8")
    } catch (_: Exception) {
        value
    }
