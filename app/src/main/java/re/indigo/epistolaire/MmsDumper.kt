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
import java.io.InputStream
import java.nio.charset.Charset


class MmsDumper(val contentResolver: ContentResolver) {
    private val TAG = "EpistolaireMmsDumper"

    val errors = JSONArray()

    private fun countRows(uriString: String): Int {
        val uri = Uri.parse(uriString)
        val cursor = contentResolver.query(
            uri, null, null, null, null
        )
        if (cursor == null) {
            return 0
        }
        cursor.use {
            return cursor.count
        }
    }

    fun countAllMessages(): Int {
        return countRows("content://sms/") + countRows("content://mms/")
    }

    fun conversations(): ArrayList<Int> {
        val ret = ArrayList<Int>()

        val uri = Uri.parse("content://mms-sms/conversations?simple=true")
        val cursor = contentResolver.query(
            uri, arrayOf("_id"), null, null, null
        )

        if (cursor == null) {
            val msg = "failed to list conversations $uri"
            Log.e(TAG, msg)
            // errors
        } else {
            cursor.use {
                while (cursor.moveToNext()) {
                    val threadId = cursor.getInt(0)
                    ret.add(threadId)
                }
            }
        }
        return ret
    }

    private fun foreachMessageByType(uriString: String, id: Int, block: (JSONObject) -> Unit) {
        val uri = Uri.parse(uriString)
        val cursor = contentResolver.query(
            uri, null,
            "thread_id=$id", null, null
        )

        if (cursor == null) {
            val msg = "failed to list mms in thread $uri $id"
            Log.e(TAG, msg)
        } else {
            while (cursor.moveToNext()) {
                val jobj = Utils().rowToJson(cursor)

                forceMillisDate(jobj, "date")
                forceMillisDate(jobj, "date_sent")

                block(jobj)
            }
        }
    }

    fun foreachThreadMessage(id: Int, block: (JSONObject) -> Unit) {
        foreachMessageByType("content://sms/", id, block)
        foreachMessageByType("content://mms/", id) { jmms ->
            val partId = jmms.getInt("_id")
            jmms.put("parts", getParts(partId))

            jmms.put("addresses", getMMSAddresses(jmms.getInt("_id")))
            block(jmms)
        }
    }

    fun forceMillisDate(message: JSONObject, field: String) {
        /* sometimes the sms are in millis and the mms in secs... */
        val value = message.optLong(field)
        if (value != 0L && value < 500000000000L) {
            message.put(field, value * 1000)
        }
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
                } else if (jpart.getString("ct").startsWith("text/")) {
                    jpart.put("my_content", usePart(jpart.getInt("_id")) { stream ->
                        stream.readBytes().toString(Charset.forName("UTF-8"))
                    })
                } else {
                    jpart.put("my_content", usePart(jpart.getInt("_id")) { stream ->
                        Base64.encodeToString(stream.readBytes(), Base64.NO_WRAP)
                    })
                }
                jarray.put(jpart)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return jarray
    }

    fun usePart(id: Int, block: (InputStream) -> String): String {
        val partURI = Uri.parse("content://mms/part/$id")
        try {
            val stream = contentResolver.openInputStream(partURI)

            if (stream == null) {
                val msg = "failed opening stream for mms part $partURI"
                Log.e(TAG, msg)
                errors.put(msg)
                return ""
            }

            stream.use {
                return block(stream)
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

        val addrURI = Uri.parse("content://mms/$id/addr")
        val cursor = contentResolver.query(
            addrURI, arrayOf("address", "type"),
            "msg_id=$id", null, null
        )

        if (cursor == null) {
            val msg = "failed getting addresses on $addrURI"
            Log.e(TAG, msg)
            errors.put(msg)
        } else {
            while (cursor.moveToNext()) {
                when (cursor.getInt(1)) {
                    type_from, type_to, type_cc, type_bcc -> jarray.put(cursor.getString(0))
                }
            }

            cursor.close()
        }
        return jarray
    }
}
