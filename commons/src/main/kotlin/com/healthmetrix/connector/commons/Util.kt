package com.healthmetrix.connector.commons

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

val Any.logger: Logger
    get() = LoggerFactory.getLogger(javaClass)

annotation class NoArg

annotation class AllOpen

fun ByteArray.encodeHex(): String = joinToString("") { "%02x".format(it) }

fun String.decodeHex(): ByteArray = chunked(2).map {
    it.toInt(16).toByte()
}.toByteArray()

fun ByteArray.encodeBase64(): B64String =
    B64String(Base64.getEncoder().encodeToString(this))

fun String.decodeBase64(): ByteArray? = try {
    Base64.getDecoder().decode(this)
} catch (ex: IllegalArgumentException) {
    logger.info("Failed decoding Base64 string", ex)
    null
}

fun String.encodeURL(): String = URLEncoder.encode(this, "UTF-8")

fun String.decodeURL(): String = URLDecoder.decode(this, "UTF-8")
