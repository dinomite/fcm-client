package net.dinomite.fcm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.io.Resources
import java.io.IOException

abstract class DataClassTest {
    val objectMapper = ObjectMapper()

    init {
        objectMapper.registerModule(KotlinModule())
        objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    }

    fun fixture(filename: String): String {
        try {
            return Resources.toString(Resources.getResource(filename), Charsets.UTF_8).trim { it <= ' ' }
        } catch (e: IOException) {
            throw IllegalArgumentException(e)
        }

    }
}