package com.e_rachit.e_rachitcloudadsplayer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.e_rachit.e_rachitcloudadsplayer.models.PlaylistItem
import java.util.*
import kotlin.coroutines.experimental.buildSequence

/**
 * Created by rohitranjan on 12/12/17.
 */
class PlaylistPlayer(val playlistItems: ArrayList<PlaylistItem>, val currentActivity: MainActivity, var video_view: VideoView?, var image_views: List<ImageView>?) {

    private var timerTask: TimerTask? = null
    private var currentItemType: String = ""
    private var timer: Timer? = null
    private var currentTime: Int = 0
    private var currentIndex = 0
    private var currentImageView: ImageView? = null
    private var currentVisibleView: View? = null
    private var currentImageViewIndex = -1


    fun init() {
        image_views?.let {
            currentImageView = image_views!!.get(0)
        }
        if (playlistItems.size > 0)
            play()
    }

    /**
     * plays the current media
     */
    private fun play() {
        when (playlistItems.get(currentIndex).content_type) {
            "video" -> {
                currentItemType = "V"
                currentActivity.runOnUiThread {
                    video_view?.let {
                        showView(video_view, currentVisibleView)
                        currentVisibleView = video_view as View
                        playVideo(playlistItems.get(currentIndex).offlineUrl, playlistItems.get(currentIndex).volume, object : MainActivity.ICompleteListener {
                            override fun finished() {
                                playNextItem()
                            }

                            override fun error() {
                                playNextItem()
                            }
                        })
                    }
                }
            }
            "image" -> {
                currentItemType = "I"
                currentActivity.runOnUiThread {
                    currentImageView = getNextImageView().first()
                    currentImageView?.let {
                        showView(currentImageView, currentVisibleView)
                        currentVisibleView = currentImageView as View
                        playImage(playlistItems.get(currentIndex).offlineUrl, playlistItems.get(currentIndex).img_duration, object : MainActivity.ICompleteListener {
                            override fun finished() {
                                playNextItem()
                            }

                            override fun error() {
                                playNextItem()
                            }
                        })
                    }
                }
            }
        }
    }

    /**
     * returns next Image View
     */
    private fun getNextImageView() = buildSequence {
        while (true) {
            currentImageViewIndex = if (currentImageViewIndex + 1 == image_views?.size) 0 else currentImageViewIndex + 1
            yield(image_views?.get(currentImageViewIndex))
        }
    }

    /**
     * shows the view being passed and hides the other views
     *
     * @param view: the view to be viewed
     */
    private fun showView(view: View?, currentView: View?) {
        view?.let {
            view.alpha = 0f
            view.visibility = View.VISIBLE

            view.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setListener(null)
            currentView?.let {
                currentView.animate()!!.alpha(0f)
                        .setDuration(1000)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                                currentView.visibility = View.INVISIBLE
                            }
                        })
            }
        }
    }

    /**
     * play Video player
     *
     * @param url
     * @param completeListener
     */
    private fun playVideo(url: String, volume: Float, completeListener: MainActivity.ICompleteListener?) {
        val mediaController = MediaController(currentActivity)
        mediaController.setVisibility(View.GONE);
        mediaController.setAnchorView(video_view)
        video_view?.setOnPreparedListener(object : MediaPlayer.OnPreparedListener {
            override fun onPrepared(p0: MediaPlayer?) {
                p0?.let {
                    p0.setVolume(volume, volume)
                }
            }
        })
        video_view?.setOnCompletionListener {
            completeListener?.let {
                video_view?.stopPlayback()
                video_view?.setVideoURI(null)
                completeListener.finished()
            }
        }
        video_view?.setOnErrorListener(object : MediaPlayer.OnErrorListener {
            override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
                completeListener?.let {
                    completeListener.error()
                }
                return true
            }
        })

        // val uri = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/media/1.mp4")

        video_view?.setMediaController(mediaController)
        video_view?.setVideoURI(Uri.parse(url))
        video_view?.requestFocus()
        video_view?.start()
    }

    /**
     * play Image
     *
     * @param url
     * @param time
     * @param completeListener
     */
    private fun playImage(url: String, time: Int, completeListener: MainActivity.ICompleteListener?) {
        try {
            timer = Timer()
            currentTime = 0
            Glide.with(currentActivity)
                    .load(url)
                    // .apply(RequestOptions.centerCropTransform())
                    .into(currentImageView)
            timerTask = object : TimerTask() {
                override fun run() {
                    currentTime += 1
                    if (currentTime == time) {
                        timer!!.cancel()
                        completeListener?.let {
                            completeListener.finished()
                        }
                    }
                }
            }
            timer!!.schedule(timerTask, 1000, 1000)
        } catch (e: Exception) {

        }
    }

    /**
     * play the next Item in the list
     */
    private fun playNextItem() {
        currentIndex = if (currentIndex == playlistItems.size - 1) 0 else currentIndex + 1
        play()
    }

    /**
     * pause the playback
     */
    fun pausePlayback() {
        when (currentItemType) {
            "V" -> {
                video_view?.pause()
            }
            "I" -> {
                /* timerTask?.let{
                     timerTask!!.removeCallbacks()
                 }*/
                timer?.let {
                    timer!!.cancel()
                }
            }
            else -> {
            }
        }
    }

    fun destroyPlayer() {
        pausePlayback()
        video_view = null
        currentImageView = null
    }
}