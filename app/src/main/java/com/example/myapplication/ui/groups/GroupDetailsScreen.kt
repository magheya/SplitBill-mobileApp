package com.example.myapplication.ui.groups

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*  // For fillMaxSize, fillMaxWidth, padding, Column, Spacer, height
import androidx.compose.runtime.Composable  // For @Composable functions
import androidx.compose.ui.Modifier  // For Modifier properties like fillMaxSize, fillMaxWidth
import com.example.myapplication.data.model.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    group: Group?,
    onNavigateBack: () -> Unit,
    onAddExpense: (Expense) -> Unit,
    onAddMember: (Member) -> Unit,
    onRemoveMember: (String) -> Unit,
    onDeleteGroup: () -> Unit
) {
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = group?.name ?: "Group Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddExpenseDialog = true }) {
                        Icon(Icons.Filled.Add, "Add Expense")
                    }
                    IconButton(onClick = { showConfirmDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete Group")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (group == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Expenses") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Members") }
                    )
                }

                when (selectedTab) {
                    0 -> GroupOverview(group)
                    1 -> ExpensesList(group.expenses.values.toList()) // Convert Map to List here
                    2 -> MembersList(
                        members = group.members,
                        onAddMember = { showAddMemberDialog = true },
                        onRemoveMember = onRemoveMember
                    )
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            members = group?.members?.values?.toList() ?: emptyList(), // Convert Map to List
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { expense ->
                onAddExpense(expense)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { member ->
                onAddMember(member)
                showAddMemberDialog = false
            }
        )
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete this group? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteGroup()
                        showConfirmDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GroupOverview(group: Group) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$${String.format("%.2f", group.totalAmount)}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Member Balances",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    group.members.values.forEach { member ->  // Changed to iterate over map values
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(member.name)
                            val balance = member.paid - member.owes
                            Text(
                                text = "$${String.format("%.2f", balance)}",
                                color = when {
                                    balance > 0 -> MaterialTheme.colorScheme.primary
                                    balance < 0 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesList(expenses: List<Expense>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(expenses) { expense ->
            ExpenseCard(expense)
        }
    }
}

@Composable
private fun ExpenseCard(expense: Expense) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "Paid by: ${expense.paidBy}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = expense.category.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MembersList(
    members: Map<String, Member>,  // Changed from List<Member> to Map<String, Member>
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Members (${members.size})",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onAddMember) {
                Icon(Icons.Filled.Add, "Add Member")
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(members.values.toList()) { member ->  // Convert map values to list
                ListItem(
                    headlineContent = { Text(member.name) },
                    leadingContent = { Icon(Icons.Filled.Person, "Member") },
                    trailingContent = {
                        IconButton(onClick = { onRemoveMember(member.id) }) {
                            Icon(Icons.Filled.Close, "Remove Member")
                        }
                    }
                )
            }
        }
    }
}