package com.example.gamebase.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamebase.model.GameDataAPI
import com.example.gamebase.repository.GameRepository
import kotlinx.coroutines.launch
import retrofit2.Response

class MainViewModel( private val gameRepository: GameRepository) : ViewModel() {

    var listMode = false
    var isPlayed = false
    var isFavorite = false

    val gameResponse: MutableLiveData<Response<GameDataAPI>> = MutableLiveData()

    fun getGame(name: String){
        viewModelScope.launch {
            val response = gameRepository.getGame(name, "4acd679dfcf14f2d80647d20f8cbc9c9")
            gameResponse.value = response
        }
    }
}