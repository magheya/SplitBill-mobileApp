package com.example.myapplication.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.Expense


class FirebaseGroupRepository : GroupRepository {
    private val database = FirebaseDatabase.getInstance()
    private val groupsRef = database.getReference("groups")
    private val expensesRef = database.getReference("expenses")

    override suspend fun createGroup(name: String, createdBy: String, members: List<String>): String {
        val groupId = groupsRef.push().key ?: throw IllegalStateException("Couldn't get push key")

        val groupData = hashMapOf(
            "id" to groupId,
            "name" to name,
            "createdBy" to createdBy,
            "members" to (listOf(createdBy) + members),
            "pendingInvites" to members,
            "createdAt" to System.currentTimeMillis()
        )

        groupsRef.child(groupId).setValue(groupData).await()
        return groupId
    }

    override fun observeGroups(userId: String): Flow<List<Group>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = snapshot.children.mapNotNull { childSnapshot ->
                    try {
                        val id = childSnapshot.key ?: return@mapNotNull null
                        val name = childSnapshot.child("name").getValue(String::class.java) ?: ""
                        val createdBy = childSnapshot.child("createdBy").getValue(String::class.java) ?: ""
                        val members = childSnapshot.child("members").children.mapNotNull { it.getValue(String::class.java) }
                        val pendingInvites = childSnapshot.child("pendingInvites").children.mapNotNull { it.getValue(String::class.java) }
                        val createdAt = childSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                        Group(
                            id = id,
                            name = name,
                            createdBy = createdBy,
                            members = members,
                            pendingInvites = pendingInvites,
                            createdAt = createdAt
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.filter { group ->
                    group.members.contains(userId) || group.pendingInvites.contains(userId)
                }
                trySend(groups)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        groupsRef.addValueEventListener(listener)
        awaitClose { groupsRef.removeEventListener(listener) }
    }

    override suspend fun addExpense(expense: Expense) {
        val expenseId = expensesRef.child(expense.groupId).push().key
            ?: throw IllegalStateException("Couldn't get push key")

        val expenseData = hashMapOf(
            "id" to expenseId,
            "groupId" to expense.groupId,
            "amount" to expense.amount,
            "description" to expense.description,
            "category" to expense.category,
            "paidBy" to expense.paidBy,
            "splitBetween" to expense.splitBetween,
            "createdAt" to System.currentTimeMillis()
        )

        expensesRef.child(expense.groupId)
            .child(expenseId)
            .setValue(expenseData)
            .await()
    }

    override fun observeExpenses(groupId: String): Flow<List<Expense>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val expenses = snapshot.children.mapNotNull { childSnapshot ->
                    try {
                        val id = childSnapshot.key ?: return@mapNotNull null
                        val amount = childSnapshot.child("amount").getValue(Double::class.java) ?: 0.0
                        val description = childSnapshot.child("description").getValue(String::class.java) ?: ""
                        val category = childSnapshot.child("category").getValue(String::class.java) ?: ""
                        val paidBy = childSnapshot.child("paidBy").getValue(String::class.java) ?: ""
                        val splitBetween = childSnapshot.child("splitBetween").children.mapNotNull { it.getValue(String::class.java) }
                        val createdAt = childSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                        Expense(
                            id = id,
                            groupId = groupId,
                            amount = amount,
                            description = description,
                            category = category,
                            paidBy = paidBy,
                            splitBetween = splitBetween,
                            createdAt = createdAt
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(expenses)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        expensesRef.child(groupId).addValueEventListener(listener)
        awaitClose { expensesRef.child(groupId).removeEventListener(listener) }
    }

    override suspend fun calculateBalances(groupId: String): Map<String, Double> {
        val expenses = getExpenses(groupId)
        val balances = mutableMapOf<String, Double>()

        expenses.forEach { expense ->
            val perPersonAmount = expense.amount / expense.splitBetween.size
            // Add amount to person who paid
            balances[expense.paidBy] = (balances[expense.paidBy] ?: 0.0) + expense.amount
            // Subtract share from each person involved
            expense.splitBetween.forEach { person ->
                balances[person] = (balances[person] ?: 0.0) - perPersonAmount
            }
        }

        return balances
    }

    override suspend fun getGroup(groupId: String): Group? {
        val snapshot = groupsRef.child(groupId).get().await()
        return try {
            val id = snapshot.key ?: return null
            val name = snapshot.child("name").getValue(String::class.java) ?: ""
            val createdBy = snapshot.child("createdBy").getValue(String::class.java) ?: ""
            val members = snapshot.child("members").children.mapNotNull { it.getValue(String::class.java) }
            val pendingInvites = snapshot.child("pendingInvites").children.mapNotNull { it.getValue(String::class.java) }
            val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

            Group(
                id = id,
                name = name,
                createdBy = createdBy,
                members = members,
                pendingInvites = pendingInvites,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getExpenses(groupId: String): List<Expense> {
        val snapshot = expensesRef.child(groupId).get().await()
        return snapshot.children.mapNotNull { childSnapshot ->
            try {
                val id = childSnapshot.key ?: return@mapNotNull null
                val amount = childSnapshot.child("amount").getValue(Double::class.java) ?: 0.0
                val description = childSnapshot.child("description").getValue(String::class.java) ?: ""
                val category = childSnapshot.child("category").getValue(String::class.java) ?: ""
                val paidBy = childSnapshot.child("paidBy").getValue(String::class.java) ?: ""
                val splitBetween = childSnapshot.child("splitBetween").children.mapNotNull { it.getValue(String::class.java) }
                val createdAt = childSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                Expense(
                    id = id,
                    groupId = groupId,
                    amount = amount,
                    description = description,
                    category = category,
                    paidBy = paidBy,
                    splitBetween = splitBetween,
                    createdAt = createdAt
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun updateGroup(group: Group) {
        val groupData = hashMapOf(
            "id" to group.id,
            "name" to group.name,
            "createdBy" to group.createdBy,
            "members" to group.members,
            "pendingInvites" to group.pendingInvites,
            "createdAt" to group.createdAt
        )
        groupsRef.child(group.id).setValue(groupData).await()
    }

    override suspend fun deleteGroup(groupId: String) {
        groupsRef.child(groupId).removeValue().await()
        expensesRef.child(groupId).removeValue().await()
    }
}