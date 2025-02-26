package com.example.myapplication.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.User
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import android.util.Patterns
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    groups: List<Group>,
    availableMembers: List<User> = emptyList(),
    onNavigateBack: () -> Unit,
    onGroupSelected: (String) -> Unit = {},
    onCreateGroup: (String, List<String>) -> Unit = { _, _ -> }
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var newMemberEmail by remember { mutableStateOf("") }
    val addedEmails = remember { mutableStateListOf<String>() }
    val currentUser = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Groups") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Filled.Add, "Create Group")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Your Groups",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (groups.isEmpty()) {
                EmptyGroupsMessage()
            } else {
                GroupsList(groups, onGroupSelected)
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = {
                showCreateDialog = false
                newGroupName = ""
                addedEmails.clear()
            },
            onCreateGroup = { name, members ->
                onCreateGroup(name, members)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun EmptyGroupsMessage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No groups yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Create a group to start sharing expenses",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GroupsList(
    groups: List<Group>,
    onGroupSelected: (String) -> Unit
) {
    LazyColumn {
        items(groups) { group ->
            GroupCard(group, onGroupSelected)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupCard(
    group: Group,
    onGroupSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onGroupSelected(group.id) }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${group.members.size} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (group.pendingInvites.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.MailOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${group.pendingInvites.size} pending",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Balance summary
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val balance = 0.0 // Replace with actual balance calculation
                    val formattedBalance = NumberFormat.getCurrencyInstance().format(balance)
                    val color = when {
                        balance > 0 -> MaterialTheme.colorScheme.primary
                        balance < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Text(
                        text = formattedBalance,
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (balance >= 0) "you'll receive" else "you owe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { /* Add expense */ },
                    label = { Text("Add Expense") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                AssistChip(
                    onClick = { /* Show balances */ },
                    label = { Text("Balances") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreateGroup: (String, List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var memberEmail by remember { mutableStateOf("") }
    val addedMembers = remember { mutableStateListOf<String>() }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    isError = showError && groupName.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = memberEmail,
                        onValueChange = {
                            memberEmail = it
                            showError = false
                        },
                        label = { Text("Member Email") },
                        singleLine = true,
                        isError = showError,
                        supportingText = if (showError) {
                            { Text(errorMessage) }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            when {
                                !Patterns.EMAIL_ADDRESS.matcher(memberEmail).matches() -> {
                                    showError = true
                                    errorMessage = "Invalid email format"
                                }
                                memberEmail in addedMembers -> {
                                    showError = true
                                    errorMessage = "Email already added"
                                }
                                else -> {
                                    addedMembers.add(memberEmail)
                                    memberEmail = ""
                                    showError = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, "Add Member")
                    }
                }

                if (addedMembers.isNotEmpty()) {
                    Text(
                        text = "Added Members",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                    ) {
                        items(addedMembers) { email ->
                            ListItem(
                                headlineContent = { Text(email) },
                                trailingContent = {
                                    IconButton(
                                        onClick = { addedMembers.remove(email) }
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove member"
                                        )
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }

                if (showError && groupName.isBlank()) {
                    Text(
                        text = "Group name is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (groupName.isBlank()) {
                        showError = true
                        return@TextButton
                    }
                    if (addedMembers.isEmpty()) {
                        showError = true
                        errorMessage = "Add at least one member"
                        return@TextButton
                    }
                    onCreateGroup(groupName, addedMembers.toList())
                },
                enabled = !showError
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
