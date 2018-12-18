package com.e_rachit.e_rachitcloudadsplayer.models

import com.google.gson.annotations.SerializedName

/**
 * Created by rohitranjan on 08/10/17.
 */

enum class Orientation {
    @SerializedName("portrait")
    PORTRAIT,
    @SerializedName("landscape")
    LANDSCAPE
}

data class Playlist(val schedule: Schedule, val playlistItems: ArrayList<PlaylistItem>, val banner: Banner?, val orientation: Orientation)

data class PlaylistItem(val url: String, val content_type: String, val title: String, val uploaded_date: String, val img_duration: Int, var offlineUrl: String, val content_length: Long, val filename: String, val volume: Float = 0.0f)

data class Banner(val name: String, val logo: String, val font_family: String, val font_size: String, val font_color: String, val background_color: String, val duration: String, val direction: String, val top_margin: Int)

data class Schedule(val start_date: String, val end_date: String, val start_time: String, val end_time: String, val playlist_id: Int, val schedule_id: Int, val days: List<String>)
data class SchedulePlaylistResponse(val schedule: List<Schedule>?)
