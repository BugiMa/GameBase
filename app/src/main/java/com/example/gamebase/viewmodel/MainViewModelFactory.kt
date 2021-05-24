package com.example.gamebase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gamebase.repository.GameRepository

class MainViewModelFactory (
    private val gameRepository: GameRepository,
): ViewModelProvider.Factory {
    override fun <T: ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(gameRepository) as T
    }

}