package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SMSBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    val db = SmsDatabase.getInstance(context)

                    for (smsMessage in messages) {
                        val smsData = SmsData(
                            id = System.currentTimeMillis(),
                            sender = smsMessage.displayOriginatingAddress,
                            message = smsMessage.messageBody,
                            timestamp = smsMessage.timestampMillis,
                            serviceCenterAddress = smsMessage.serviceCenterAddress,
                            protocolIdentifier = smsMessage.protocolIdentifier,
                            status = smsMessage.status,
                            indexOnIcc = smsMessage.indexOnIcc
                        )
                        db.addSms(smsData)
                        Log.d("SMSBroadcastReceiver", "Received and stored SMS from ${smsData.sender}")
                    }

                    if (messages.isNotEmpty()) {
                        val serviceIntent = Intent(context, SmsWorkerService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SMSBroadcastReceiver", "Error receiving SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
