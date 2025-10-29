package com.example.smsforwarder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SmsWorkerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var db: SmsDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private var idleCounter = 0

    override fun onCreate() {
        super.onCreate()
        db = SmsDatabase.getInstance(this)
        sharedPreferences = getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        scope.launch {
            workerLoop()
        }
        return START_STICKY
    }

    private suspend fun workerLoop() {
        val workerPeriod = sharedPreferences.getInt("worker_period", 5) * 1000L
        val idleAttempts = sharedPreferences.getInt("idle_attempts", 33)

        while (idleCounter < idleAttempts) {
            val pendingSms = db.getAllPendingSms()
            if (pendingSms.isNotEmpty()) {
                idleCounter = 0
                sendSmsToApi(pendingSms)
            } else {
                idleCounter++
                Log.d("SmsWorkerService", "No pending SMS. Idle counter: $idleCounter")
            }
            delay(workerPeriod)
        }
        stopSelf()
    }

    private suspend fun sendSmsToApi(smsList: List<SmsData>) {
        val apiUrl = sharedPreferences.getString("api_endpoint", "")
        if (apiUrl.isNullOrEmpty()) {
            Log.e("SmsWorkerService", "API endpoint is not set.")
            return
        }

        val username = sharedPreferences.getString("username", "") ?: ""
        val password = sharedPreferences.getString("password", "") ?: ""
        val credentials = "$username:$password"
        val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val jsonArray = JSONArray()
        smsList.forEach {
            jsonArray.put(it.toJson())
        }
        val jsonPayload = jsonArray.toString()

        withContext(Dispatchers.IO) {
            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", basicAuth)
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonPayload)
                writer.flush()
                writer.close()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val idsToDelete = smsList.map { it.id }
                    db.deleteSms(idsToDelete)
                    Log.d("SmsWorkerService", "Successfully sent ${smsList.size} SMS to API.")
                } else {
                    Log.e("SmsWorkerService", "Failed to send SMS to API. Response code: ${connection.responseCode}")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("SmsWorkerService", "Error sending SMS to API", e)
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "sms_forwarder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SMS Forwarder", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("SMS Forwarder")
                .setContentText("Forwarding SMS messages...")
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("SMS Forwarder")
                .setContentText("Forwarding SMS messages...")
                .build()
        }
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
