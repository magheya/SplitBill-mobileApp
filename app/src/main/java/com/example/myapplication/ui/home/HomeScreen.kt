package com.example.myapplication.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    groupCount: Int,
    dashboardSummary: String,
    onNavigateToGroups: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp)) // Push cards higher

            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Groups Card
            ActionCard(
                title = "View Groups",
                subtitle = "$groupCount groups",
                icon = Icons.Default.Group,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = onNavigateToGroups
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dashboard Card
            ActionCard(
                title = "Dashboard & Insights",
                subtitle = dashboardSummary,
                icon = Icons.Default.Analytics,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onNavigateToDashboard
            )

            Spacer(modifier = Modifier.height(70.dp))

            // Temporary App Icon for "Split the Bill"
            Image(
                painter = painterResource(id = R.drawable.payment_green), // Replace with your actual app icon
                contentDescription = "Split The Bill App Icon",
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp) // Taller for more content
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp) // Bigger icon
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(title, fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(subtitle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}
