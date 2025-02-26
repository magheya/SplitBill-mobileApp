package com.example.myapplication.ui.groups

import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Group
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.*  // For Box, Column, fillMaxSize, padding
import androidx.compose.foundation.lazy.LazyColumn  // For LazyColumn
import androidx.compose.foundation.lazy.items  // For items in LazyColumn
import androidx.compose.material3.AlertDialog  // For AlertDialog
import androidx.compose.material3.CircularProgressIndicator  // For CircularProgressIndicator
import androidx.compose.material3.ListItem  // For ListItem
import androidx.compose.runtime.remember  // For remember state management


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    group: Group?,
    onNavigateBack: () -> Unit
) {
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Filled.PersonAdd, "Add Member")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Group Information",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ListItem(
                                headlineContent = { Text(group.name) },
                                leadingContent = { Icon(Icons.Filled.Group, "Group Name") },
                                overlineContent = { Text("NAME") }
                            )
                            ListItem(
                                headlineContent = { Text("${group.members.size} members") },
                                leadingContent = { Icon(Icons.Filled.People, "Members Count") },
                                overlineContent = { Text("MEMBERS") }
                            )
                            if (group.createdBy.isNotEmpty()) {
                                ListItem(
                                    headlineContent = { Text(group.createdBy) },
                                    leadingContent = { Icon(Icons.Filled.Person, "Created By") },
                                    overlineContent = { Text("CREATED BY") }
                                )
                            }
                        }
                    }
                }

                if (group.members.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Members",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(
                                onClick = { showAddMemberDialog = true }
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Add Member",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }

                    items(group.members) { memberId ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(memberId) },
                                leadingContent = { Icon(Icons.Filled.Person, "Member") },
                                trailingContent = {
                                    if (memberId != group.createdBy) {
                                        IconButton(onClick = { /* Handle remove member */ }) {
                                            Icon(Icons.Filled.Close, "Remove Member")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                if (group.pendingInvites.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Invites",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(group.pendingInvites) { email ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(email) },
                                leadingContent = { Icon(Icons.Filled.Email, "Pending Invite") },
                                trailingContent = {
                                    IconButton(onClick = { /* Handle cancel invite */ }) {
                                        Icon(Icons.Filled.Close, "Cancel Invite")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddMemberDialog) {
        // Add member dialog implementation
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete this group? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Handle delete group
                        showConfirmDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
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