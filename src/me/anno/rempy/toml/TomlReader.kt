package me.anno.rempy.toml

import me.anno.rempy.animation.Image
import me.anno.rempy.vm.MenuState
import me.anno.rempy.vm.SaveState

object TomlReader {
    fun read(text: String): SaveState {
        val vars = HashMap<String, Any>()
        var index = 0
        var bg: Image? = null
        var shownImages: List<Image> = emptyList()
        var currentText: String? = null
        var menu: MenuState? = null
        var section = ""

        for (l in text.lines()) {
            val line = l.trim(); if (line.isEmpty()) continue
            if (line.startsWith("[")) {
                section = line.trim('[', ']'); continue
            }
            if ('=' !in line) continue
            val (k, v) = line.split("=")
            when (section) {
                "" -> if (k == "index") index = v.toInt()
                "variables" -> vars[k] = v.trim('"')
                "scene" -> when (k) {
                    "background" -> bg = Image.parse(v)
                    "shownImages" -> shownImages = v.trim('[', ']')
                        .split(",")
                        .filter { it.isNotEmpty() }
                        .map { Image.parse(it) }
                    "currentText" -> currentText = v.trim('"')
                }
                "menu" -> if (k == "selected") menu = MenuState(emptyList(), v.toInt())
            }
        }
        return SaveState(index, vars, bg, shownImages, currentText, menu)
    }
}
