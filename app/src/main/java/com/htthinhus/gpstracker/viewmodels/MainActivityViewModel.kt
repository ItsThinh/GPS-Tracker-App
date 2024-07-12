package com.htthinhus.gpstracker.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel: ViewModel() {
    private val _isBottomNavVisible = MutableLiveData<Boolean>()
    val isBottomNavVisible = _isBottomNavVisible

    fun hideBottomNav() {
        _isBottomNavVisible.value = false
    }

    fun showBottomNav() {
        _isBottomNavVisible.value = true
    }
}