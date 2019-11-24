package re.indigo.epistolaire

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

    fun forceMillisDate(message: JSONObject, field: String) {
        /* sometimes the sms are in millis and the mms in secs... */
        val value = message.optLong(field)
        if (value != 0L && value < 500000000000L) {
            message.put(field, value * 1000)
        }
    }

    fun getThread(id: Int): JSONArray {
        val jarray = JSONArray()

        var cursor = contentResolver.query(
            Uri.parse("content://sms/"), null,
            "thread_id=$id", null, null
        )

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val jobj = Utils().rowToJson(cursor)

                forceMillisDate(jobj, "date")
                forceMillisDate(jobj, "date_sent")

                jarray.put(jobj)
            } while (cursor.moveToNext())

            cursor.close()
        }

        cursor = contentResolver.query(
            Uri.parse("content://mms/"), null,
            "thread_id=$id", null, null
        )

        if (cursor != null && cursor.moveToFirst()) {
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

    fun getMMSAddresses(id: Int): JSONArray {
        val jarray = JSONArray()

        /* values come from PduHeaders */
        val type_from = 137
        val type_to = 151
        val type_cc = 130
        val type_bcc = 129

        for (addrtype in arrayOf(type_from, type_to, type_cc, type_bcc)) {
            val cursor = contentResolver.query(
                Uri.parse("content://mms/$id/addr"), arrayOf("address"),
                "type=$addrtype AND msg_id=$id", null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    jarray.put(cursor.getString(0))
                } while (cursor.moveToNext())

                cursor.close()
            }
        }
        return jarray
    }
}