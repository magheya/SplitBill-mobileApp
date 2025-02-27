package com.example.myapplication.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val members: List<Member> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val expenses : List<Expense> = emptyList(),
    val totalAmount: Double = 0.0,
    val splitType: SplitType = SplitType.EQUAL
)
