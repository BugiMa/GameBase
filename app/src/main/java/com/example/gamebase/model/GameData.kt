package com.example.gamebase.model

data class GameData(
        val key:         String = "",
        val title:       String = "",
        var genre:       String? = "",
        var releaseYear: String? = "",
        val studio:      String? = "",
        val description: String? = "",
        val note:        String? = "",
        var imageUrl:    String? = "",
        var favorite:    Boolean = false,
        var played:      Boolean = false
)

