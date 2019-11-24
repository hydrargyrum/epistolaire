package com.example.myapplication

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

class SmsDumper(val contentResolver: ContentResolver) {
    fun getJson(): JSONObject {
        val jobj = JSONObject()
        jobj.put("messages", allMessages())
        return jobj
    }

    fun allMessages(): JSONArray {
        return messagesInPath("")
    }

    fun messagesInPath(path: String): JSONArray {
        val cursor = contentResolver.query(
            Uri.parse("content://sms/$path"), null, null, null, null
        )

        val jarray = JSONArray()

        if (cursor != null && cursor.moveToFirst()) {
            do {
                jarray.put(Utils().rowToJson(cursor))
            } while (cursor.moveToNext())

            cursor.close()
        }

        return jarray
    }

    fun getColumnString(cursor: Cursor, name: String): String {
        val id = cursor.getColumnIndex(name)
        return cursor.getString(id)
    }
}