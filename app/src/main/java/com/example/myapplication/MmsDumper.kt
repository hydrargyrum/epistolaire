package com.example.myapplication

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset


class MmsDumper(val contentResolver: ContentResolver) {
    fun getJson(): JSONObject {
        val jobj = JSONObject()
        jobj.put("messages", allMessages())
        return jobj
    }

    fun allMessages(): JSONArray {
        val cursor = contentResolver.query(
            Uri.parse("content://mms-sms/conversations"), arrayOf("thread_id"), null, null, null
            //Uri.parse("content://mms/"), null, "ct_t=\"application/vnd.wap.multipart.related\"", null, null
        )

        val jarray = JSONArray()

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val threadId = cursor.getInt(0)
                jarray.put(getThread(threadId))
            } while (cursor.moveToNext())

            cursor.close()
        }

        return jarray
    }

    fun allMessages2(): JSONArray {
        val cursor = contentResolver.query(
            Uri.parse("content://mms-sms/conversations"), null, null, null, null
            //Uri.parse("content://mms/"), null, "ct_t=\"application/vnd.wap.multipart.related\"", null, null
        )

        val jarray = JSONArray()

        if (cursor != null && cursor.moveToFirst()) {
            do {
                jarray.put(Utils().rowToJson(cursor))
                val last = Utils().rowToJson(cursor)
                val tid = last.getInt("thread_id")
                jarray.put(getThread(tid))
                break
            } while (cursor.moveToNext())

            cursor.close()
        }

        return jarray
    }

    fun getThread(id: Int): JSONArray {
        val jarray = JSONArray()

        val cursor = contentResolver.query(
            Uri.parse("content://sms/"), null,
            "thread_id=$id", null, null
        )

        if (cursor != null && cursor.moveToFirst()) {
            do {
                jarray.put(Utils().rowToJson(cursor))
            } while (cursor.moveToNext())

            cursor.close()
        }

        val cursor2 = contentResolver.query(
            Uri.parse("content://mms/"), null,
            "thread_id=$id", null, null
        )

        if (cursor2 != null && cursor2.moveToFirst()) {
            do {
                val jmms = Utils().rowToJson(cursor2)
                val partId = jmms.getInt("_id")
                jmms.put("parts", getParts(partId))

                jarray.put(jmms)
            } while (cursor2.moveToNext())

            cursor2.close()
        }

        return jarray
    }

    fun getParts(mmsId: Int): JSONArray {
        val jarray = JSONArray()

        val cursor = contentResolver.query(
            Uri.parse("content://mms/part"), null,
            "mid=$mmsId", null, null
        )

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val jpart = Utils().rowToJson(cursor)
                if (jpart.getString("ct").startsWith("image/")) {
                    jpart.put("my_content", getMmsImage(jpart.getInt("_id")))
                } else {
                    jpart.put("my_content", getMmsText(jpart.getInt("_id")))
                }
                jarray.put(jpart)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return jarray
    }

    fun getMmsText2(id: Int): String {
        val partURI = Uri.parse("content://mms/part/$id")
        var stream: InputStream? = null
        val sb = StringBuilder()
        try {
            val stream = contentResolver.openInputStream(partURI)
            if (stream != null) {
                val isr = InputStreamReader(stream, "UTF-8")
                val reader = BufferedReader(isr)
                var temp = reader.readLine()
                while (temp != null) {
                    sb.append(temp)
                    temp = reader.readLine()
                }
            }
        } catch (e: IOException) {
        } finally {
            if (stream != null) {
                try {
                    stream!!.close()
                } catch (e: IOException) {
                }

            }
        }
        return sb.toString()
    }

    fun getMmsText(id: Int): String {
        val partURI = Uri.parse("content://mms/part/$id")
        try {
            val stream = contentResolver.openInputStream(partURI)
            try {
                return stream!!.readBytes().toString(Charset.forName("UTF-8"))
            } finally {
                stream!!.close()
            }
        } catch (e: IOException) {
            return ""
        }
    }

    fun getMmsImage(id: Int): String {
        val partURI = Uri.parse("content://mms/part/$id")
        try {
            val stream = contentResolver.openInputStream(partURI)
            try {
                return Base64.encodeToString(stream!!.readBytes(), Base64.DEFAULT)
            } finally {
                stream!!.close()
            }
        } catch (e: IOException) {
            return ""
        }
    }

    /*fun getSms(id: String): JSONObject {

    }

    fun getMms(id: String): JSONObject {

    }*/
}