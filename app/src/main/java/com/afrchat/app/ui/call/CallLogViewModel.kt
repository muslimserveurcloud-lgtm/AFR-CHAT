package com.afrchat.app.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.CallSession
import com.afrchat.app.data.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CallLogViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    val currentUid: String get() = authRepository.currentUserId.orEmpty()

    private val _calls = MutableStateFlow<List<CallSession>>(emptyList())
    val calls: StateFlow<List<CallSession>> = _calls.asStateFlow()

    fun load() {
        val uid = currentUid
        if (uid.isEmpty()) return
        viewModelScope.launch {
            val asCaller = firestore.collection("calls").whereEqualTo("callerUid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(30).get().await().toObjects(CallSession::class.java)
            val asCallee = firestore.collection("calls").whereEqualTo("calleeUid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(30).get().await().toObjects(CallSession::class.java)
            _calls.value = (asCaller + asCallee).sortedByDescending { it.createdAt }
        }
    }
}
