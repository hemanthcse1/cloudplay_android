package com.e_rachit.e_rachitcloudadsplayer.services

import com.e_rachit.e_rachitcloudadsplayer.models.DeviceSettingLockState
import com.e_rachit.e_rachitcloudadsplayer.models.OneSignalDeviceRegistration
import com.e_rachit.e_rachitcloudadsplayer.models.Playlist
import com.e_rachit.e_rachitcloudadsplayer.models.SchedulePlaylistResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import rx.Observable


/**
 * Created by rohitranjan on 05/11/17.
 */

public interface CloudServices {

    @GET("/index.php/secure/get_playlist/{device_id}")
    fun getPlaylist(@Path("device_id") device_id: String): Observable<Playlist>

    @GET("/index.php/secure/get_default_playlist/{device_id}")
    fun getDefaultPlaylist(@Path("device_id") device_id: String): Observable<Playlist>

    @GET
    fun downloadFile(@Url fileUrl: String): Call<ResponseBody>

    @POST("/index.php/secure/update_device_info/")
    fun registerOneSignalDevice(@Body oneSignalDeviceRegistration: OneSignalDeviceRegistration): Observable<ResponseBody>

    @GET("/index.php/secure/check_device/")
    fun getDeviceSettingLockState(@Query("device_id") deviceId: String): Observable<DeviceSettingLockState>

    @GET("/index.php/secure/get_schedule_list/")
    fun getDeviceSchedules(@Query("device_id") device_id: String): Observable<SchedulePlaylistResponse>

    @GET("/index.php/secure/get_playlist_by_id")
    fun getPlaylistById(@Query("playlist_id") playlist_id: String): Observable<Playlist>

}