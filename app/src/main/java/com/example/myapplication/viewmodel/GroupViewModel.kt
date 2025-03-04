package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.Member
import com.example.myapplication.data.model.Balance
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

    private val _settlements = MutableStateFlow<List<Pair<Member, Member>>>(emptyList())
    val settlements: StateFlow<List<Pair<Member, Member>>> = _settlements

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        Log.d("GroupViewModel", "Auth state changed. User: ${firebaseAuth.currentUser?.uid}")
        firebaseAuth.currentUser?.let { user ->
            observeGroups(user.uid)
        } ?: run {
            _groups.value = emptyList()
            _selectedGroup.value = null
        }
    }

    init {
        Log.d("GroupViewModel", "Initializing GroupViewModel")
        auth.addAuthStateListener(authStateListener)
        auth.currentUser?.let { user ->
            observeGroups(user.uid)
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun observeGroups(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("GroupViewModel", "Starting to observe groups for user: $userId")
                groupRepository.observeGroups(userId)
                    .collect { groupsList ->
                        Log.d("GroupViewModel", "Received ${groupsList.size} groups")
                        _groups.value = groupsList.sortedByDescending { it.createdAt }
                    }
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error observing groups", e)
                _groups.value = emptyList()
            }
        }
    }

    fun createGroup(name: String, members: List<Member>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Log.e("GroupViewModel", "No authenticated user found")
                    return@launch
                }

                Log.d("GroupViewModel", "Creating group: $name for user: ${currentUser.uid}")

                // Create initial member list including the current user
                val allMembers = members.toMutableList()
                if (!members.any { it.id == currentUser.uid }) {
                    allMembers.add(
                        Member(
                            id = currentUser.uid,
                            name = currentUser.displayName ?: "User",
                        )
                    )
                }

                val groupId = groupRepository.createGroup(
                    name = name,
                    createdBy = currentUser.uid,
                    members = allMembers
                )

                Log.d("GroupViewModel", "Group created with ID: $groupId")

                // Force refresh groups
                observeGroups(currentUser.uid)
                onSuccess()
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error creating group", e)
            }
        }
    }

    fun selectGroup(groupId: String) {
        viewModelScope.launch {
            try {
                val group = _groups.value.find { it.id == groupId }
                Log.d("GroupViewModel", "Selecting group: ${group?.name}")
                _selectedGroup.value = group
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error selecting group", e)
            }
        }
    }

    // ... rest of your methods remain the same ...


    fun addExpense(groupId: String, expense: Expense) {
        viewModelScope.launch {
            try {
                groupRepository.addExpense(groupId, expense)
                // Re-select the group to refresh the data
                selectGroup(groupId)
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error adding expense", e)
            }
        }
    }

    fun addMember(groupId: String, member: Member) {
        viewModelScope.launch {
            try {
                groupRepository.addMember(groupId, member)
                // Re-select the group to refresh the data
                selectGroup(groupId)
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

    fun calculateMemberBalances(groupId: String): Map<String, Balance> {
        val currentGroup = _selectedGroup.value ?: return emptyMap()
        val balances = mutableMapOf<String, Balance>()

        // Initialize balances for all members
        currentGroup.members.keys.forEach { memberId ->
            balances[memberId] = Balance()
        }

        // Calculate balances as before
        currentGroup.expenses.forEach { (_, expense) ->
            val payer = expense.paidBy
            balances[payer] = balances[payer]?.let {
                it.copy(totalPaid = it.totalPaid + expense.amount)
            } ?: Balance(totalPaid = expense.amount)

            expense.splitAmounts.forEach { (participantId, splitAmount) ->
                balances[participantId] = balances[participantId]?.let {
                    it.copy(totalOwes = it.totalOwes + splitAmount)
                } ?: Balance(totalOwes = splitAmount)
            }
        }

        // Calculate final balances
        val finalBalances = balances.mapValues { (_, balance) ->
            balance.copy(netBalance = balance.totalPaid - balance.totalOwes)
        }

        // Calculate settlements
        calculateSettlements(finalBalances, currentGroup.members)

        return finalBalances
    }

    private fun calculateSettlements(
        balances: Map<String, Balance>,
        members: Map<String, Member>
    ) {
        val debtors = mutableListOf<Triple<String, Member, Double>>()
        val creditors = mutableListOf<Triple<String, Member, Double>>()

        // Separate members into debtors and creditors
        balances.forEach { (memberId, balance) ->
            val member = members[memberId] ?: return@forEach
            when {
                balance.netBalance < 0 -> debtors.add(Triple(memberId, member, -balance.netBalance))
                balance.netBalance > 0 -> creditors.add(Triple(memberId, member, balance.netBalance))
            }
        }

        // Sort by amount (descending)
        debtors.sortByDescending { it.third }
        creditors.sortByDescending { it.third }

        val settlements = mutableListOf<Pair<Member, Member>>()

        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val debtor = debtors[i]
            val creditor = creditors[j]

            val settlementAmount = minOf(debtor.third, creditor.third)
            if (settlementAmount > 0.01) { // Avoid tiny transactions
                // Update Member objects with new balances
                val updatedDebtor = debtor.second.copy(
                    owes = settlementAmount
                )
                val updatedCreditor = creditor.second.copy(
                    paid = settlementAmount
                )

                settlements.add(updatedDebtor to updatedCreditor)
            }

            when {
                debtor.third > creditor.third -> {
                    // Debtor still owes money
                    debtors[i] = debtor.copy(third = debtor.third - creditor.third)
                    j++
                }
                debtor.third < creditor.third -> {
                    // Creditor still needs to be paid
                    creditors[j] = creditor.copy(third = creditor.third - debtor.third)
                    i++
                }
                else -> {
                    // Both are settled
                    i++
                    j++
                }
            }
        }

        _settlements.value = settlements
    }
}