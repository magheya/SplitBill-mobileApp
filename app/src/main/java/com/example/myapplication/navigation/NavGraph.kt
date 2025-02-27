package com.example.myapplication.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.Member
import com.example.myapplication.ui.auth.AuthScreen
import com.example.myapplication.ui.groups.GroupsScreen
import com.example.myapplication.ui.groups.GroupDetailsScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.viewmodel.GroupViewModel
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Groups : Screen("groups")
    object GroupDetails : Screen("group/{groupId}") {
        fun createRoute(groupId: String) = "group/$groupId"
    }
    object Profile : Screen("profile")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val groupViewModel: GroupViewModel = viewModel()

    // Collect state from ViewModel
    val groups by groupViewModel.groups.collectAsState(initial = emptyList())
    val selectedGroup by groupViewModel.selectedGroup.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route,
        modifier = modifier
    ) {
        // Authentication screen
        composable(Screen.Auth.route) {
            AuthScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // Home screen
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGroups = { navController.navigate(Screen.Groups.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        // Groups screen
        composable(Screen.Groups.route) {
            GroupsScreen(
                groups = groups,
                onGroupSelected = { groupId: String ->
                    navController.navigate(Screen.GroupDetails.createRoute(groupId))
                },
                onCreateGroup = { name: String, members: List<Member> ->
                    groupViewModel.createGroup(
                        name = name,
                        members = members,
                        onSuccess = {
                            navController.popBackStack()
                        }
                    )
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Group details screen
        composable(
            route = Screen.GroupDetails.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable

            LaunchedEffect(groupId) {
                groupViewModel.selectGroup(groupId)
            }

            GroupDetailsScreen(
                group = selectedGroup,
                onNavigateBack = { navController.popBackStack() },
                onAddExpense = { expense ->
                    groupViewModel.addExpense(selectedGroup?.id ?: return@GroupDetailsScreen, expense)
                },
                onAddMember = { member ->
                    groupViewModel.addMember(selectedGroup?.id ?: return@GroupDetailsScreen, member)
                },
                onRemoveMember = { memberId ->
                    groupViewModel.removeMember(selectedGroup?.id ?: return@GroupDetailsScreen, memberId)
                },
                onDeleteGroup = {
                    groupViewModel.deleteGroup(selectedGroup?.id ?: return@GroupDetailsScreen) {
                        navController.popBackStack()
                    }
                }
            )
        }

        // Profile screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                onSignOut = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}