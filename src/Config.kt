/*
 * Copyright (C) 2019 Nicole Borrelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.IllegalArgumentException

class Config() {
    lateinit var textTable: Map<String, String>
    private lateinit var dataPointers: Map<String, String>
    private lateinit var structureSizes: Map<String, Int>

    val dataPointer = object: HashMap<String, Int>() {
        override operator fun get(key: String): Int? =
                dataPointers[key]?.asInt()
    }
    val dataSize get() = structureSizes

    private val byteToChar: Map<UByte, String> by lazy {
        HashMap<UByte, String>().also {
            textTable.forEach { key, value ->
                val keyValue = key.toUByte(16)
                it[keyValue] = value
            }
        }
    }

    private val charsToByte: Map<String, UByte> by lazy {
        HashMap<String, UByte>().also {
            textTable.forEach { key, value ->
                val keyValue = key.toUByte(16)
                if (it[value] == null) {
                    it[value] = keyValue
                }
            }
        }
    }

    fun encodeText(text: String): UByteArray {

        // At worst the bytes returned will be the same length as the string (with a trailing null)
        val bytes = UByteArray(text.length + 1)
        var index = 0
        var byteIndex = 0

        while (index < text.length) {
            val twoChars: UByte? = if (index + 1 < text.length) {
                charsToByte[text.substring(index, index + 2)]
            } else {
                null
            }

            if (twoChars != null) {
                bytes[byteIndex] = twoChars
                index++
            } else {
                val encodedByte = charsToByte[text[index].toString()]
                    ?: throw IllegalArgumentException("No encoding found for character '${text[index]}'")
                bytes[byteIndex] = encodedByte
            }

            byteIndex++
            index++
        }

        bytes[byteIndex] = NULL_BYTE
        return bytes.copyOfRange(0, byteIndex + 1)
    }

    fun decodeText(bytes: UByteArray) =
        bytes.fold("") { acc: String, byte: UByte ->
            if (byte == NULL_BYTE) return@fold acc
            acc + byteToChar[byte]
        }
}

/**
 * Both Java and Kotlin have a thing where they can't deal with '0x' at the front of hex numbers, despite that
 * this is how almost everyone writes hex numbers. So we'll just write our extension which converts numbers
 * in a nice way.
 */
fun String.asInt():Int =
        when {
            startsWith("0x") -> substring(2).toInt(16)
            startsWith("0") -> substring(1).toInt(8)
            else -> toInt()
        }

val NULL_BYTE: UByte = 0.toUByte()
