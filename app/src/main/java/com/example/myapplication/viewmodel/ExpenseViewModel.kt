package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ExpenseViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    fun fetchExpenses(groupId: String) {
        db.collection("expenses")
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.mapNotNull {
                    it.toObject(Expense::class.java)
                }?.let { expenses ->
                    _expenses.value = expenses
                }
            }
    }
}