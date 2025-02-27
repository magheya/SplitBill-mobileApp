package com.example.myapplication.data.model

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val paidBy: String = "",
    val participants: List<String> = emptyList(), // member ids
    val splitAmounts: Map<String, Double> = emptyMap() // member id to amount
)
