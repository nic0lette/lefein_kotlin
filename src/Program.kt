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

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import java.io.File

fun main(args: Array<String>) {
    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()
    val config = gson.fromJson(File("data/Config.json").readText(), Config::class.java)

    println("Hex: ${"80".toInt(16)}")

    val vanillaRom = Rom(File("../ff-vol.nes").readBytes(), config)
    println("Vanilla: ${vanillaRom.mapper} : Randomized? ${vanillaRom.isRandomized}")
    vanillaRom.test()
}

class Rom(initContents: ByteArray, private val config: Config) {
    private val contents = initContents.toUByteArray()

    val mapper get() = (((contents[6].toInt() shr 4) and 0x0f) or (contents[7].toInt() and 0xf0)) and 0x0ff

    /**
     * The Final Fantasy Randomizer upgrades from MMC1 to MMC3, so we can tell if a ROM has been randomized
     * by looking at which mapper it's using.
     */
    val isRandomized get() = mapper == 4

    fun test() {
        val magicNamesOffset = config.dataPointer["magic_names"] ?: return
        val magicDataOffset = config.dataPointer["magic_data"] ?: return
        val magicDataSize = config.dataSize["magic_data"] ?: return

        val magicData = contents.copyOfRange(magicDataOffset, magicDataOffset + (64 * magicDataSize))
        fetchStrings(magicNamesOffset, 64).forEachIndexed { index, name ->
            val offset = (index * magicDataSize) + magicDataOffset

            val magic = MagicSpell.fromBytes(name, contents.copyOfRange(offset, offset + magicDataSize))
            println("${magic.name} : ${magic.hitRate.toString(16)} ${magic.effectivity}")
        }
    }

    private fun fetchStrings(offset: Int, count: Int = 1): List<String> {
        val stringsList = mutableListOf<String>()

        var start = offset
        var index = start

        // Because Kotlin for loops are inclusive, start at 1 (since the index doesn't really matter to us anyway)
        for (i in 1..count) {
            start = index

            while (contents[index] != NULL_BYTE) ++index
            stringsList += config.decodeText(contents.copyOfRange(start, index))
            ++index
        }
        return stringsList
    }
}

data class MagicSpell(
    val name: String,
    val hitRate: UByte,
    val effectivity: UByte,
    val element: UByte,
    val target: UByte,
    val effect: UByte,
    val graphic: UByte,
    val palette: UByte,
    val unused: UByte
) {

    companion object {
        fun fromBytes(name: String, byteData: UByteArray): MagicSpell =
            MagicSpell(
                name,
                byteData[0],
                byteData[1],
                byteData[2],
                byteData[3],
                byteData[4],
                byteData[5],
                byteData[6],
                byteData[7]
            )
    }
}