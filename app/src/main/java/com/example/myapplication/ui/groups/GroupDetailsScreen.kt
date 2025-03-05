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
import com.example.myapplication.viewmodel.GroupViewModel
import com.example.myapplication.ui.groups.AddMemberDialog
import com.example.myapplication.ui.groups.AddExpenseDialog
import android.content.Context
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.util.PdfExporter
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    viewModel: GroupViewModel,
    groupId: String,
    onNavigateBack: () -> Unit,
    onAddExpense: (Expense) -> Unit,
    onAddMember: (Member) -> Unit,
    onRemoveMember: (String) -> Unit,
    onDeleteGroup: () -> Unit,
    onNavigateToScanner: () -> Unit // Add this parameter
) {
    val context = LocalContext.current
    val group = viewModel.selectedGroup.collectAsState().value
    val balances = viewModel.calculateMemberBalances(groupId)
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showExportSnackbar by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val selectedGroup by viewModel.selectedGroup.collectAsState()

    val settlements by viewModel.settlements.collectAsState()

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
                    IconButton(
                        onClick = {
                            group?.let { safeGroup ->
                                PdfExporter().exportGroupDetails(
                                    context = context,
                                    group = safeGroup,
                                    balances = balances,
                                    onComplete = { filePath ->
                                        val file = File(filePath)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "Open PDF")
                                        )
                                        exportMessage = "PDF exported and opened"
                                        showExportSnackbar = true
                                    },
                                    onError = { error ->
                                        exportMessage = "Failed to export PDF: ${error.message}"
                                        showExportSnackbar = true
                                    }
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Save, "Export PDF")
                    }
                    IconButton(onClick = { showAddExpenseDialog = true }) {
                        Icon(Icons.Filled.Add, "Add Expense")
                    }
                    IconButton(onClick = { showConfirmDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete Group")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        } ,
        floatingActionButton = {
            // Add a FloatingActionButton for receipt scanning
            ExtendedFloatingActionButton(
                onClick = onNavigateToScanner,
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                text = { Text("Scan Receipt") }
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
                    0 -> GroupOverview(
                        group = group,
                        balances = balances,
                        settlements = settlements
                    )
                    1 -> ExpensesList(
                        expenses = group.expenses.values.toList(),
                        members = group.members  // Pass the members map
                    )
                    2 -> MembersList(
                        members = group.members,
                        onAddMember = { showAddMemberDialog = true },
                        onRemoveMember = onRemoveMember
                    )
                }
            }
        }
    }

    LaunchedEffect(showExportSnackbar) {
        if (showExportSnackbar) {
            snackbarHostState.showSnackbar(exportMessage)
            showExportSnackbar = false
        }
    }

    if (showAddExpenseDialog && group != null) {
        AddExpenseDialog(
            members = group.members.values.toList(),
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { expense ->
                viewModel.addExpense(groupId, expense)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { member ->
                viewModel.addMember(groupId, member)
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
private fun GroupOverview(
    group: Group,
    balances: Map<String, Balance>,
    settlements: List<Pair<Member, Member>>

) {

    val totalAmount by rememberUpdatedState(newValue = group.totalAmount) // Update dynamically

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${String.format("%.2f", totalAmount)}",  // Updates instantly
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Suggested Settlements",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (settlements.isEmpty()) {
                    Text(
                        text = "All balances are settled!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    settlements.forEach { (debtor, creditor) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${debtor.name} → ${creditor.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$${String.format("%.2f", debtor.owes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(
    name: String,
    balance: Double,
    paid: Double,
    owes: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name)
            Text(
                text = "$${String.format("%.2f", balance)}",
                color = when {
                    balance > 0 -> MaterialTheme.colorScheme.primary
                    balance < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Paid: $${String.format("%.2f", paid)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Owes: $${String.format("%.2f", owes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpensesList(
    expenses: List<Expense>,
    members: Map<String, Member>  // Add this parameter
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(expenses) { expense ->
            ExpenseCard(expense, members)  // Pass members to ExpenseCard
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    members: Map<String, Member>  // Add this parameter
) {
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
                text = "Paid by: ${members[expense.paidBy]?.name ?: "Unknown"}",  // Use member name
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
    members: Map<String, Member>,
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
            items(members.values.toList()) { member ->
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

