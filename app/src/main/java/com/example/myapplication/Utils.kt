package com.example.myapplication

import android.database.Cursor
import org.json.JSONArray
import org.json.JSONObject

class Utils {
    fun rowToJson(cursor: Cursor): JSONObject {
        val res = JSONObject()

        for (i in 0 until cursor.columnCount) {
            val key = cursor.getColumnName(i)

            when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_INTEGER -> res.put(key, cursor.getInt(i))
                Cursor.FIELD_TYPE_FLOAT -> res.put(key, cursor.getFloat(i))
                Cursor.FIELD_TYPE_STRING -> res.put(key, cursor.getString(i))
                Cursor.FIELD_TYPE_NULL -> res.put(key, JSONObject.NULL)
            }
        }
        return res
    }

    fun concatArray(out: JSONArray, in_: JSONArray) {
        for (i in 0 until in_.length()) {
            out.put(in_.get(i))
        }
    }
}