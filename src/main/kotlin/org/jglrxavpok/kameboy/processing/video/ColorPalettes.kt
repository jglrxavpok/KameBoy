package org.jglrxavpok.kameboy.processing.video


abstract class ColorPalette {
    abstract fun color(index: Int): Long
    abstract val name: String

    operator fun invoke(index: Int) = color(index)

    override fun toString(): String {
        return name
    }
}

val DefaultPalette = object: ColorPalette() {
    override fun color(index: Int) = when(index) {
        0 -> 0xFFFFFFFF
        1 -> 0xFFAAAAAA
        2 -> 0xFF555555
        3 -> 0xFF000000
        else -> error("Invalid color index: $index")
    }

    override val name = "Emulator default"
}

// https://lospec.com/palette-list/pokemon-sgb
val PokemonSGB = object: ColorPalette() {
    override fun color(index: Int) = when(index) {
        0 -> 0xFFFFEFFF
        1 -> 0xFFF7B58C
        2 -> 0xFF84739C
        3 -> 0xFF181010
        else -> error("Invalid color index: $index")
    }

    override val name = "Pokemon Super GameBoy"
}

// https://lospec.com/palette-list/2-bit-grayscale
val GrayscalePalette = object: ColorPalette() {
    override fun color(index: Int) = when(index) {
        0 -> 0xFFFFFFFF
        1 -> 0xFFB6B6B6
        2 -> 0xFF676767
        3 -> 0xFF000000
        else -> error("Invalid color index: $index")
    }

    override val name = "Grayscale"
}

// https://lospec.com/palette-list/kirokaze-gameboy
val KirokazePalette = object: ColorPalette() {
    override fun color(index: Int) = when(index) {
        0 -> 0xFFE2F3E4
        1 -> 0xFF94E344
        2 -> 0xFF46878F
        3 -> 0xFF332C50
        else -> error("Invalid color index: $index")
    }

    override val name = "Kirokaze's Palette"
}

// https://lospec.com/palette-list/links-awakening-sgb
val LinksAwakeningPalette = object: ColorPalette() {
    override fun color(index: Int) = when(index) {
        0 -> 0xFFFFFFB5
        1 -> 0xFF7BC67B
        2 -> 0xFF6B8C42
        3 -> 0xFF5A3921
        else -> error("Invalid color index: $index")
    }

    override val name = "Link's Awakening"
}

val Palettes = arrayOf(DefaultPalette, PokemonSGB, GrayscalePalette, KirokazePalette, LinksAwakeningPalette)
