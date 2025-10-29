package com.example.smsforwarder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SmsWorkerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var idleAttempts = 0

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "SmsForwarderChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        Log.d("SmsWorkerService", "Service started")

        scope.launch {
            val sharedPreferences = getSharedPreferences("sms_forwarder_prefs", MODE_PRIVATE)
            val waitPeriod = sharedPreferences.getString("workerThreadWaitPeriod", "5")?.toLongOrNull() ?: 5L
            val maxIdleAttempts = sharedPreferences.getString("workerThreadIdealAttemptsCount", "33")?.toIntOrNull() ?: 33

            while (isActive) {
                val db = SmsDatabase.getInstance(applicationContext)
                val pendingSms = db.getAllPendingSms()

                if (pendingSms.isNotEmpty()) {
                    idleAttempts = 0
                    val success = sendSmsToApi(pendingSms)
                    if (success) {
                        db.deleteSms(pendingSms.map { it.id })
                    }
                } else {
                    idleAttempts++
                    Log.d("SmsWorkerService", "No pending SMS, idle attempt: $idleAttempts")
                    if (idleAttempts >= maxIdleAttempts) {
                        Log.d("SmsWorkerService", "Max idle attempts reached, stopping service")
                        stopSelf()
                        break
                    }
                }
                delay(waitPeriod * 1000)
            }
        }
        return START_STICKY
    }

    private suspend fun sendSmsToApi(smsList: List<SmsData>): Boolean {
        val sharedPreferences = getSharedPreferences("sms_forwarder_prefs", MODE_PRIVATE)
        val apiEndpoint = sharedPreferences.getString("apiEndpoint", null)
        val username = sharedPreferences.getString("username", null)
        val password = sharedPreferences.getString("password", null)

        if (apiEndpoint.isNullOrEmpty()) {
            Log.e("SmsWorkerService", "API endpoint is not set")
            return false
        }

        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(apiEndpoint)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                    val credentials = "$username:$password"
                    val auth = "Basic ${Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)}"
                    connection.setRequestProperty("Authorization", auth)
                }
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true

                val jsonArray = JSONArray()
                smsList.forEach { jsonArray.put(it.toJson()) }
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(jsonArray.toString())
                outputStream.flush()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("SmsWorkerService", "Successfully sent ${smsList.size} SMS to API")
                    true
                } else {
                    Log.e("SmsWorkerService", "Failed to send SMS to API, response code: $responseCode")
                    false
                }
            } catch (e: Exception) {
                Log.e("SmsWorkerService", "Error sending SMS to API", e)
                false
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Sms Forwarder Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Sms Forwarder")
            .setContentText("Forwarding SMS messages in the background")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Log.d("SmsWorkerService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
