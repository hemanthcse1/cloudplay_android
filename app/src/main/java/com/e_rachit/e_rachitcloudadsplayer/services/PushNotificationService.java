package com.e_rachit.e_rachitcloudadsplayer.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.e_rachit.e_rachitcloudadsplayer.CloudAdsPlayerApplication;
import com.e_rachit.e_rachitcloudadsplayer.Utils;
import com.e_rachit.e_rachitcloudadsplayer.models.Constants;
import com.e_rachit.e_rachitcloudadsplayer.receivers.NotificationReceiver;
import com.onesignal.OneSignal;

/**
 * Created by rohitranjan on 21/11/17.
 */

public class PushNotificationService extends Service {

    private final IBinder mBinder = new PushNotificationServiceBinder();
    public static String one_signal_device_id = "";
    private static boolean isStarted = false;
    private CloudServices cloudServices = CloudAdsPlayerApplication.Companion.getRetrofit().create(CloudServices.class);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isStarted) {
            isStarted = true;
            initializeOneSignal();
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    /**
     * register the OneSignal Push Notification GCM/FCM receiver
     */
    private void initializeOneSignal() {

         /* initialize the One Signal Application */
        OneSignal.startInit(this).inFocusDisplaying(OneSignal.OSInFocusDisplayOption.None).setNotificationReceivedHandler(new NotificationReceiver(getApplicationContext())).init();

        /* shows the notification even if the app is open */
        //OneSignal.enableInAppAlertNotification(false);
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.INFO, OneSignal.LOG_LEVEL.NONE);
        //OneSignal.enableNotificationsWhenActive(true);
        try {
        /* register the user's one signal id and google registration ID */
            OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
                @Override
                public void idsAvailable(String userId, String registrationId) {
                    one_signal_device_id = userId;
                    Utils.Companion.savePreferenceData(getApplicationContext(), Constants.INSTANCE.getONE_SIGNAL_ID(), one_signal_device_id);
                    String deviceId = Utils.Companion.readPreferenceData(getApplicationContext(), Constants.INSTANCE.getDEVICE_ID(), "");
                    Utils.Companion.updateDeviceId(deviceId, one_signal_device_id);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public class PushNotificationServiceBinder extends Binder {
        PushNotificationService getService() {
            return PushNotificationService.this;
        }
    }
}