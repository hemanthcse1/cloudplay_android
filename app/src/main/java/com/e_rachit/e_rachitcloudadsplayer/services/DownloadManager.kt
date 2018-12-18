package com.e_rachit.e_rachitcloudadsplayer.services

import android.os.Handler
import com.e_rachit.e_rachitcloudadsplayer.CloudAdsPlayerApplication
import com.e_rachit.e_rachitcloudadsplayer.models.DownloadFile
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.io.*
import java.io.File.separator


/**
 * Created by rohitranjan on 06/11/17.
 */
class DownloadManager(val downloadFiles: List<DownloadFile>, var basePath: String, val iOnFileDownloadCallback: DownloadManager.IOnFileDownloadCallback) {

    private val cloudServices = CloudAdsPlayerApplication.Companion.retrofit!!.create(CloudServices::class.java)
    private var currentDownloadFileIndex = 0

    init {
        if (downloadFiles.size > 0)
            downloadItem(currentDownloadFileIndex)
    }

    /**
     * download the file with the given index
     *
     * @param index
     */
    private fun downloadItem(index: Int) {
        if (currentDownloadFileIndex == downloadFiles.size) {
            iOnFileDownloadCallback.onFileDownloaded(-1)
        } else {
            downloadFile(downloadFiles[index])
        }
    }

    /**
     * initiates the download file call
     */
    private fun downloadFile(downloadFile: DownloadFile) {
        val filePath = basePath + separator + downloadFile.filename
        if (downloadFile.contentLength == File(filePath).length()) {
            currentDownloadFileIndex += 1
            downloadItem(currentDownloadFileIndex)

        } else
            cloudServices.downloadFile(downloadFile.url).enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                    reDownloadItem()
                }

                override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                    response?.let {
                        if (response.isSuccessful) {
                            if (!(File(filePath).exists()) || downloadFile.contentLength != File(filePath).length()) {
                                if (File(filePath).exists()) {
                                    File(filePath).delete()
                                }
                                val writtenToDisk: Boolean = writeResponseBodyToDisk(filePath, response.body()!!)
                                if (writtenToDisk) {
                                    currentDownloadFileIndex += 1
                                    downloadItem(currentDownloadFileIndex)
                                } else
                                    reDownloadItem()
                            } else {
                                currentDownloadFileIndex += 1
                                downloadItem(currentDownloadFileIndex)
                            }
                        } else {
                            reDownloadItem()
                        }
                    }
                }

                private fun reDownloadItem() {
                    Handler().postDelayed({
                        downloadItem(currentDownloadFileIndex)
                    }, 10000)
                }
            })
    }

    /**
     * writes the response to teh disk
     *
     * @param path: the path where the file has to be saved
     * @param body: the content that needs to be saved
     */
    private fun writeResponseBodyToDisk(path: String, body: ResponseBody): Boolean {
        try {
            val futureStudioIconFile = File(path)

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
               // val fileReader = ByteArray(4096)
                val fileReader = ByteArray(10240)

                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0

                inputStream = body.byteStream()
                outputStream = FileOutputStream(futureStudioIconFile)

                while (true) {
                    val read = inputStream!!.read(fileReader)

                    if (read == -1) {
                        break
                    }

                    outputStream.write(fileReader, 0, read)

                    fileSizeDownloaded += read.toLong()

                    // Log.d(TAG, "file download: $fileSizeDownloaded of $fileSize")
                }

                outputStream.flush()

                return true
            } catch (e: IOException) {
                return false
            } finally {
                if (inputStream != null) {
                    inputStream.close()
                }

                if (outputStream != null) {
                    outputStream.close()
                }
            }
        } catch (e: IOException) {
            return false
        }

    }

    interface IOnFileDownloadCallback {
        fun onFileDownloaded(index: Int)
    }
}