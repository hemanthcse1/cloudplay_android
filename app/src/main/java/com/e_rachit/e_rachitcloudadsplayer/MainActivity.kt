package com.e_rachit.e_rachitcloudadsplayer

import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.webkit.JavascriptInterface
import android.widget.*
import com.e_rachit.e_rachitcloudadsplayer.models.*
import com.e_rachit.e_rachitcloudadsplayer.models.Constants.CHECK_DEFAULT_PLAY_LIST_UPDATE
import com.e_rachit.e_rachitcloudadsplayer.models.Constants.CHECK_PLAY_LIST_UPDATE
import com.e_rachit.e_rachitcloudadsplayer.models.Constants.CURRENT_PLAYLIST_DIR_PATH
import com.e_rachit.e_rachitcloudadsplayer.models.Constants.DEFAULT_PLAYLIST_DIR_PATH
import com.e_rachit.e_rachitcloudadsplayer.models.Constants.DEFAULT_PLAYLIST_PREF_KEY
import com.e_rachit.e_rachitcloudadsplayer.models.Constants.DEVICE_ID
import com.e_rachit.e_rachitcloudadsplayer.models.Constants.DEVICE_SCHEDULE
import com.e_rachit.e_rachitcloudadsplayer.receivers.NotificationReceiver.Companion.PN_MESSAGE
import com.e_rachit.e_rachitcloudadsplayer.receivers.ScheduleAlarmReceiver.Companion.SCHEDULE_TRIGGER
import com.e_rachit.e_rachitcloudadsplayer.services.CloudServices
import com.e_rachit.e_rachitcloudadsplayer.services.DownloadManager
import com.e_rachit.e_rachitcloudadsplayer.services.ScheduleManager
import com.e_rachit.e_rachitcloudadsplayer.services.WifiHelper
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.w3c.dom.Text
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File.separator
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : Activity() {

    private var currentActivity: MainActivity? = null
    private var scheduleMap = mutableMapOf<Int, Schedule>()
    private val TAG: String = "MainActivity"
    private var currentPlaylist: Playlist? = null
    private var currentVideoPlayer: PlaylistPlayer? = null
    private val cloudServices = CloudAdsPlayerApplication.Companion.retrofit!!.create(CloudServices::class.java)
    private var wifiHelper: WifiHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)
        currentActivity = this
        wifiHelper = WifiHelper(currentActivity)
        val offlinePlaylist = Utils.readPreferenceData(currentActivity!!, "PLAYLIST", "")
        offlinePlaylist?.let {
            try {
                val playlist = Utils.gson.fromJson(offlinePlaylist, Playlist::class.java)
                play(playlist, currentActivity!!.filesDir.toString() + CURRENT_PLAYLIST_DIR_PATH)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        parent_view.setOnClickListener {
            setting_view.visibility = if (setting_view.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        setting_view.setOnClickListener {
            showDeviceSettingsDialog()
        }

        if (Utils.readPreferenceData(currentActivity!!, DEFAULT_PLAYLIST_DIR_PATH, "").equals(""))
            updateDefaultPlaylist()

        updateDeviceSchedule()
    }

    /**
     * updates the default playlist
     */
    private fun updateDefaultPlaylist(iCompleteListener: ICompleteListener? = null) {
        val deviceId: String? = Utils.readPreferenceData(currentActivity!!, DEVICE_ID, "")
        if (!deviceId!!.trim().equals(""))
            cloudServices.getDefaultPlaylist(deviceId).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : rx.Observer<Playlist> {
                override fun onError(e: Throwable?) {
                    e?.printStackTrace()
                    iCompleteListener?.error()
                }

                override fun onCompleted() {
                }

                override fun onNext(playlist: Playlist?) {
                    Toast.makeText(currentActivity, "Default Playlist Updated...", Toast.LENGTH_SHORT).show()
                    playlist?.let {
                        Utils.savePreferenceData(currentActivity!!, DEFAULT_PLAYLIST_PREF_KEY, Utils.gson.toJson(playlist).toString())
                        // play(playlist)
                        Utils.createDirectory(currentActivity!!.filesDir.toString() + DEFAULT_PLAYLIST_DIR_PATH)
                        val downloadFiles = ArrayList<DownloadFile>()
                        for (playlistItem in playlist.playlistItems) {
                            downloadFiles.add(DownloadFile(playlistItem.url, playlistItem.filename, playlistItem.content_length))
                        }
                        Utils.createDirectory(currentActivity!!.filesDir.toString() + DEFAULT_PLAYLIST_DIR_PATH)
                        DownloadManager(downloadFiles, currentActivity!!.filesDir.toString() + DEFAULT_PLAYLIST_DIR_PATH, object : DownloadManager.IOnFileDownloadCallback {
                            override fun onFileDownloaded(index: Int) {
                                Toast.makeText(currentActivity, "Default Playlist Downloaded...", Toast.LENGTH_SHORT).show()
                                iCompleteListener?.finished()
                            }
                        })
                    }
                }
            })
        else {
            Toast.makeText(currentActivity!!, "Please set the device id", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * plays default playlist
     */
    private fun playDefaultPlaylist() {
        val defaultPlaylist = Utils.readPreferenceData(currentActivity!!, DEFAULT_PLAYLIST_PREF_KEY, "")
        defaultPlaylist?.let {
            val playlist = Utils.gson.fromJson<Playlist>(defaultPlaylist, Playlist::class.java)
            play(playlist, currentActivity!!.filesDir.toString() + DEFAULT_PLAYLIST_DIR_PATH)
        }
    }


    /**
     * shows the settings dialog
     */
    private fun showDeviceSettingsDialog() {
        val deviceId = Utils.readPreferenceData(currentActivity!!, DEVICE_ID, "")
        val dialog = Dialog(currentActivity)
        dialog.setContentView(R.layout.settings_dialog)
        dialog.setTitle("Device Settings (v" + BuildConfig.VERSION_NAME + ")")
        dialog.findViewById<Button>(R.id.setup_wifi_btn).setOnClickListener {
            if (wifiHelper!!.checkPermission())
                wifiHelper?.startWifiScans()
        }
        val wifiConn = wifiHelper!!.isConnected()
        dialog.findViewById<TextView>(R.id.is_dialog_connected).setText(if (wifiConn == null) "No" else wifiConn)
        val deviceIdET = dialog.findViewById<TextView>(R.id.dialog_device_id_et)
        deviceIdET.setText(deviceId!!)
        (dialog.findViewById<Button>(R.id.dialog_settings_update_btn) as Button).setOnClickListener {
            Utils.savePreferenceData(currentActivity!!, DEVICE_ID, deviceIdET.text.toString())
            dialog.dismiss()
            Utils.updateDeviceId(deviceIdET.text.toString(), Utils.readPreferenceData(currentActivity!!, Constants.ONE_SIGNAL_ID, "")!!)

            setting_view.visibility = View.GONE
            updateDefaultPlaylist(object : ICompleteListener {
                override fun finished() {
                    updateDeviceSchedule()
                }

                override fun error() {
                    Toast.makeText(currentActivity, "Unable to fetch Default Playlist.", Toast.LENGTH_LONG).show()
                    updateDeviceSchedule()
                }
            })

        }

        dialog.show()
    }

    /**
     * plays the passed in playlist
     *
     * @param playlist: the playlist to be played
     */
    private fun play(playlist: Playlist?, downloadDir: String) {
        playlist?.let {
            if (currentPlaylist != null && Utils.gson.toJson(currentPlaylist).equals(Utils.gson.toJson(playlist)))
                return
            currentPlaylist = playlist
            val downloadFiles = ArrayList<DownloadFile>()
            for (playlistItem in playlist.playlistItems) {
                downloadFiles.add(DownloadFile(playlistItem.url, playlistItem.filename, playlistItem.content_length))
            }

            //creates a directory if does not exists
            Utils.createDirectory(downloadDir)
            // Download the files from the playlist
            DownloadManager(downloadFiles, downloadDir, object : DownloadManager.IOnFileDownloadCallback {
                override fun onFileDownloaded(index: Int) {
                    if (playlist.orientation == Orientation.PORTRAIT) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    }
                    val updatedPlayListItems = ArrayList<PlaylistItem>()
                    for (item in playlist.playlistItems) {
                        item.offlineUrl = downloadDir + separator + item.filename
                        updatedPlayListItems.add(item)
                    }
                    if (currentVideoPlayer != null)
                        currentVideoPlayer?.destroyPlayer()
                    currentVideoPlayer = PlaylistPlayer(updatedPlayListItems, currentActivity!!, video_view, listOf<ImageView>(image_view_1, image_view_2))
                    currentVideoPlayer!!.init()
                    setBannerView(playlist.banner)
                }
            })
        }
    }

    /**
     * plays the current Schedule from list of schedule
     *
     * @param schedules
     */
    private fun playCurrentSchedule(schedules: List<Schedule>?) {
        schedules?.let {
            // Utils.savePreferenceData(currentActivity!!, CURRENT_PLAYLIST_ID, -1)
            val scheduleManager = ScheduleManager(currentActivity!!)
            val sortedSchedules = scheduleManager.sortAndFilterSchedule(schedules)
            val filteredSchedules = scheduleManager.getCurrentSchedule(sortedSchedules)
            filteredSchedules?.let {
                if (filteredSchedules.size >= 0) {
                    // play the selected schedule
                    val playlist = scheduleMap.get(filteredSchedules.get(0).scheduleId)
                    // Utils.savePreferenceData(currentActivity!!, CURRENT_PLAYLIST_ID, playlist!!.playlist_id)

                    // clear existing schedules
                    ScheduleManager(currentActivity!!).clearAllAlarms()

                    // set current schedule
                    ScheduleManager(currentActivity!!).setScheduleEndAlarm(filteredSchedules.get(0))

                    if (filteredSchedules[0].scheduleId < 0) {
                        playDefaultPlaylist()
                    } else {
                        playlist?.let {
                            updatePlaylist(playlist.playlist_id)
                        }
                    }
                } else {
                    // conflict case
                    Toast.makeText(currentActivity, "More than one playlists are eligible to be played at this time.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * updates the local playlist data
     *
     * @param id
     */
    private fun updatePlaylist(id: Int) {
        val deviceId: String? = Utils.readPreferenceData(currentActivity!!, DEVICE_ID, "")
        if (!deviceId!!.trim().equals(""))
            cloudServices.getPlaylistById(id.toString()).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : rx.Observer<Playlist> {
                override fun onError(e: Throwable?) {
                    e?.printStackTrace()
                }

                override fun onCompleted() {

                }

                override fun onNext(playlist: Playlist?) {
                    Utils.savePreferenceData(currentActivity!!, "PLAYLIST", Utils.gson.toJson(playlist).toString())
                    play(playlist, currentActivity!!.filesDir.toString() + CURRENT_PLAYLIST_DIR_PATH)
                }

            })
        else {
            Toast.makeText(currentActivity!!, "Please set the device id", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * updates the device schedules
     */
    private fun updateDeviceSchedule() {
        val deviceId: String? = Utils.readPreferenceData(currentActivity!!, DEVICE_ID, "")
        if (!deviceId!!.trim().equals(""))
            cloudServices.getDeviceSchedules(deviceId).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : rx.Observer<SchedulePlaylistResponse> {
                override fun onNext(t: SchedulePlaylistResponse?) {
                    Utils.savePreferenceData(currentActivity!!, DEVICE_SCHEDULE, Utils.gson.toJson(t))
                    if (t?.schedule != null) {
                        if (t.schedule.size > 0)
                            Toast.makeText(currentActivity!!, "New schedule has been set up", Toast.LENGTH_SHORT).show()
                        t.schedule.forEach { schedule: Schedule ->
                            scheduleMap.put(schedule.schedule_id, schedule)
                        }
                        playCurrentSchedule(t.schedule)
                    } else {
                        playDefaultPlaylist()
                    }
                }

                override fun onError(e: Throwable?) {
                    val scheduleData = Utils.readPreferenceData(currentActivity!!, DEVICE_SCHEDULE, "")
                    if (!scheduleData!!.isEmpty()) {
                        val schedules = Utils.gson.fromJson<SchedulePlaylistResponse>(scheduleData, SchedulePlaylistResponse::class.java).schedule
                        if (schedules != null) {
                            schedules.forEach { schedule: Schedule ->
                                scheduleMap.put(schedule.schedule_id, schedule)
                            }
                            playCurrentSchedule(schedules)
                        } else {
                            playDefaultPlaylist()
                        }
                    }
                }

                override fun onCompleted() {

                }
            })
        else {
            Toast.makeText(currentActivity!!, "Please set the device id", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * sets the marquee text model
     *
     * @param textView
     * @param bannerViewTextModel
     */
    private fun setBannerView(bannerViewTextModel: Banner?) {
        if (bannerViewTextModel == null) {
            banner_parent.visibility = View.GONE
            return
        } else
            banner_parent.visibility = View.VISIBLE
        try {
            marqueeText!!.post {
                try {
                    if (marqueeText != null) {
                        // Get the screen width
                        val size = Point()
                        windowManager.defaultDisplay.getSize(size)
                        val screenWidth = size.x
                        marqueeText.setTextColor(Color.parseColor(bannerViewTextModel.font_color))
                        marqueeText.setTextSize(COMPLEX_UNIT_DIP, (30 + Integer.parseInt(bannerViewTextModel.font_size)).toFloat())
                        val bounds = Rect()
                        val textPaint = marqueeText.paint
                        //textPaint.setTextSize(30 + Integer.parseInt(bannerViewTextModel.getFont_size()));
                        textPaint.getTextBounds(bannerViewTextModel.name, 0, bannerViewTextModel.name.length, bounds)

                        var fontPath: String? = null
                        try {
                            val am = assets
                            val mapList = Arrays.asList(*am.list("fonts"))
                            if (mapList.contains(bannerViewTextModel.font_family)) {
                                fontPath = "fonts/" + bannerViewTextModel.font_family
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        /* set font to the text view */
                        if (fontPath != null) {
                            val typeface = Typeface.createFromAsset(currentActivity!!.getAssets(), fontPath)
                            marqueeText.typeface = typeface
                        }

                        val width = bounds.width()
                        if (width > screenWidth) {
                            val params = marqueeText.layoutParams
                            params.width = width
                            marqueeText.layoutParams = params
                        } else {
                            val params = marqueeText.layoutParams
                            params.width = WRAP_CONTENT
                            marqueeText.layoutParams = params
                        }
                        marqueeText.post {
                            try {
                                marqueeText.setText(bannerViewTextModel.name)
                                val animation: Animation
                                if (Integer.parseInt(bannerViewTextModel.direction) < 0)
                                    animation = TranslateAnimation(screenWidth.toFloat(), (-1 * width).toFloat(), 0f, 0f)
                                else
                                    animation = TranslateAnimation((-1 * width).toFloat(), screenWidth.toFloat(), 0f, 0f)
                                animation.setFillAfter(true)
                                animation.setFillEnabled(true)
                                banner_parent.setBackgroundColor(if (bannerViewTextModel.background_color.equals("#ffffff")) Color.parseColor("#00000000") else Color.parseColor(bannerViewTextModel.background_color))
                                val params = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT
                                )
                                params.setMargins(0, bannerViewTextModel.top_margin * 5, 0, 0)
                                banner_parent.setLayoutParams(params)
                                // animation.setDuration(30000)
                                animation.setDuration(Integer.parseInt(bannerViewTextModel.duration).toLong())
                                animation.setInterpolator(LinearInterpolator())
                                animation.setRepeatMode(Animation.RESTART)
                                animation.setRepeatCount(Animation.INFINITE)
                                marqueeText.animation = animation
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(currentActivity, "Unable to load banner message. Check configuration file.", Toast.LENGTH_SHORT).show()
        }

    }

    /**
     * Broadcast receiver for push notification in case of the APP is in the foreground
     */
    private val mPNReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            processBroadcast(intent)
        }
    }

    /**
     * process Push Notification Messages
     */
    private fun processBroadcast(intent: Intent) {
        val bundle = intent.extras
        bundle.let {
            if (intent.action == PN_MESSAGE) {
                val data = bundle.getString(Constants.PN_DATA)
                if (data != null && !data.isEmpty()) {
                    val pnData = Utils.gson.fromJson<JSONObject>(data, JSONObject::class.java)

                    if (pnData.has("command")) {
                        val command = pnData.getString("command")
                        if (command.equals(CHECK_PLAY_LIST_UPDATE)) {
                            updateDeviceSchedule()
                        } else if (command.equals(CHECK_DEFAULT_PLAY_LIST_UPDATE)) {
                            updateDefaultPlaylist(object : ICompleteListener {
                                override fun finished() {
                                    updateDeviceSchedule()
                                }

                                override fun error() {
                                    updateDeviceSchedule()
                                    //TODO Hemanth Commented
                                }
                            })
                        }
                    }
                }
            } else if (intent.action == SCHEDULE_TRIGGER) {
                updateDeviceSchedule()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mPNReceiver, IntentFilter(PN_MESSAGE))
        registerReceiver(mPNReceiver, IntentFilter(SCHEDULE_TRIGGER))
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(mPNReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    interface ICompleteListener {
        fun finished()
        fun error()
    }


}
