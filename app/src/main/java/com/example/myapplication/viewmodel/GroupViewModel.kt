package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private val _availableMembers = MutableStateFlow<List<User>>(emptyList())
    val availableMembers: StateFlow<List<User>> = _availableMembers

    init {
        fetchUserGroups()
    }

    private fun fetchUserGroups() {
        auth.currentUser?.let { user ->
            db.collection("groups")
                .whereArrayContains("members", user.uid)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.documents?.mapNotNull {
                        it.toObject(Group::class.java)
                    }?.let { groups ->
                        _groups.value = groups
                    }
                }
        }
    }

    fun createGroup(name: String, members: List<String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            auth.currentUser?.let { user ->
                val group = Group(
                    name = name,
                    createdBy = user.uid,
                    members = listOf(user.uid) + members
                )
                db.collection("groups")
                    .add(group)
                    .addOnSuccessListener { onSuccess() }
            }
        }
    }
}