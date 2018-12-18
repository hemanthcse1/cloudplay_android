package com.e_rachit.e_rachitcloudadsplayer

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.e_rachit.e_rachitcloudadsplayer.models.OneSignalDeviceRegistration
import com.e_rachit.e_rachitcloudadsplayer.services.CloudServices
import com.google.gson.Gson
import okhttp3.ResponseBody
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File

/**
 * Created by rohitranjan on 06/11/17.
 */
class Utils {

    companion object {
        val cloudServices = CloudAdsPlayerApplication.retrofit!!.create(CloudServices::class.java)
        var gson = Gson()

        /**
         * save user preferences
         *
         * @param context
         * @param key
         * @param value
         */
        fun savePreferenceData(context: Context, key: String, value: String) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sp.edit()
            editor.putString(key, value)
            editor.apply()
        }

        /**
         * save user preferences
         *
         * @param context
         * @param key
         * @param value
         */
        fun savePreferenceData(context: Context, key: String, value: Long) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sp.edit()
            editor.putLong(key, value)
            editor.apply()
        }

        /**
         * save user preferences
         *
         * @param context
         * @param key
         * @param value
         */
        fun savePreferenceData(context: Context, key: String, value: Boolean?) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sp.edit()
            editor.putBoolean(key, value!!)
            editor.apply()
        }


        /**
         * save user preferences
         *
         * @param context
         * @param key
         * @param value
         */
        fun savePreferenceData(context: Context, key: String, value: Set<String>?) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sp.edit()
            editor.putStringSet(key, value!!)
            editor.apply()
        }

        /**
         * save user preferences
         *
         * @param context
         * @param key
         * @param value
         */
        fun savePreferenceData(context: Context, key: String, value: Int) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sp.edit()
            editor.putInt(key, value)
            editor.apply()
        }

        /**
         * clear all the key value pairs from the preferences
         *
         * @param context
         */
        fun clearPreferences(context: Context) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sp.edit()
            editor.clear()
            editor.apply()
        }

        /**
         * removes the passed in key from the preferences
         *
         * @param context
         * @param key
         */
        fun removePreferenceData(context: Context, key: String) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            sp.edit().remove(key).apply()
        }

        /**
         * delete specific keys value pair from the preferences
         *
         * @param context
         * @param key
         */
        fun deletePreferenceData(context: Context, key: String) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sp.edit()
            editor.remove(key)
            editor.apply()
        }

        /**
         * read user preferences
         *
         * @param context
         * @param key
         * @param defaultValue
         * @return
         */
        fun readPreferenceData(context: Context?, key: String, defaultValue: String): String? {
            if (context != null) {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                return sp.getString(key, defaultValue)
            }
            return null
        }

        /**
         * read user preferences
         *
         * @param context
         * @param key
         * @param defaultValue
         * @return
         */
        fun readPreferenceData(context: Context?, key: String, defaultValue: Long): Long {
            if (context != null) {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                return sp.getLong(key, defaultValue)
            }
            return 0
        }

        /**
         * read user preferences
         *
         * @param context
         * @param key
         * @param defaultValue
         * @return
         */
        fun readPreferenceData(context: Context?, key: String, defaultValue: Set<String>?): HashSet<String> {
            if (context != null) {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                return HashSet(sp.getStringSet(key, defaultValue))
            }
            return HashSet<String>()
        }

        /**
         * read user preferences
         *
         * @param context
         * @param key
         * @param defaultValue
         * @return
         */
        fun readPreferenceData(context: Context?, key: String, defaultValue: Boolean): Boolean {
            if (context != null) {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                return sp.getBoolean(key, defaultValue)
            }
            return false
        }

        /**
         * read user preferences
         *
         * @param context
         * @param key
         * @param defaultValue
         * @return
         */
        fun readPreferenceData(context: Context?, key: String, defaultValue: Int): Int {
            if (context != null) {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                return sp.getInt(key, defaultValue)
            }
            return -1
        }

        /**
         * overriding the function to add the gson conversion to it
         *
         * @param context
         * @param key
         * @param defaultValue
         * @param clazz
         * @return
         */
        fun readPreferenceData(context: Context, key: String, defaultValue: String, clazz: Class<*>): Any? {
            val result = readPreferenceData(context, key, defaultValue)
            return if (result != null) {
                gson.fromJson(result, clazz)
            } else result
        }

        /**
         * create directory locally if already dose not exists
         *
         * @param path
         */
        fun createDirectory(path: String) {
            val dirPath = File(path)
            if (!dirPath.exists())
                dirPath.mkdirs()
        }

        /**
         * update the device id and one signal id to the server
         *
         * @param deviceId
         * @param one_signal_id
         */
        fun updateDeviceId(deviceId: String, one_signal_id: String) {
            if (!deviceId.trim().equals("") && !one_signal_id.trim().equals(""))
                cloudServices.registerOneSignalDevice(OneSignalDeviceRegistration(deviceId, one_signal_id)).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : rx.Observer<ResponseBody> {
                    override fun onCompleted() {

                    }

                    override fun onError(e: Throwable) {
                        Log.d("ONE_SIGNAL", e.message)
                    }

                    override fun onNext(responseBody: ResponseBody) {
                        Log.d("ONE_SIGNAL", "success")
                    }
                })
        }
    }
}