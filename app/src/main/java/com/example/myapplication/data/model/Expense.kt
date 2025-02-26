package com.example.myapplication.data.model

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val participants: List<String> = emptyList(),
    val category: String = "",
    val date: Long = System.currentTimeMillis()
)

