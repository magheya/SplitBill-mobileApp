package com.example.myapplication.ui.groups

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*  // For fillMaxSize, fillMaxWidth, padding, Column, Spacer, height
import androidx.compose.runtime.Composable  // For @Composable functions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.ExpenseCategory
import com.example.myapplication.data.model.Member
import com.example.myapplication.data.model.SplitType
import java.util.*
//vertical scroll and rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    members: List<Member>,
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var paidById by remember { mutableStateOf("") }
    var selectedParticipants by remember { mutableStateOf(members.map { it.id }.toSet()) }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    var customSplitAmounts by remember { mutableStateOf(members.associate { it.id to "" }) }
    var percentageSplitAmounts by remember { mutableStateOf(members.associate { it.id to "" }) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showPaidByDropdown by remember { mutableStateOf(false) }

    // Validation functions
    fun validateCustomSplits(): Boolean {
        val total = customSplitAmounts
            .filterKeys { selectedParticipants.contains(it) }
            .values
            .mapNotNull { it.toDoubleOrNull() }
            .sum()
        return total == (amount.toDoubleOrNull() ?: 0.0)
    }

    fun validatePercentageSplits(): Boolean {
        val total = percentageSplitAmounts
            .filterKeys { selectedParticipants.contains(it) }
            .values
            .mapNotNull { it.toDoubleOrNull() }
            .sum()
        return total == 100.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, "Show categories") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("Category") }
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        ExpenseCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = showPaidByDropdown,
                    onExpandedChange = { showPaidByDropdown = !showPaidByDropdown }
                ) {
                    OutlinedTextField(
                        value = members.find { it.id == paidById }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, "Show members") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("Paid By") }
                    )

                    ExposedDropdownMenu(
                        expanded = showPaidByDropdown,
                        onDismissRequest = { showPaidByDropdown = false }
                    ) {
                        members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name) },
                                onClick = {
                                    paidById = member.id
                                    showPaidByDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Split Type", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SplitType.values().forEach { type ->
                        FilterChip(
                            selected = splitType == type,
                            onClick = { splitType = type },
                            label = { Text(type.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Participants", style = MaterialTheme.typography.titleSmall)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    members.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(member.name)
                            Checkbox(
                                checked = selectedParticipants.contains(member.id),
                                onCheckedChange = { checked ->
                                    selectedParticipants = if (checked) {
                                        selectedParticipants + member.id
                                    } else {
                                        selectedParticipants - member.id
                                    }
                                }
                            )
                        }

                        if (selectedParticipants.contains(member.id)) {
                            when (splitType) {
                                SplitType.EQUAL -> {
                                    val equalShare = amount.toDoubleOrNull()?.let {
                                        it / selectedParticipants.size
                                    } ?: 0.0
                                    Text(
                                        "Will pay: ${String.format("%.2f", equalShare)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                                    )
                                }
                                SplitType.CUSTOM -> {
                                    OutlinedTextField(
                                        value = customSplitAmounts[member.id] ?: "",
                                        onValueChange = { value ->
                                            if (value.isEmpty() || value.toDoubleOrNull() != null) {
                                                customSplitAmounts = customSplitAmounts + (member.id to value)
                                            }
                                        },
                                        label = { Text("Amount") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    )
                                }
                                SplitType.PERCENTAGE -> {
                                    OutlinedTextField(
                                        value = percentageSplitAmounts[member.id] ?: "",
                                        onValueChange = { value ->
                                            if (value.isEmpty() || (value.toDoubleOrNull() != null && value.toDouble() <= 100)) {
                                                percentageSplitAmounts = percentageSplitAmounts + (member.id to value)
                                            }
                                        },
                                        label = { Text("Percentage") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    when (splitType) {
                        SplitType.CUSTOM -> {
                            if (!validateCustomSplits()) {
                                Text(
                                    "Total must equal ${amount}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        SplitType.PERCENTAGE -> {
                            if (!validatePercentageSplits()) {
                                Text(
                                    "Percentages must sum to 100%",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    val splitAmounts = when (splitType) {
                        SplitType.EQUAL -> {
                            val splitAmount = amountValue / selectedParticipants.size
                            selectedParticipants.associateWith { splitAmount }
                        }
                        SplitType.CUSTOM -> {
                            customSplitAmounts
                                .filterKeys { selectedParticipants.contains(it) }
                                .mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                        }
                        SplitType.PERCENTAGE -> {
                            percentageSplitAmounts
                                .filterKeys { selectedParticipants.contains(it) }
                                .mapValues { (_, percentage) ->
                                    val percentageValue = percentage.toDoubleOrNull() ?: 0.0
                                    (percentageValue / 100.0) * amountValue
                                }
                        }
                    }

                    onConfirm(
                        Expense(
                            id = UUID.randomUUID().toString(),
                            amount = amountValue,
                            description = description,
                            category = selectedCategory,
                            paidBy = paidById,
                            participants = selectedParticipants.toList(),
                            splitAmounts = splitAmounts
                        )
                    )
                },
                enabled = description.isNotBlank() &&
                        amount.isNotBlank() &&
                        paidById.isNotBlank() &&
                        selectedParticipants.isNotEmpty() &&
                        when (splitType) {
                            SplitType.EQUAL -> true
                            SplitType.CUSTOM -> validateCustomSplits()
                            SplitType.PERCENTAGE -> validatePercentageSplits()
                        }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}