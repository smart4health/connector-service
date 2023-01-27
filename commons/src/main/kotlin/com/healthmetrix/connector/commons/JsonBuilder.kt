package com.healthmetrix.connector.commons

import org.json.JSONArray
import org.json.JSONObject

/**
 * allows for things such as:
 * array {
 *  json {
 *      "key" to "value"
 *      "otherkey" to json {
 *          "nested" to "objects"
 *      }
 *  }
 * }
 *
 * and makes the mailjet requests a lot easier to read
 */

fun json(builder: JsonObjectBuilder.() -> Unit) = with(
    JsonObjectBuilder(),
) {
    builder()
    json
}

fun array(builder: JsonArrayBuilder.() -> Unit) = with(
    JsonArrayBuilder(),
) {
    builder()
    json
}

class JsonObjectBuilder {
    val json = JSONObject()

    infix fun <T> String.to(value: T) {
        json.put(this, value)
    }

    fun json(builder: JsonObjectBuilder.() -> Unit) =
        com.healthmetrix.connector.commons.json(builder)
}

class JsonArrayBuilder {
    val json = JSONArray()

    fun json(builder: JsonObjectBuilder.() -> Unit) {
        json.put(com.healthmetrix.connector.commons.json(builder))
    }

    operator fun Any.unaryPlus() {
        json.put(this)
    }
}
