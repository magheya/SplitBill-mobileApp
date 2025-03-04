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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    expenses: List<Expense>,
    users: Map<String, String>,
    onNavigateBack: () -> Unit,
    viewModel: GroupViewModel
) {
    val totalExpenses = expenses.sumOf { it.amount }
    val totalDebts by viewModel.totalDebts.collectAsState()
    val categoryExpenses = expenses.groupBy { it.category }
        .mapValues { it.value.sumOf { expense -> expense.amount } }
    val topSpenders = expenses.flatMap { expense ->
        expense.splitAmounts.map { (userId, amount) -> userId to amount }
    }.groupBy { it.first }
        .mapValues { it.value.sumOf { pair -> pair.second } }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Total Expenses Card
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
                                contentDescription = "Total Expenses",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Total Expenses",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            String.format("%.2f", totalExpenses),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Total Debts Card
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
                                contentDescription = "Total Debts",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Total Debts",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            String.format("%.2f", totalDebts),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expense Debt Chart
            item {
                ExpenseDebtChart(
                    totalExpenses = totalExpenses,
                    totalDebts = totalDebts,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Category Pie Chart
            item {
                CategoryPieChart(
                    categoryExpenses = categoryExpenses,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Top Spenders
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Top Spenders",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                topSpenders.forEach { (userId, amount) ->
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