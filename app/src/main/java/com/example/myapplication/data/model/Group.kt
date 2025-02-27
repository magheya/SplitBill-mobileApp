package com.example.myapplication.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val members: Map<String, Member> = emptyMap(), // Changed from List to Map
    val expenses: Map<String, Expense> = emptyMap(),
    val totalAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val splitType: SplitType = SplitType.EQUAL
) {
    constructor() : this(
        id = "",
        name = "",
        createdBy = "",
        members = emptyMap(),
        expenses = emptyMap(),
        totalAmount = 0.0,
        createdAt = System.currentTimeMillis(),
        splitType = SplitType.EQUAL
    )
}
