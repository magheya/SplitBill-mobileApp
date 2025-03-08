package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Expense
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.ArrowBack
import com.example.myapplication.viewmodel.GroupViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    expenses: List<Expense>,
    users: Map<String, String>,
    onNavigateBack: () -> Unit,
    viewModel: GroupViewModel
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val (personalExpenses, personalDebts) = viewModel.calculatePersonalBalances(currentUserId)

    val categoryExpenses = expenses
        .filter { it.paidBy == currentUserId }
        .groupBy { it.category }
        .mapValues { it.value.sumOf { expense -> expense.amount } }

    val personalTopSpenders = expenses
        .filter { it.paidBy == currentUserId }
        .flatMap { expense ->
            expense.splitAmounts.map { (userId, amount) -> userId to amount }
        }
        .groupBy { it.first }
        .mapValues { it.value.sumOf { pair -> pair.second } }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack?.invoke() }) {  // Ensure it's not null
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Personal Expenses Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = "My Expenses",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "My Expenses",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            String.format("%.2f", personalExpenses),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Personal Debts Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = "My Debts",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "My Debts",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            String.format("%.2f", personalDebts),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Net Balance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = "Net Balance",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Net Balance",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            String.format("%.2f", personalExpenses - personalDebts),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (personalExpenses - personalDebts >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Personal Category Pie Chart
            item {
                CategoryPieChart(
                    categoryExpenses = categoryExpenses,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Personal Top Spenders
            item {
                if (personalTopSpenders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Top People I Paid For",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    personalTopSpenders.forEach { (userId, amount) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(users[userId] ?: "Unknown User")
                                Text(String.format("%.2f", amount))
                            }
                        }
                    }
                }
            }
        }
    }
}