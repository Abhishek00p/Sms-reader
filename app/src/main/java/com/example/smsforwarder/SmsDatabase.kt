package com.example.smsforwarder

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class SmsDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "sms_forwarder.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "pending_sms"
        private const val COLUMN_ID = "id"
        private const val COLUMN_SENDER = "sender"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_SERVICE_CENTER = "service_center"
        private const val COLUMN_PROTOCOL_ID = "protocol_id"
        private const val COLUMN_STATUS = "status"
        private const val COLUMN_INDEX_ON_ICC = "index_on_icc"

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
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY," +
                "$COLUMN_SENDER TEXT," +
                "$COLUMN_MESSAGE TEXT," +
                "$COLUMN_TIMESTAMP INTEGER," +
                "$COLUMN_SERVICE_CENTER TEXT," +
                "$COLUMN_PROTOCOL_ID INTEGER," +
                "$COLUMN_STATUS INTEGER," +
                "$COLUMN_INDEX_ON_ICC INTEGER)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    @Synchronized
    fun addSms(sms: SmsData) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, sms.id)
            put(COLUMN_SENDER, sms.sender)
            put(COLUMN_MESSAGE, sms.message)
            put(COLUMN_TIMESTAMP, sms.timestamp)
            put(COLUMN_SERVICE_CENTER, sms.serviceCenterAddress)
            put(COLUMN_PROTOCOL_ID, sms.protocolIdentifier)
            put(COLUMN_STATUS, sms.status)
            put(COLUMN_INDEX_ON_ICC, sms.indexOnIcc)
        }
        db.insert(TABLE_NAME, null, values)
        Log.d("SmsDatabase", "SMS from ${sms.sender} added to the database.")
    }

    @Synchronized
    fun getAllPendingSms(): List<SmsData> {
        val smsList = mutableListOf<SmsData>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
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
        val db = writableDatabase
        val idsString = ids.joinToString(",")
        db.delete(TABLE_NAME, "$COLUMN_ID IN ($idsString)", null)
        Log.d("SmsDatabase", "${ids.size} SMS records deleted from the database.")
    }

    @Synchronized
    fun getCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return count
    }
}
