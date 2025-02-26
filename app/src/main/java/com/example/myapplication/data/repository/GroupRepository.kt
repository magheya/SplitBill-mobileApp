package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.Expense
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroup(name: String, createdBy: String, members: List<String>): String
    suspend fun getGroup(groupId: String): Group?
    fun observeGroups(userId: String): Flow<List<Group>>
    suspend fun updateGroup(group: Group)
    suspend fun deleteGroup(groupId: String)
    suspend fun addExpense(expense: Expense)
    suspend fun getExpenses(groupId: String): List<Expense>
    fun observeExpenses(groupId: String): Flow<List<Expense>>
    suspend fun calculateBalances(groupId: String): Map<String, Double>
}