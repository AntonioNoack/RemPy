package me.anno.rempy.toml

import me.anno.rempy.vm.SaveState

object TomlWriter {
    fun write(s: SaveState) = buildString {
        append("index=${s.index}\n")
        append("[variables]\n")
        s.variables.forEach { (k,v) -> append("$k=\"$v\"\n") }
        append("\n[scene]\n")
        append("background=\"${s.background?.source ?: ""}\"\n")
        append("shownImages=[${s.shownImages.joinToString(","){ "\"${it.source}\"" }}]\n")
        append("currentText=\"${s.currentText ?: ""}\"\n")
        s.currentMenu?.let {
            append("[menu]\n")
            append("selected=${it.selected}\n")
            append("choices=[\n")
            it.choices.forEach { c -> append("{ text=\"${c.text}\", target=\"${c.target}\" },\n") }
            append("]\n")
        }
    }
}
