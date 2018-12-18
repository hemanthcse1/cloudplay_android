package com.e_rachit.e_rachitcloudadsplayer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.e_rachit.e_rachitcloudadsplayer.models.Constants

/**
 * Created by rohitranjan on 30/12/17.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val mNotificationId = 1
        val intent1 = Intent(SCHEDULE_TRIGGER)
        intent1.putExtra(Constants.SCHEDULE_TIMER_OVER, "")
        context?.sendBroadcast(intent1)
        /* val mBuilder = NotificationCompat.Builder(context!!, "something")
                 .setSmallIcon(R.mipmap.ic_launcher)
                 .setContentTitle("My notification")
                 .setContentText("Hello World!")

         // Gets an instance of the NotificationManager service
         val mNotifyMgr = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
         // Builds the notification and issues it.
         mNotifyMgr.notify(mNotificationId, mBuilder.build())*/
        /*val notificationManager = NotificationHelper(context!!)
        notificationManager.createChannels()
        val format = SimpleDateFormat("MMM dd,yyyy  hh:mm a", Locale.US)
        val date = format.format(Date())
        notificationManager.notify(100, notificationManager.getNotification1("Sample Alarm", "Alarm went of at " + date))*/
    }
    companion object {
        val SCHEDULE_TRIGGER = "SCHEDULE_TRIGGER"
    }
}