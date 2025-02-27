package com.example.myapplication.data.repository

import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.myapplication.data.model.*

class FirebaseGroupRepository : GroupRepository {
    private val database = FirebaseDatabase.getInstance()
    private val groupsRef = database.getReference("groups")

    override suspend fun createGroup(name: String, createdBy: String, members: List<Member>): String {
        val groupId = groupsRef.push().key ?: throw IllegalStateException("Couldn't get push key")

        val group = Group(
            id = groupId,
            name = name,
            createdBy = createdBy,
            members = members,
            expenses = emptyList(),
            totalAmount = 0.0,
            createdAt = System.currentTimeMillis()
        )

        groupsRef.child(groupId).setValue(group).await()
        return groupId
    }

    override fun observeGroups(userId: String): Flow<List<Group>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = snapshot.children.mapNotNull { childSnapshot ->
                    try {
                        childSnapshot.getValue(Group::class.java)?.copy(
                            id = childSnapshot.key ?: return@mapNotNull null
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.filter { group ->
                    group.members.any { it.id == userId }
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

    override suspend fun addExpense(groupId: String, expense: Expense) {
        val expenseId = groupsRef.child(groupId)
            .child("expenses")
            .push()
            .key ?: throw IllegalStateException("Couldn't get push key")

        val expenseWithId = expense.copy(id = expenseId)

        // Update the expense in Firebase
        groupsRef.child(groupId)
            .child("expenses")
            .child(expenseId)
            .setValue(expenseWithId)
            .await()

        // Update group total amount
        val group = getGroup(groupId) ?: return
        val updatedGroup = group.copy(
            expenses = group.expenses + expenseWithId,
            totalAmount = group.totalAmount + expense.amount
        )
        updateGroup(updatedGroup)
    }

    override fun observeExpenses(groupId: String): Flow<List<Expense>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val expenses = snapshot.children.mapNotNull { childSnapshot ->
                    try {
                        childSnapshot.getValue(Expense::class.java)?.copy(
                            id = childSnapshot.key ?: return@mapNotNull null
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
        groupsRef.child(groupId).child("expenses").addValueEventListener(listener)
        awaitClose {
            groupsRef.child(groupId).child("expenses").removeEventListener(listener)
        }
    }

    override suspend fun calculateBalances(groupId: String): Map<String, Double> {
        val expenses = getExpenses(groupId)
        val balances = mutableMapOf<String, Double>()

        expenses.forEach { expense ->
            // Add the full amount to the payer's balance
            balances[expense.paidBy] = (balances[expense.paidBy] ?: 0.0) + expense.amount

            // Subtract each participant's share
            expense.splitAmounts.forEach { (participantId, amount) ->
                balances[participantId] = (balances[participantId] ?: 0.0) - amount
            }
        }

        return balances
    }

    override suspend fun getGroup(groupId: String): Group? {
        val snapshot = groupsRef.child(groupId).get().await()
        return try {
            snapshot.getValue(Group::class.java)?.copy(
                id = snapshot.key ?: return null
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getExpenses(groupId: String): List<Expense> {
        val snapshot = groupsRef.child(groupId).child("expenses").get().await()
        return snapshot.children.mapNotNull { childSnapshot ->
            try {
                childSnapshot.getValue(Expense::class.java)?.copy(
                    id = childSnapshot.key ?: return@mapNotNull null
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun updateGroup(group: Group) {
        groupsRef.child(group.id).setValue(group).await()
    }

    override suspend fun deleteGroup(groupId: String) {
        groupsRef.child(groupId).removeValue().await()
    }
}