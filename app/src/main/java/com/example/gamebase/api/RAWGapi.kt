package com.example.gamebase.api

import com.example.gamebase.model.GameDataAPI
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RAWGapi {

    @GET("api/games/{gameSlug}")
    suspend fun getGame(
        @Path("gameSlug") name: String,
        @Query("key") apiKey: String
    ): Response<GameDataAPI>
}