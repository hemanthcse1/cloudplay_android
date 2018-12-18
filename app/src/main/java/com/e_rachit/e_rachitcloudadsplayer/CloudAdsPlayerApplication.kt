package com.e_rachit.e_rachitcloudadsplayer

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.e_rachit.e_rachitcloudadsplayer.models.Constants
import com.e_rachit.e_rachitcloudadsplayer.services.PushNotificationService
import io.fabric.sdk.android.Fabric

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


/**
 * Created by rohitranjan on 08/10/17.
 */
class CloudAdsPlayerApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        init()
    }

    /**
     * Initializes the application
     */
    private fun init() {
        updateDeviceId()
        try {
            startService(Intent(this, PushNotificationService::class.java))
        } catch (e: IllegalStateException) {
        }

        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
//                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()

        retrofit = Retrofit.Builder()
                .baseUrl("http://www.cloudplay.in")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()


    }

    /**
     * update the device Id from the device Installer App
     */
    private fun updateDeviceId() {
        val cursor = contentResolver.query(Uri.parse("content://" + Constants.APP_UPDATE_CONTENT_PROVIDER + "/forKey/DEVICE_SERIAL_NUMBER"), arrayOf<String>(), "", arrayOf<String>(), "")
        if (cursor != null && cursor.count > 0) {
            var deviceId = ""
            while (cursor.moveToNext()) {
                deviceId = cursor.getString(1);
            }
            cursor.close()
            if (deviceId.isNotEmpty()) {
                Utils.savePreferenceData(this, Constants.DEVICE_ID, deviceId)
            } else {
                Utils.savePreferenceData(this, Constants.DEVICE_ID, "UnconfiguredDevice")
            }
        } else {
            Toast.makeText(this, "Base Application not installed", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        var retrofit: Retrofit? = null
    }

}