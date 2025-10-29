package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SMSBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val db = SmsDatabase.getInstance(context)

            messages.forEach { sms ->
                val smsData = SmsData(
                    id = System.currentTimeMillis(),
                    sender = sms.originatingAddress ?: "Unknown",
                    message = sms.messageBody,
                    timestamp = sms.timestampMillis,
                    serviceCenterAddress = sms.serviceCenterAddress,
                    protocolIdentifier = sms.protocolIdentifier,
                    status = sms.status,
                    indexOnIcc = sms.indexOnIcc
                )
                db.addSms(smsData)
                Log.d("SMSBroadcastReceiver", "SMS received from ${smsData.sender} and stored in the database.")
            }

            // Start the worker service
            val serviceIntent = Intent(context, SmsWorkerService::class.java)
            context.startService(serviceIntent)
        }
    }
}
