package com.htthinhus.gpstracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UserViewModel: ViewModel() {
    private val auth = Firebase.auth

    private val _loginState = MutableLiveData<Boolean>().apply {
        if (auth.currentUser != null) {
            value = true
        } else {
            value = false
        }
    }
    val loginState: LiveData<Boolean> = _loginState

    fun login(email: String, password: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                result.value = it.isSuccessful
                _loginState.value = it.isSuccessful
            }
        return result
    }

    fun logout() {
        auth.signOut()
        _loginState.value = false
    }
}