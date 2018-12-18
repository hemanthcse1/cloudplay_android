package com.e_rachit.e_rachitcloudadsplayer.receivers

import android.content.Context
import android.content.Intent
import android.util.Log
import com.e_rachit.e_rachitcloudadsplayer.Utils
import com.e_rachit.e_rachitcloudadsplayer.models.Constants.PN_DATA
import com.onesignal.OSNotification
import com.onesignal.OneSignal


/**
 * Created by rohitranjan on 19/11/17.
 */
class NotificationReceiver(val context: Context) : OneSignal.NotificationReceivedHandler {

    override fun notificationReceived(notification: OSNotification?) {

        val data = notification!!.payload.additionalData
        try {
            if (data != null) {
                Log.i("OneSignalPNReceived", "data : " + Utils.gson.toJson(data));
                val intent = Intent(PN_MESSAGE)
                intent.putExtra(PN_DATA, Utils.Companion.gson.toJson(data))
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val PN_MESSAGE = "PN_MESSAGE"
    }
}