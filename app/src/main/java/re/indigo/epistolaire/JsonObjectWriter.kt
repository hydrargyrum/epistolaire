package re.indigo.epistolaire

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.util.*


class JsonObjectWriter(val output: OutputStream) {
    private abstract class State {
        var first = true
    }
    private class EmptyState : State()
    private class ArrayState : State()
    private class ObjectState : State() {
        var key = true
    }

    private val stack = Stack<State>()
    init {
        stack.push(EmptyState())
    }

    private fun write(s: String) {
        output.write(s.toByteArray())
    }

    private fun rawDump(n: Number) {
        write(JSONObject.numberToString(n))
    }

    private fun rawDump(b: Boolean) {
        write(if (b) "true" else "false")
    }

    private fun rawDump(s: String) {
        write(JSONObject.quote(s))
    }

    private fun dumpValue(obj: Any) {
        when (obj) {
            JSONObject.NULL -> {
                beginValue()
                write("null")
            }
            is Boolean -> {
                beginValue()
                rawDump(obj)
            }
            is Number -> {
                beginValue()
                rawDump(obj)
            }
            is String -> {
                beginValue()
                rawDump(obj)
            }
            is JSONArray -> dumpValue(obj)
            is JSONObject -> dumpValue(obj)
        }
    }

    private fun beginKey() {
        when (val state = stack.peek()) {
            is EmptyState -> throw Exception("No object is open")
            is ArrayState -> throw Exception("No object is open")
            is ObjectState -> {
                if (!state.key) {
                    throw Exception("Expecting a value, not a key")
                }

                if (state.first) {
                    state.first = false
                } else {
                    write(",")
                }

                state.key = false
            }
        }
    }

    private fun beginValue() {
        when (val state = stack.peek()) {
            is EmptyState -> {
                if (!state.first) {
                    throw Exception("There can be only one top-level value")
                }
                state.first = false
            }
            is ArrayState -> {
                if (state.first) {
                    state.first = false
                } else {
                    write(",")
                }
            }

            is ObjectState -> {
                if (state.key) {
                    throw Exception("Excepting a key, not a value")
                }

                write(":")
                state.key = true
            }
        }
    }

    fun dumpValue(obj: JSONObject) {
        beginObject()
        for (key in obj.keys()) {
            beginKey()
            rawDump(key)
            dumpValue(obj.get(key))
        }
        endObject()
    }

    fun dumpValue(arr: JSONArray) {
        beginArray()
        for (i in 0 until arr.length()) {
            dumpValue(arr.get(i))
        }
        endArray()
    }

    fun beginArray() {
        beginValue()
        write("[")
        stack.push(ArrayState())
    }

    fun endArray() {
        when (stack.peek()) {
            is EmptyState -> throw Exception("No array is currently open")
            is ObjectState -> throw Exception("No array is currently open")
        }

        stack.pop()
        write("]")
    }

    fun beginObject() {
        beginValue()
        write("{")
        stack.push(ObjectState())
    }

    fun dumpKey(key: String) {
        when (val state = stack.peek()) {
            is EmptyState -> throw Exception()
            is ArrayState -> throw Exception()
            is ObjectState -> {
                if (!state.key) {
                    throw Exception()
                }
                beginKey()
                rawDump(key)
            }
        }
    }

    fun endObject() {
        when (stack.peek()) {
            is EmptyState -> throw Exception("No object is currently open")
            is ArrayState -> throw Exception("No object is currently open")
        }

        stack.pop()
        write("}")
    }

    fun close() {
        output.close()

        when (stack.peek()) {
            is ArrayState -> throw Exception("An array is still open")
            is ObjectState -> throw Exception("An object is still open")
        }
    }
}
