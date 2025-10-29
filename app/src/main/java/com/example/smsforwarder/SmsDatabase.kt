package com.example.smsforwarder

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class SmsDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "SmsDatabase.db"
        const val TABLE_PENDING_SMS = "pending_sms"
        const val COLUMN_ID = "id"
        const val COLUMN_SENDER = "sender"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SERVICE_CENTER = "service_center"
        const val COLUMN_PROTOCOL_ID = "protocol_id"
        const val COLUMN_STATUS = "status"
        const val COLUMN_INDEX_ON_ICC = "index_on_icc"

        @Volatile
        private var INSTANCE: SmsDatabase? = null

        fun getInstance(context: Context): SmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = SmsDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_PENDING_SMS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY,"
                + "$COLUMN_SENDER TEXT,"
                + "$COLUMN_MESSAGE TEXT,"
                + "$COLUMN_TIMESTAMP INTEGER,"
                + "$COLUMN_SERVICE_CENTER TEXT,"
                + "$COLUMN_PROTOCOL_ID INTEGER,"
                + "$COLUMN_STATUS INTEGER,"
                + "$COLUMN_INDEX_ON_ICC INTEGER)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_SMS")
        onCreate(db)
    }

    @Synchronized
    fun addSms(sms: SmsData) {
        val values = ContentValues()
        values.put(COLUMN_ID, sms.id)
        values.put(COLUMN_SENDER, sms.sender)
        values.put(COLUMN_MESSAGE, sms.message)
        values.put(COLUMN_TIMESTAMP, sms.timestamp)
        values.put(COLUMN_SERVICE_CENTER, sms.serviceCenterAddress)
        values.put(COLUMN_PROTOCOL_ID, sms.protocolIdentifier)
        values.put(COLUMN_STATUS, sms.status)
        values.put(COLUMN_INDEX_ON_ICC, sms.indexOnIcc)
        val db = this.writableDatabase
        db.insert(TABLE_PENDING_SMS, null, values)
        Log.d("SmsDatabase", "SMS from ${sms.sender} added to database")
    }

    @Synchronized
    fun getAllPendingSms(): List<SmsData> {
        val smsList = ArrayList<SmsData>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PENDING_SMS", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val sender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER))
                val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                val serviceCenter = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SERVICE_CENTER))
                val protocolId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROTOCOL_ID))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STATUS))
                val indexOnIcc = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INDEX_ON_ICC))
                smsList.add(SmsData(id, sender, message, timestamp, serviceCenter, protocolId, status, indexOnIcc))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return smsList
    }

    @Synchronized
    fun deleteSms(ids: List<Long>) {
        if (ids.isEmpty()) {
            return
        }
        val db = this.writableDatabase
        val idsString = ids.joinToString(",")
        db.execSQL("DELETE FROM $TABLE_PENDING_SMS WHERE $COLUMN_ID IN ($idsString)")
        Log.d("SmsDatabase", "${ids.size} SMS deleted from database")
    }

    @Synchronized
    fun getCount(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_PENDING_SMS", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }
}
