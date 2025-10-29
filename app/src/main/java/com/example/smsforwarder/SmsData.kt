package com.example.smsforwarder

import org.json.JSONObject

data class SmsData(
    val id: Long,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val serviceCenterAddress: String?,
    val protocolIdentifier: Int,
    val status: Int,
    val indexOnIcc: Int
) {
    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("sender", sender)
        jsonObject.put("message", message)
        jsonObject.put("timestamp", timestamp)
        jsonObject.put("serviceCenterAddress", serviceCenterAddress)
        jsonObject.put("protocolIdentifier", protocolIdentifier)
        jsonObject.put("status", status)
        jsonObject.put("indexOnIcc", indexOnIcc)
        return jsonObject
    }
}
