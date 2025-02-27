package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class GroupViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : ViewModel() {
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup

    init {
        loadGroups()
    }

    private fun loadGroups() {
        database.reference.child("groups")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupsList = snapshot.children.mapNotNull {
                        it.getValue(Group::class.java)
                    }
                    _groups.value = groupsList
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    fun selectGroup(groupId: String) {
        _selectedGroup.value = _groups.value.find { it.id == groupId }
    }

    fun createGroup(name: String, members: List<Member>, onSuccess: () -> Unit) {
        val currentUser = auth.currentUser ?: return
        val newGroup = Group(
            id = database.reference.child("groups").push().key ?: return,
            name = name,
            createdBy = currentUser.uid,
            members = members,
            expenses = emptyList(),
            totalAmount = 0.0,
            createdAt = System.currentTimeMillis()
        )

        database.reference.child("groups").child(newGroup.id).setValue(newGroup)
            .addOnSuccessListener { onSuccess() }
    }

    fun addExpense(groupId: String, expense: Expense) {
        viewModelScope.launch {
            val groupRef = database.reference.child("groups").child(groupId)

            // Create new expense with ID
            val expenseId = groupRef.child("expenses").push().key ?: return@launch
            val expenseWithId = expense.copy(id = expenseId)

            // Update expenses list
            groupRef.child("expenses").child(expenseId).setValue(expenseWithId)

            // Update group total amount
            val currentGroup = _selectedGroup.value ?: return@launch
            val updatedGroup = currentGroup.copy(
                expenses = currentGroup.expenses + expenseWithId,
                totalAmount = currentGroup.totalAmount + expense.amount
            )
            groupRef.setValue(updatedGroup)
        }
    }

    fun addMember(groupId: String, member: Member) {
        viewModelScope.launch {
            val currentGroup = _selectedGroup.value ?: return@launch
            val updatedMembers = currentGroup.members + member

            database.reference.child("groups")
                .child(groupId)
                .child("members")
                .setValue(updatedMembers)
        }
    }

    fun removeMember(groupId: String, memberId: String) {
        viewModelScope.launch {
            val currentGroup = _selectedGroup.value ?: return@launch
            val updatedMembers = currentGroup.members.filter { it.id != memberId }

            database.reference.child("groups")
                .child(groupId)
                .child("members")
                .setValue(updatedMembers)
        }
    }

    fun deleteGroup(groupId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            database.reference.child("groups")
                .child(groupId)
                .removeValue()
                .addOnSuccessListener { onSuccess() }
        }
    }

    fun calculateBalances(groupId: String): Map<String, Double> {
        val currentGroup = _selectedGroup.value ?: return emptyMap()
        val balances = mutableMapOf<String, Double>()

        currentGroup.expenses.forEach { expense ->
            // Add the full amount to the payer's balance
            balances[expense.paidBy] = (balances[expense.paidBy] ?: 0.0) + expense.amount

            // Subtract each participant's share
            expense.splitAmounts.forEach { (participantId, amount) ->
                balances[participantId] = (balances[participantId] ?: 0.0) - amount
            }
        }

        return balances
    }
}