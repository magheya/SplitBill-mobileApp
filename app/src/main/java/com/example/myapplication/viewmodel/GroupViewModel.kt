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
    private val auth: FirebaseAuth,
    private val database: DatabaseReference
) : ViewModel() {
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup

    private val _settlements = MutableStateFlow<List<Pair<Member, Member>>>(emptyList())
    val settlements: StateFlow<List<Pair<Member, Member>>> = _settlements

    private val _expenses = MutableStateFlow<Map<String, Expense>>(emptyMap())
    val expenses: StateFlow<Map<String, Expense>> = _expenses

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        Log.d("GroupViewModel", "Auth state changed. User: ${firebaseAuth.currentUser?.uid}")
        firebaseAuth.currentUser?.let { user ->
            observeGroups(user.uid)
        } ?: run {
            _groups.value = emptyList()
            _selectedGroup.value = null
        }
    }

    private fun listenToGroupChanges(groupId: String) {
        val groupRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId)

        groupRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(Group::class.java)?.let { group ->
                    _selectedGroup.value = group
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error listening to group changes", error.toException())
            }
        })

        // Listen for changes in expenses
        groupRef.child("expenses").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedExpenses = snapshot.children.mapNotNull { it.getValue(Expense::class.java) }
                _selectedGroup.value?.let { currentGroup ->
                    _selectedGroup.value = currentGroup.copy(expenses = updatedExpenses.associateBy { it.id })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error listening to expenses changes", error.toException())
            }
        })

        // Listen for changes in members
        groupRef.child("members").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedMembers = snapshot.children.mapNotNull { it.getValue(Member::class.java) }
                _selectedGroup.value?.let { currentGroup ->
                    _selectedGroup.value = currentGroup.copy(members = updatedMembers.associateBy { it.id })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error listening to members changes", error.toException())
            }
        })
    }


    private val _totalDebts = MutableStateFlow<Double>(0.0)
    val totalDebts: StateFlow<Double> = _totalDebts

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
                        calculateGlobalTotalDebts(groupsList) // Calculate debts for all groups
                    }
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error observing groups", e)
                _groups.value = emptyList()
                _totalDebts.value = 0.0
            }
        }
    }

    private fun calculateGlobalTotalDebts(groups: List<Group>) {
        val totalDebts = groups.sumOf { group ->
            val balances = mutableMapOf<String, Balance>()

            // Initialize balances for all members
            group.members.keys.forEach { memberId ->
                balances[memberId] = Balance()
            }

            // Calculate balances for this group
            group.expenses.forEach { (_, expense) ->
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

            // Sum up negative balances (debts) for this group
            balances.values
                .map { it.copy(netBalance = it.totalPaid - it.totalOwes) }
                .filter { it.netBalance < 0 }
                .sumOf { -it.netBalance }
        }

        _totalDebts.value = totalDebts
        Log.d("GroupViewModel", "Total debts calculated: $totalDebts")
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

                // Create a member object for the current user
                val currentUserMember = Member(
                    id = currentUser.uid,
                    name = currentUser.displayName ?: "User"
                )

                // Create a new list with the current user and other members
                val membersList = buildList {
                    add(currentUserMember)  // Add current user first
                    addAll(members.filter { it.id != currentUser.uid })  // Add other members, avoiding duplicates
                }

                val groupId = groupRepository.createGroup(
                    name = name,
                    createdBy = currentUser.uid,
                    members = membersList
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


    fun addMember(groupId: String, member: Member) {
        val groupRef = database.child("groups").child(groupId).child("members").child(member.id)
        groupRef.setValue(member).addOnSuccessListener {
            Log.d("Firebase", "Member successfully added!")

            // 🔄 Force refresh
            listenToGroupChanges(groupId)

        }.addOnFailureListener { e ->
            Log.e("Firebase", "Error adding member", e)
        }
    }


    fun addExpense(groupId: String, expense: Expense) {
        val groupRef = database.child("groups").child(groupId)
        val expensesRef = groupRef.child("expenses").child(expense.id)

        groupRef.child("totalAmount").get().addOnSuccessListener { snapshot ->
            val currentTotalAmount = snapshot.getValue(Double::class.java) ?: 0.0
            val newTotalAmount = currentTotalAmount + expense.amount

            val updates = mapOf(
                "expenses/${expense.id}" to expense,
                "totalAmount" to newTotalAmount
            )

            groupRef.updateChildren(updates).addOnSuccessListener {
                Log.d("Firebase", "Expense successfully added!")

                // 🔄 Force refresh
                listenToGroupChanges(groupId)

            }.addOnFailureListener { e ->
                Log.w("Firebase", "Error adding expense", e)
            }
        }.addOnFailureListener { e ->
            Log.w("Firebase", "Error retrieving totalAmount", e)
        }
    }

    fun updateExpense(groupId: String, expense: Expense) {
        viewModelScope.launch {
            try {
                val groupRef = database.child("groups").child(groupId)
                val expensesRef = groupRef.child("expenses").child(expense.id)

                // Get the old expense to calculate total amount adjustment
                expensesRef.get().addOnSuccessListener { snapshot ->
                    val oldExpense = snapshot.getValue(Expense::class.java)
                    val amountDifference = expense.amount - (oldExpense?.amount ?: 0.0)

                    // Update total amount and expense
                    groupRef.child("totalAmount").get().addOnSuccessListener { totalSnapshot ->
                        val currentTotalAmount = totalSnapshot.getValue(Double::class.java) ?: 0.0
                        val updates = mapOf(
                            "expenses/${expense.id}" to expense,
                            "totalAmount" to (currentTotalAmount + amountDifference)
                        )

                        groupRef.updateChildren(updates).addOnSuccessListener {
                            Log.d("Firebase", "Expense successfully updated!")

                            fetchExpenses(groupId)

                        }.addOnFailureListener { e ->
                            Log.e("Firebase", "Error updating expense", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error updating expense", e)
            }
        }
    }

    fun fetchExpenses(groupId: String) {
        val expensesRef = database.child("groups").child(groupId).child("expenses")

        expensesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val expenseMap = snapshot.children.associate {
                    it.key!! to it.getValue(Expense::class.java)!!
                }
                _expenses.value = expenseMap  // ✅ Updates StateFlow correctly
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to fetch expenses", error.toException())
            }
        })
    }


    fun removeMember(groupId: String, memberId: String) {
        val groupRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId)
        val memberRef = groupRef.child("members").child(memberId)

        memberRef.removeValue().addOnSuccessListener {
            Log.d("Firebase", "Member successfully removed!")
        }.addOnFailureListener { e ->
            Log.e("Firebase", "Error removing member", e)
        }
    }

    fun deleteGroup(groupId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val groupRef = database.child("groups").child(groupId)

        groupRef.removeValue()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
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

    fun calculatePersonalBalances(userId: String): Pair<Double, Double> {
        val allGroups = _groups.value // Use all groups instead of only the selected group
        var totalExpenses = 0.0
        var totalDebts = 0.0

        for (group in allGroups) {
            // Calculate total expenses paid by user
            group.expenses.values
                .filter { it.paidBy == userId }
                .forEach { expense ->
                    totalExpenses += expense.amount
                }

            // Calculate debts (what the user owes to others)
            group.expenses.values
                .filter { it.paidBy != userId } // Expenses not paid by the user
                .forEach { expense ->
                    // Get the split amount for the current user
                    val userSplitAmount = expense.splitAmounts[userId] ?: 0.0
                    totalDebts += userSplitAmount
                }
        }

        return Pair(totalExpenses, totalDebts)
    }

    fun calculateAmountIOwe(userId: String): Double {
        return _settlements.value
            .filter { (debtor, _) -> debtor.id == userId }
            .sumOf { (_, creditor) -> creditor.paid }
    }

}