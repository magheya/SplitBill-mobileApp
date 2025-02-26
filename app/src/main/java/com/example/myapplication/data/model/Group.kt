package com.example.myapplication.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val pendingInvites: List<String> = emptyList()
)

