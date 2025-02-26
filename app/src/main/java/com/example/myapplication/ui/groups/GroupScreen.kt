package com.example.myapplication.ui.groups

import androidx.compose.foundation.layout.* // Missing import
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp // Missing import
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.User
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.selection.toggleable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    groups: List<Group>,
    availableMembers: List<User> = emptyList(), // Add this parameter
    onNavigateBack: () -> Unit,
    onGroupSelected: (String) -> Unit = {},
    onCreateGroup: (String, List<String>) -> Unit = { _, _ -> }
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf(setOf<String>()) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Groups") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, "Create Group")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.padding(16.dp)
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
            if (groups.isEmpty()) {
                Text(
                    text = "Your Groups",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No groups yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(groups) { group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { onGroupSelected(group.id) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${group.members.size} members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Select Members",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(availableMembers) { member ->
                            if (member.id != currentUser?.uid) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = selectedMembers.contains(member.id),
                                            onValueChange = { checked ->
                                                selectedMembers = if (checked) {
                                                    selectedMembers + member.id
                                                } else {
                                                    selectedMembers - member.id
                                                }
                                            }
                                        )
                                        .padding(vertical = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedMembers.contains(member.id),
                                        onCheckedChange = null
                                    )
                                    Text(
                                        text = member.email,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            onCreateGroup(newGroupName, selectedMembers.toList())
                            newGroupName = ""
                            selectedMembers = emptySet()
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newGroupName = ""
                        selectedMembers = emptySet()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}