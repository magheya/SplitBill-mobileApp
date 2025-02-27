package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.Member
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroup(name: String, createdBy: String, members: List<Member>): String
    fun observeGroups(userId: String): Flow<List<Group>>
    suspend fun addExpense(groupId: String, expense: Expense)
    fun observeExpenses(groupId: String): Flow<List<Expense>>
    suspend fun calculateBalances(groupId: String): Map<String, Double>
    suspend fun getGroup(groupId: String): Group?
    suspend fun getExpenses(groupId: String): List<Expense>
    suspend fun updateGroup(group: Group)
    suspend fun deleteGroup(groupId: String)
    suspend fun addMember(groupId: String, member: Member)
    suspend fun removeMember(groupId: String, memberId: String)
}