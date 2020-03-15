/*
 This is free and unencumbered software released into the public domain.
 See LICENSE file for details.
 */

package re.indigo.epistolaire

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset


class MmsDumper(val contentResolver: ContentResolver) {
    private val TAG = "MmsDumper"

    val errors = JSONArray()

    fun getJson(): JSONObject {
        val jobj = JSONObject()

        jobj.put("conversations", allMessages())
        jobj.put("errors", errors)
        return jobj
    }

    fun allMessages(): JSONArray {
        val uri = Uri.parse("content://mms-sms/conversations")
        val cursor = contentResolver.query(
            uri, arrayOf("thread_id"), null, null, null
            //Uri.parse("content://mms/"), null, "ct_t=\"application/vnd.wap.multipart.related\"", null, null
        )

        val jarray = JSONArray()

        if (cursor == null) {
            val msg = "failed to list conversations $uri"
            Log.e(TAG, msg)
            errors.put(msg)
        } else if (cursor.moveToFirst()) {
            do {
                val threadId = cursor.getInt(0)
                jarray.put(getThread(threadId))
            } while (cursor.moveToNext())

            cursor.close()
        }

        return jarray
    }

    fun forceMillisDate(message: JSONObject, field: String) {
        /* sometimes the sms are in millis and the mms in secs... */
        val value = message.optLong(field)
        if (value != 0L && value < 500000000000L) {
            message.put(field, value * 1000)
        }
    }

    fun getThread(id: Int): JSONArray {
        val jarray = JSONArray()

        var uri = Uri.parse("content://sms/")
        var cursor = contentResolver.query(
            uri, null,
            "thread_id=$id", null, null
        )

        if (cursor == null) {
            val msg = "failed to list sms in thread $uri $id"
            Log.e(TAG, msg)
            errors.put(msg)
        } else if (cursor.moveToFirst()) {
            do {
                val jobj = Utils().rowToJson(cursor)

                forceMillisDate(jobj, "date")
                forceMillisDate(jobj, "date_sent")

                jarray.put(jobj)
            } while (cursor.moveToNext())

            cursor.close()
        }

        uri = Uri.parse("content://mms/")
        cursor = contentResolver.query(
            uri, null,
            "thread_id=$id", null, null
        )

        if (cursor == null) {
            val msg = "failed to list mms in thread $uri $id"
            Log.e(TAG, msg)
            errors.put(msg)
        } else if (cursor.moveToFirst()) {
            do {
                val jmms = Utils().rowToJson(cursor)

                forceMillisDate(jmms, "date")
                forceMillisDate(jmms, "date_sent")

                val partId = jmms.getInt("_id")
                jmms.put("parts", getParts(partId))

                jmms.put("addresses", getMMSAddresses(jmms.getInt("_id")))

                jarray.put(jmms)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return jarray
    }

    fun getParts(mmsId: Int): JSONArray {
        val jarray = JSONArray()

        val uri = Uri.parse("content://mms/part")
        val cursor = contentResolver.query(
            uri, null,
            "mid=$mmsId", null, null
        )

        if (cursor == null) {
            val msg = "failed listing parts of mms $uri $mmsId"
            Log.e(TAG, msg)
            errors.put(msg)
        } else if (cursor.moveToFirst()) {
            do {
                val jpart = Utils().rowToJson(cursor)

                val hasTextValue = (jpart.has("text") && jpart.get("text") is String)

                if (hasTextValue) {
                    jpart.put("my_content", "")
                } else if (jpart.getString("ct").startsWith("image/")) {
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

    fun getMmsText(id: Int): String {
        val partURI = Uri.parse("content://mms/part/$id")
        try {
            val stream = contentResolver.openInputStream(partURI)

            if (stream == null) {
                val msg = "failed opening stream for mms text part $partURI"
                Log.e(TAG, msg)
                errors.put(msg)
                return ""
            }

            stream.use {
                return stream.readBytes().toString(Charset.forName("UTF-8"))
            }
        } catch (e: IOException) {
            val msg = "failed to read MMS text on $partURI"
            Log.e(TAG, msg, e)
            errors.put("$msg: $e")

            return ""
        }
    }

    fun getMmsImage(id: Int): String {
        val partURI = Uri.parse("content://mms/part/$id")
        try {
            val stream = contentResolver.openInputStream(partURI)

            if (stream == null) {
                val msg = "failed opening stream for mms binary part $partURI"
                Log.e(TAG, msg)
                errors.put(msg)

                return ""
            }

            stream.use {
                return Base64.encodeToString(stream.readBytes(), Base64.DEFAULT)
            }
        } catch (e: IOException) {
            val msg = "failed to read MMS part on $partURI"
            Log.e(TAG, msg, e)
            errors.put("$msg: $e")

            return ""
        }
    }

    fun getMMSAddresses(id: Int): JSONArray {
        val jarray = JSONArray()

        /* values come from PduHeaders */
        val type_from = 137
        val type_to = 151
        val type_cc = 130
        val type_bcc = 129

        for (addrtype in arrayOf(type_from, type_to, type_cc, type_bcc)) {
            val addrURI = Uri.parse("content://mms/$id/addr")
            val cursor = contentResolver.query(
                addrURI, arrayOf("address"),
                "type=$addrtype AND msg_id=$id", null, null
            )

            if (cursor == null) {
                val msg = "failed getting addresses on $addrURI"
                Log.e(TAG, msg)
                errors.put(msg)
            } else if (cursor.moveToFirst()) {
                do {
                    jarray.put(cursor.getString(0))
                } while (cursor.moveToNext())

                cursor.close()
            }
        }
        return jarray
    }
}
