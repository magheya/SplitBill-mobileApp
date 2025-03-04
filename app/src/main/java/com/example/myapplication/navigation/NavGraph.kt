package com.example.myapplication.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.Member
import com.example.myapplication.data.model.Expense
import com.example.myapplication.ui.auth.AuthScreen
import com.example.myapplication.ui.dashboard.DashboardScreen
import com.example.myapplication.ui.groups.GroupsScreen
import com.example.myapplication.ui.groups.GroupDetailsScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.splash.SplashScreen
import com.example.myapplication.viewmodel.GroupViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.ArrowBack

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Groups : Screen("groups")
    object GroupDetails : Screen("group/{groupId}") {
        fun createRoute(groupId: String) = "group/$groupId"
    }
    object Profile : Screen("profile")
    object Dashboard : Screen("dashboard")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val groupViewModel: GroupViewModel = viewModel()

    // Collect state from ViewModel
    val groups by groupViewModel.groups.collectAsState(initial = emptyList())
    val selectedGroup by groupViewModel.selectedGroup.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        // Splash screen
        composable("splash") {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate("auth") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("auth") {
            AuthScreen(
                onSignInSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        // Home screen
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGroups = { navController.navigate(Screen.Groups.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },  // Add comma here
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) }
            )
        }

        // Dashboard screen
        composable(Screen.Dashboard.route) {
            val usersMap = groups.flatMap { group ->
                group.members?.let { members ->
                    members.map { (_, member) ->
                        member.id to member.name
                    }
                } ?: emptyList()
            }.toMap()

            DashboardScreen(
                expenses = groups.flatMap { group ->
                    group.expenses?.values?.toList() ?: emptyList<Expense>()
                },
                users = usersMap,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Groups screen
        composable(Screen.Groups.route) {
            GroupsScreen(
                groups = groups,
                onGroupSelected = { groupId ->
                    navController.navigate(Screen.GroupDetails.createRoute(groupId))
                },
                onCreateGroup = { name, members ->
                    groupViewModel.createGroup(
                        name = name,
                        members = members,
                        onSuccess = {
                            //navController.popBackStack()
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
                viewModel = groupViewModel,
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() },
                onAddExpense = { expense ->
                    groupViewModel.addExpense(groupId, expense)
                },
                onAddMember = { member ->
                    groupViewModel.addMember(groupId, member)
                },
                onRemoveMember = { memberId ->
                    groupViewModel.removeMember(groupId, memberId)
                },
                onDeleteGroup = {
                    groupViewModel.deleteGroup(groupId) {
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