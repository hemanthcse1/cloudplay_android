package com.e_rachit.e_rachitcloudadsplayer.utils

import android.widget.Toast
import com.e_rachit.e_rachitcloudadsplayer.MainActivity
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

/**
 * Created by rohitranjan on 15/10/17.
 */

class MQTTManager(val activity: MainActivity) {

    private val ip = "tcp://52.66.128.145:1883"
    private val username = "cloud_player"
    private val password = "only4Priyanka".toCharArray()
    private lateinit var client: MqttAndroidClient
    private var isConnected = false

    fun connect(imqttManagerCallback: IMQTTManagerConnectCallback) {
        val clientId = MqttClient.generateClientId()
        client = MqttAndroidClient(activity, ip,
                clientId)
        val options = MqttConnectOptions()
        options.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
        options.userName = username
        options.password = password
        try {
            val token = client.connect(options)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // We are connected
//                    Log.d(TAG, "onSuccess")
                    Toast.makeText(activity, "onSuccess", Toast.LENGTH_SHORT).show()
                    imqttManagerCallback.onConnect(asyncActionToken)
//                    subscribe()
                    isConnected = true
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    // Something went wrong e.g. connection timeout or firewall problems
//                    Log.d(TAG, "onFailure")
                    Toast.makeText(activity, "onFailure", Toast.LENGTH_SHORT).show()
                    imqttManagerCallback.onConnectFailure(asyncActionToken, exception)
                    isConnected = false
                }
            }

        } catch (e: MqttException) {
            e.printStackTrace()
            isConnected = false
        }
    }

    /**
     * publishes Data to the topic
     */
    fun publishData(topic: String, payload: String, imqttManagerPublishCallback: IMQTTManagerPublishCallback) {
        if (!isConnected)
            imqttManagerPublishCallback.onFailed(Exception("Broker Not Connected"))
        var encodedPayload = ByteArray(1)
        try {
            encodedPayload = payload.toByteArray(charset("UTF-8"))
            val message = MqttMessage(encodedPayload)
            message.setRetained(true)
            client.publish(topic, message)
            imqttManagerPublishCallback.onSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            imqttManagerPublishCallback.onFailed(e)
        }
    }

    /**
     * subscribes on the topic
     */
    fun subscribeTopic(topic: String, imqttManagerSubscribeCallback: IMQTTManagerSubscribeCallback) {
        if (!isConnected)
            imqttManagerSubscribeCallback.onFailed(Exception("Broker Not Connected"))
        val qos = 0
        try {
            val subToken = client.subscribe(topic, qos, object : IMqttMessageListener {
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    imqttManagerSubscribeCallback.onData(message!!.payload.toString())
                    message.clearPayload()
                }
            })
            subToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    imqttManagerSubscribeCallback.onSubscribed()
                }

                override fun onFailure(asyncActionToken: IMqttToken,
                                       exception: Throwable) {
                    imqttManagerSubscribeCallback.onFailed(exception)
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


    interface IMQTTManagerConnectCallback {
        fun onConnect(asyncActionToken: IMqttToken)
        fun onConnectFailure(asyncActionToken: IMqttToken, exception: Throwable)
    }

    interface IMQTTManagerPublishCallback {
        fun onSuccess()
        fun onFailed(e: Exception)
    }

    interface IMQTTManagerSubscribeCallback {
        fun onSubscribed()
        fun onData(data: String)
        fun onFailed(e: Throwable)
    }

}