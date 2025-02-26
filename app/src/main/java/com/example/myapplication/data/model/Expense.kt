package com.example.myapplication.data.model

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val paidBy: String = "",
    val splitBetween: List<String> = emptyList(),
    val category: String = "",
    val createdAt: Long = System.currentTimeMillis()
)