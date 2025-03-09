// app/src/main/java/com/example/myapplication/ui/groups/GroupsScreen.kt
package com.example.myapplication.ui.groups

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*  // For fillMaxSize, fillMaxWidth, padding, Column, Spacer, height
import androidx.compose.runtime.Composable  // For @Composable functions
import androidx.compose.ui.Modifier  // For Modifier properties like fillMaxSize, fillMaxWidth
import androidx.compose.ui.unit.dp  // For dp unit (spacing, padding)
import com.example.myapplication.data.model.*

/**
 * Composable function for displaying the groups screen.
 *
 * @param groups The list of groups to display.
 * @param onCreateGroup Callback function to be called when a new group is created.
 * @param onNavigateBack Callback function to be called when navigating back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    groups: List<Group>,
    onGroupSelected: (String) -> Unit,
    onCreateGroup: (String, List<Member>) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var members by remember { mutableStateOf(listOf<Member>()) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Groups") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, "Create Group")
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "No Groups",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Groups",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(groups) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onGroupSelected(group.id) }
                    )
                }
            }
        }

        if (showCreateDialog) {
            CreateGroupDialog(
                groupName = groupName,
                members = members,
                onGroupNameChange = { groupName = it },
                onAddMember = { showAddMemberDialog = true },
                onRemoveMember = { memberToRemove ->
                    members = members.filterNot { it == memberToRemove }
                },
                onDismiss = {
                    showCreateDialog = false
                    groupName = ""
                    members = emptyList()
                },
                onConfirm = {
                    onCreateGroup(groupName, members)
                    showCreateDialog = false
                    groupName = ""
                    members = emptyList()
                }
            )
        }

        if (showAddMemberDialog) {
            AddMemberDialog(
                onDismiss = { showAddMemberDialog = false },
                onConfirm = { member ->
                    members = members + member
                    showAddMemberDialog = false
                }
            )
        }
    }
}

/**
 * Composable function for displaying a group card.
 *
 * @param group The group to display.
 * @param onClick Callback function to be called when the card is clicked.
 */
@Composable
private fun GroupCard(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${group.members.size} members",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Total: $${String.format("%.2f", group.totalAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateGroupDialog(
    groupName: String,
    members: List<Member>,
    onGroupNameChange: (String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (Member) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = onGroupNameChange,
                    label = { Text("Group Name") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Members:")
                members.forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(member.name)
                        IconButton(onClick = { onRemoveMember(member) }) {
                            Icon(Icons.Default.Remove, "Remove member")
                        }
                    }
                }

                TextButton(
                    onClick = onAddMember,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, "Add member")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Member")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = groupName.isNotBlank() && members.isNotEmpty()
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