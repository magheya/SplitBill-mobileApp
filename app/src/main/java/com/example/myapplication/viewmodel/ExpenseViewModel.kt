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

    init {
        listenForExpenses()
    }

    private fun listenForExpenses() {
        db.collection("expenses")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val updatedExpenses = snapshot.documents.mapNotNull { it.toObject(Expense::class.java) }
                _expenses.value = updatedExpenses  // Triggers UI recomposition
            }
    }
}
