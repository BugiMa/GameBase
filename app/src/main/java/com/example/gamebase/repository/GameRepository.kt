package com.example.gamebase.repository

import com.example.gamebase.api.RetrofitInstance
import com.example.gamebase.model.GameDataAPI
import retrofit2.Response

class GameRepository {

    suspend fun getGame(name: String, apiKey: String): Response<GameDataAPI> {
        return RetrofitInstance.api.getGame(name, apiKey)
    }
}