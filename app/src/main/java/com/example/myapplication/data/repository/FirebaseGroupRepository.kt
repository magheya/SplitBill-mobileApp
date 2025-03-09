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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for managing group-related data in Firebase.
 *
 * @property database The Firebase database instance.
 * @property auth The Firebase authentication instance.
 */
@Singleton
class FirebaseGroupRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) : GroupRepository {
    private val groupsRef = database.reference.child("groups")
    private val userGroupsRef = database.reference.child("user_groups")
    /**
     * Creates a new group in the Firebase database.
     *
     * @param name The name of the group.
     * @param createdBy The ID of the user who created the group.
     * @param members The list of members in the group.
     * @return The ID of the created group.
     * @throws Exception If an error occurs while creating the group.
     */
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
                expenses = emptyMap(),
                totalAmount = 0.0,
                createdAt = System.currentTimeMillis()
            )

            // Create group in groups node
            Log.d("FirebaseGroupRepository", "Setting group data for $groupId")
            groupsRef.child(groupId).setValue(group).await()

            // Add group reference to creator's user_groups node
            Log.d("FirebaseGroupRepository", "Adding group reference for creator: $createdBy")
            userGroupsRef.child(createdBy).child(groupId).setValue(true).await()

            // Add group reference to all members' user_groups node
            members.forEach { member ->
                Log.d("FirebaseGroupRepository", "Adding group reference for member: ${member.id}")
                userGroupsRef.child(member.id).child(groupId).setValue(true).await()
            }

            Log.d("FirebaseGroupRepository", "Group created successfully")
            return groupId
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error creating group", e)
            throw e
        }
    }

    /**
     * Observes the groups of a specific user.
     *
     * @param userId The ID of the user.
     * @return A flow of the list of groups.
     */
    override fun observeGroups(userId: String): Flow<List<Group>> = callbackFlow {
        var groupsListener: ValueEventListener? = null

        Log.d("FirebaseGroupRepository", "Starting observeGroups for userId: $userId")

        val userGroupsListener = object : ValueEventListener {
            override fun onDataChange(userGroupsSnapshot: DataSnapshot) {
                try {
                    Log.d("FirebaseGroupRepository", "UserGroups snapshot received: ${userGroupsSnapshot.exists()}")

                    // Get all group IDs for the user
                    val groupIds = userGroupsSnapshot.children.mapNotNull { it.key }

                    Log.d("FirebaseGroupRepository", "Found group IDs: $groupIds")

                    if (groupIds.isEmpty()) {
                        Log.d("FirebaseGroupRepository", "No groups found for user")
                        trySend(emptyList())
                        return
                    }

                    // Remove previous listener if exists
                    groupsListener?.let {
                        Log.d("FirebaseGroupRepository", "Removing previous groups listener")
                        groupsRef.removeEventListener(it)
                    }

                    // Create a new listener for the groups
                    groupsListener = object : ValueEventListener {
                        override fun onDataChange(groupsSnapshot: DataSnapshot) {
                            Log.d("FirebaseGroupRepository", "Groups snapshot received")
                            val groups = groupIds.mapNotNull { groupId ->
                                groupsSnapshot.child(groupId).getValue(Group::class.java)?.copy(
                                    id = groupId
                                ).also { group ->
                                    Log.d("FirebaseGroupRepository", "Parsed group: ${group?.name}")
                                }
                            }
                            Log.d("FirebaseGroupRepository", "Sending ${groups.size} groups")
                            trySend(groups)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("FirebaseGroupRepository", "Error observing groups", error.toException())
                            trySend(emptyList())
                        }
                    }

                    // Add listener for groups
                    groupsListener?.let {
                        Log.d("FirebaseGroupRepository", "Adding new groups listener")
                        groupsRef.addValueEventListener(it)
                    }

                } catch (e: Exception) {
                    Log.e("FirebaseGroupRepository", "Error parsing user groups", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseGroupRepository", "Error observing user groups", error.toException())
                trySend(emptyList())
            }
        }

        // Add listener for user's groups
        Log.d("FirebaseGroupRepository", "Adding user groups listener")
        userGroupsRef.child(userId).addValueEventListener(userGroupsListener)

        // Remove all listeners when flow is cancelled
        awaitClose {
            Log.d("FirebaseGroupRepository", "Removing all listeners")
            userGroupsRef.child(userId).removeEventListener(userGroupsListener)
            groupsListener?.let { groupsRef.removeEventListener(it) }
        }
    }

    /**
     * Observes a specific group.
     *
     * @param groupId The ID of the group.
     * @return A flow of the group.
     */
    override fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val group = snapshot.getValue(Group::class.java)?.copy(
                        id = snapshot.key ?: return
                    )
                    trySend(group)
                } catch (e: Exception) {
                    Log.e("FirebaseGroupRepository", "Error parsing group", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseGroupRepository", "Error observing group", error.toException())
                trySend(null)
            }
        }

        groupsRef.child(groupId).addValueEventListener(listener)
        awaitClose { groupsRef.child(groupId).removeEventListener(listener) }
    }

    /**
     * Observes the expenses of a specific group.
     *
     * @param groupId The ID of the group.
     * @return A flow of the list of expenses.
     */
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
                }.sortedByDescending { it.createdAt } // Sort by date
                trySend(expenses)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseGroupRepository", "Error observing expenses", error.toException())
                trySend(emptyList())
            }
        }

        groupsRef.child(groupId).child("expenses").addValueEventListener(listener)
        awaitClose {
            groupsRef.child(groupId).child("expenses").removeEventListener(listener)
        }
    }

    /**
     * Retrieves the expenses of a specific group.
     *
     * @param groupId The ID of the group.
     * @return The list of expenses.
     * @throws Exception If an error occurs while retrieving the expenses.
     */
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
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error getting expenses", e)
            emptyList()
        }
    }

    /**
     * Adds an expense to a specific group.
     *
     * @param groupId The ID of the group.
     * @param expense The expense to add.
     * @throws Exception If an error occurs while adding the expense.
     */
    override suspend fun addExpense(groupId: String, expense: Expense) {
        try {
            val expenseId = groupsRef.child(groupId)
                .child("expenses")
                .push()
                .key ?: throw IllegalStateException("Couldn't get push key")

            val expenseWithId = expense.copy(
                id = expenseId,
                createdAt = System.currentTimeMillis()
            )

            val updates = hashMapOf<String, Any>(
                "/expenses/$expenseId" to expenseWithId,
                "/totalAmount" to ServerValue.increment(expense.amount)
            )

            groupsRef.child(groupId).updateChildren(updates).await()
            Log.d("FirebaseGroupRepository", "Expense added successfully")
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error adding expense", e)
            throw e
        }
    }

    /**
     * Adds a member to a specific group.
     *
     * @param groupId The ID of the group.
     * @param member The member to be added.
     * @throws Exception If an error occurs while adding the member.
     */
    override suspend fun addMember(groupId: String, member: Member) {
        try {
            // Add member to group's members
            groupsRef.child(groupId)
                .child("members")
                .child(member.id)
                .setValue(member)
                .await()

            // Add group reference to member's user_groups
            userGroupsRef.child(member.id)
                .child(groupId)
                .setValue(true)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error adding member", e)
            throw e
        }
    }

    /**
     * Removes a member from a specific group.
     *
     * @param groupId The ID of the group.
     * @param memberId The ID of the member to be removed.
     * @throws Exception If an error occurs while removing the member.
     */
    override suspend fun removeMember(groupId: String, memberId: String) {
        try {
            // Remove member from group's members
            groupsRef.child(groupId)
                .child("members")
                .child(memberId)
                .removeValue()
                .await()

            // Remove group reference from member's user_groups
            userGroupsRef.child(memberId)
                .child(groupId)
                .removeValue()
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error removing member", e)
            throw e
        }
    }

    /**
     * Retrieves a specific group.
     *
     * @param groupId The ID of the group.
     * @return The group, or null if not found.
     * @throws Exception If an error occurs while retrieving the group.
     */
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

    /**
     * Calculates the balances for a specific group.
     *
     * @param groupId The ID of the group.
     * @return A map of user IDs to their balances.
     * @throws Exception If an error occurs while calculating the balances.
     */
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

    /**
     * Updates a specific group.
     *
     * @param group The group to be updated.
     * @throws Exception If an error occurs while updating the group.
     */
    override suspend fun updateGroup(group: Group) {
        try {
            groupsRef.child(group.id).setValue(group).await()
            Log.d("FirebaseGroupRepository", "Group updated successfully")
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error updating group", e)
            throw e
        }
    }

    /**
     * Deletes a specific group.
     *
     * @param groupId The ID of the group.
     * @throws Exception If an error occurs while deleting the group.
     */
    override suspend fun deleteGroup(groupId: String) {
        try {
            // Get the group first to get all members
            val group = getGroup(groupId) ?: throw IllegalStateException("Group not found")

            // Remove group reference from all members' user_groups
            group.members.keys.forEach { memberId ->
                userGroupsRef.child(memberId)
                    .child(groupId)
                    .removeValue()
                    .await()
            }

            // Remove group reference from creator's user_groups
            userGroupsRef.child(group.createdBy)
                .child(groupId)
                .removeValue()
                .await()

            // Delete the group
            groupsRef.child(groupId)
                .removeValue()
                .await()

            Log.d("FirebaseGroupRepository", "Group deleted successfully")
        } catch (e: Exception) {
            Log.e("FirebaseGroupRepository", "Error deleting group", e)
            throw e
        }
    }
}