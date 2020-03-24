package re.indigo.epistolaire

import android.util.JsonWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream


class JsonObjectWriter(val writer: JsonWriter) {
    private fun dump(obj: Any) {
        when (obj) {
            JSONObject.NULL -> writer.nullValue()
            is Boolean -> writer.value(obj)
            is Number -> writer.value(obj)
            is String -> writer.value(obj)
            is JSONArray -> dump(obj)
            is JSONObject -> dump(obj)
        }
    }

    fun dump(obj: JSONObject) {
        writer.beginObject()
        for (key in obj.keys()) {
            writer.name(key)
            dump(obj.get(key))
        }
        writer.endObject()
    }

    fun dump(arr: JSONArray) {
        writer.beginArray()
        for (i in 0 until arr.length()) {
            dump(arr.get(i))
        }
        writer.endArray()
    }
}
