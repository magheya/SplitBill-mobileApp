package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GroupViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private val _availableMembers = MutableStateFlow<List<User>>(emptyList())
    val availableMembers: StateFlow<List<User>> = _availableMembers

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _balances = MutableStateFlow<Map<String, Double>>(emptyMap())
    val balances: StateFlow<Map<String, Double>> = _balances

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadGroups()
        loadAvailableMembers()
    }

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = db.collection("groups")
                    .document(groupId)
                    .get()
                    .await()

                if (document != null && document.exists()) {
                    _selectedGroup.value = document.toObject(Group::class.java)?.copy(id = document.id)
                    loadExpenses(groupId)
                    calculateBalances(groupId)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createGroup(name: String, members: List<String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.currentUser?.let { user ->
                    val newGroup = Group(
                        name = name,
                        createdBy = user.uid,
                        members = listOf(user.uid),
                        pendingInvites = members,
                        createdAt = System.currentTimeMillis()
                    )

                    db.collection("groups")
                        .add(newGroup)
                        .await()

                    loadGroups()
                    onSuccess()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addExpense(
        groupId: String,
        amount: Double,
        description: String,
        category: String,
        splitBetween: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.currentUser?.let { user ->
                    val expense = Expense(
                        groupId = groupId,
                        amount = amount,
                        description = description,
                        category = category,
                        paidBy = user.uid,
                        splitBetween = splitBetween,
                        createdAt = System.currentTimeMillis()
                    )

                    db.collection("groups")
                        .document(groupId)
                        .collection("expenses")
                        .add(expense)
                        .await()

                    loadExpenses(groupId)
                    calculateBalances(groupId)
                    onSuccess()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadExpenses(groupId: String) {
        db.collection("groups")
            .document(groupId)
            .collection("expenses")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { documents ->
                    _expenses.value = documents.map { doc ->
                        doc.toObject(Expense::class.java).copy(id = doc.id)
                    }
                    calculateBalances(groupId)
                }
            }
    }

    private fun calculateBalances(groupId: String) {
        viewModelScope.launch {
            val currentExpenses = expenses.value
            val balanceMap = mutableMapOf<String, Double>()

            currentExpenses.forEach { expense ->
                // Add the full amount to the person who paid
                balanceMap[expense.paidBy] = (balanceMap[expense.paidBy] ?: 0.0) + expense.amount

                // Subtract each person's share
                val perPersonShare = expense.amount / expense.splitBetween.size
                expense.splitBetween.forEach { personId ->
                    balanceMap[personId] = (balanceMap[personId] ?: 0.0) - perPersonShare
                }
            }

            _balances.value = balanceMap
        }
    }

    fun acceptInvite(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.currentUser?.let { user ->
                    val group = selectedGroup.value
                    if (group != null) {
                        val updatedGroup = group.copy(
                            members = group.members + user.uid,
                            pendingInvites = group.pendingInvites - user.uid
                        )

                        db.collection("groups")
                            .document(groupId)
                            .set(updatedGroup)
                            .await()

                        loadGroup(groupId)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadGroups() {
        auth.currentUser?.let { user ->
            db.collection("groups")
                .whereArrayContains("members", user.uid)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.let { documents ->
                        _groups.value = documents.map { doc ->
                            doc.toObject(Group::class.java).copy(id = doc.id)
                        }
                    }
                }
        }
    }

    private fun loadAvailableMembers() {
        db.collection("users")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { documents ->
                    _availableMembers.value = documents.map { doc ->
                        doc.toObject(User::class.java).copy(id = doc.id)
                    }
                }
            }
    }

    fun deleteExpense(groupId: String, expenseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("groups")
                    .document(groupId)
                    .collection("expenses")
                    .document(expenseId)
                    .delete()
                    .await()

                loadExpenses(groupId)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}