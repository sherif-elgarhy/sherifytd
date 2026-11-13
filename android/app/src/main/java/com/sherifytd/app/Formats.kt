package com.sherifytd.app

data class FormatOption(val label: String, val value: String)

val FORMATS = listOf(
    FormatOption("Best Available",  "bv*+ba/b"),
    FormatOption("Music (M4A)",     "bestaudio[ext=m4a]/bestaudio"),
    FormatOption("Music (MP3)",     "mp3"),
    FormatOption("Video 360p",      "bv*[height<=360]+ba/b[height<=360]/b"),
    FormatOption("Video 480p",      "bv*[height<=480]+ba/b[height<=480]/b"),
    FormatOption("Video 720p",      "bv*[height<=720]+ba/b[height<=720]/b"),
    FormatOption("Video 1080p",     "bv*[height<=1080]+ba/b[height<=1080]/b"),
    FormatOption("Video 2160p",     "bv*[height<=2160]+ba/b[height<=2160]/b"),
)
