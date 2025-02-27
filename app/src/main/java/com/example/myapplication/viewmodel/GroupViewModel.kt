package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.myapplication.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup

    init {
        observeGroups()
    }

    private fun observeGroups() {
        viewModelScope.launch {
            auth.currentUser?.uid?.let { userId ->
                groupRepository.observeGroups(userId).collect { groupsList ->
                    _groups.value = groupsList
                }
            }
        }
    }

    fun selectGroup(groupId: String) {
        _selectedGroup.value = _groups.value.find { it.id == groupId }
    }

    fun createGroup(name: String, members: List<Member>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val groupId = groupRepository.createGroup(
                    name = name,
                    createdBy = currentUser.uid,
                    members = members
                )
                onSuccess()
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error creating group", e)
            }
        }
    }

    fun addExpense(groupId: String, expense: Expense) {
        viewModelScope.launch {
            try {
                groupRepository.addExpense(groupId, expense)
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error adding expense", e)
            }
        }
    }

    fun addMember(groupId: String, member: Member) {
        viewModelScope.launch {
            try {
                groupRepository.addMember(groupId, member)
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error adding member", e)
            }
        }
    }

    fun removeMember(groupId: String, memberId: String) {
        viewModelScope.launch {
            try {
                groupRepository.removeMember(groupId, memberId)
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error removing member", e)
            }
        }
    }

    fun deleteGroup(groupId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                groupRepository.deleteGroup(groupId)
                onSuccess()
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error deleting group", e)
            }
        }
    }

    fun calculateBalances(groupId: String): Map<String, Double> {
        val currentGroup = _selectedGroup.value ?: return emptyMap()
        val balances = mutableMapOf<String, Double>()

        // Access expenses as Map entries
        currentGroup.expenses.forEach { (_, expense) ->
            // Add the full amount to the payer's balance
            balances[expense.paidBy] = (balances[expense.paidBy] ?: 0.0) + expense.amount

            // Subtract each participant's share
            expense.splitAmounts.forEach { (participantId, splitAmount) ->
                balances[participantId] = (balances[participantId] ?: 0.0) - splitAmount
            }
        }

        return balances
    }
}