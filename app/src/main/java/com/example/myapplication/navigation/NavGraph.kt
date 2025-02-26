package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.ui.auth.AuthScreen
import com.example.myapplication.ui.groups.GroupsScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.viewmodel.GroupViewModel
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Groups : Screen("groups")
    object Profile : Screen("profile")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val groupViewModel: GroupViewModel = viewModel()
    val groups by groupViewModel.groups.collectAsState(initial = emptyList())
    val availableMembers by groupViewModel.availableMembers.collectAsState(initial = emptyList())

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route,
        modifier = modifier
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGroups = { navController.navigate(Screen.Groups.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.Groups.route) {
            GroupsScreen(
                groups = groups,
                availableMembers = availableMembers,
                onNavigateBack = { navController.popBackStack() },
                onGroupSelected = { groupId ->
                    // Handle group selection if needed
                },
                onCreateGroup = { name, members ->
                    groupViewModel.createGroup(
                        name = name,
                        members = members,
                        onSuccess = {
                            navController.popBackStack()
                        }
                    )
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onSignOut = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}