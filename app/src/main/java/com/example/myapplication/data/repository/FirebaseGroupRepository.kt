package com.example.myapplication.data.repository

import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.myapplication.data.model.*
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseGroupRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) : GroupRepository {
    private val groupsRef = database.reference.child("groups")

    override suspend fun createGroup(name: String, createdBy: String, members: List<Member>): String {
        try {
            val groupId = groupsRef.push().key ?: throw IllegalStateException("Couldn't get push key")
            Log.d("FirebaseGroupRepository", "Creating group with ID: $groupId")

            val membersMap = members.associateBy { it.id }

            val group = Group(
                id = groupId,
                name = name,
                createdBy = createdBy,
                members = membersMap,
                expenses = emptyList(),
                totalAmount = 0.0,
                createdAt = System.currentTimeMillis()
            )

            groupsRef.child(groupId).setValue(group).await()
            Log.d("FirebaseGroupRepository", "Group created successfully")
            return groupId
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error creating group", e)
            throw e
        }
    }

    override fun observeGroups(userId: String): Flow<List<Group>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val groups = snapshot.children.mapNotNull { groupSnapshot ->
                        groupSnapshot.getValue(Group::class.java)?.copy(
                            id = groupSnapshot.key ?: return@mapNotNull null
                        )
                    }
                    trySend(groups)
                } catch (e: Exception) {
                    Log.e("FirebaseGroupRepository", "Error parsing groups", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseGroupRepository", "Error observing groups", error.toException())
                trySend(emptyList())
            }
        }

        groupsRef.addValueEventListener(listener)
        awaitClose { groupsRef.removeEventListener(listener) }
    }

    override suspend fun addExpense(groupId: String, expense: Expense) {
        try {
            val expenseId = groupsRef.child(groupId)
                .child("expenses")
                .push()
                .key ?: throw IllegalStateException("Couldn't get push key")

            val expenseWithId = expense.copy(id = expenseId)

            val updates = hashMapOf<String, Any>(
                "expenses/$expenseId" to expenseWithId,
                "totalAmount" to ServerValue.increment(expense.amount)
            )

            groupsRef.child(groupId).updateChildren(updates).await()
            Log.d("FirebaseGroupRepository", "Expense added successfully")
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error adding expense", e)
            throw e
        }
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
                        Log.e("FirebaseGroupRepository", "Error parsing expense", e)
                        null
                    }
                }
                trySend(expenses)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseGroupRepository", "Error observing expenses", error.toException())
                close(error.toException())
            }
        }
        groupsRef.child(groupId).child("expenses").addValueEventListener(listener)
        awaitClose {
            groupsRef.child(groupId).child("expenses").removeEventListener(listener)
        }
    }

    override suspend fun addMember(groupId: String, member: Member) {
        try {
            groupsRef.child(groupId)
                .child("members")
                .child(member.id)
                .setValue(member)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error adding member", e)
            throw e
        }
    }

    override suspend fun removeMember(groupId: String, memberId: String) {
        try {
            groupsRef.child(groupId)
                .child("members")
                .child(memberId)
                .removeValue()
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error removing member", e)
            throw e
        }
    }

    override suspend fun getGroup(groupId: String): Group? {
        return try {
            val snapshot = groupsRef.child(groupId).get().await()
            snapshot.getValue(Group::class.java)?.copy(
                id = snapshot.key ?: return null
            )
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error getting group", e)
            null
        }
    }

    override suspend fun getExpenses(groupId: String): List<Expense> {
        return try {
            val snapshot = groupsRef.child(groupId).child("expenses").get().await()
            snapshot.children.mapNotNull { childSnapshot ->
                try {
                    childSnapshot.getValue(Expense::class.java)?.copy(
                        id = childSnapshot.key ?: return@mapNotNull null
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseGroupRepository", "Error parsing expense", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error getting expenses", e)
            emptyList()
        }
    }

    override suspend fun calculateBalances(groupId: String): Map<String, Double> {
        return try {
            val expenses = getExpenses(groupId)
            val balances = mutableMapOf<String, Double>()

            expenses.forEach { expense ->
                balances[expense.paidBy] = (balances[expense.paidBy] ?: 0.0) + expense.amount
                expense.splitAmounts.forEach { (participantId, amount) ->
                    balances[participantId] = (balances[participantId] ?: 0.0) - amount
                }
            }
            balances
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error calculating balances", e)
            emptyMap()
        }
    }

    override suspend fun updateGroup(group: Group) {
        try {
            groupsRef.child(group.id).setValue(group).await()
            Log.d("FirebaseGroupRepository", "Group updated successfully")
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error updating group", e)
            throw e
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        try {
            groupsRef.child(groupId).removeValue().await()
            Log.d("FirebaseGroupRepository", "Group deleted successfully")
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error deleting group", e)
            throw e
        }
    }
}