package re.indigo.epistolaire

import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream

/*
 JSONObject.toString() can cause OutOfMemory errors when the data is big.
 Stream output instead.
 */
class JsonWriter(val file: FileOutputStream) {
    private fun write(s: String) {
        file.write(s.toByteArray())
    }

    fun dump(obj: Any) {
        when (obj) {
            JSONObject.NULL -> write("null")
            is Boolean -> dump(obj)
            is Number -> dump(obj)
            is String -> dump(obj)
            is JSONArray -> dump(obj)
            is JSONObject -> dump(obj)
        }
    }

    fun dump(obj: JSONObject) {
        var first = true
        write("{")
        for (key in obj.keys()) {
            if (!first) {
                write(",")
            }
            first = false

            dump(key)
            write(":")
            dump(obj.get(key))
        }
        write("}")
    }

    fun dump(arr: JSONArray) {
        var first = true
        write("[")
        for (i in 0 until arr.length()) {
            if (!first) {
                write(",")
            }
            first = false

            dump(arr.get(i))
        }
        write("]")
    }

    fun dump(s: String) {
        write(JSONObject.quote(s))
    }

    fun dump(b: Boolean) {
        if (b) {
            write("true")
        } else {
            write("false")
        }
    }

    fun dump(i: Long) {
        write(JSONObject.numberToString(i))
    }

    fun dump(i: Double) {
        write(JSONObject.numberToString(i))
    }

    fun dump(i: Number) {
        write(JSONObject.numberToString(i))
    }
}
