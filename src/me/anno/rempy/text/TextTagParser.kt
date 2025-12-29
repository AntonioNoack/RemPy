package me.anno.rempy.text

class TextTagParser {
    private val regex = Regex("\\{color=#([0-9A-Fa-f]{6})}(.*?)\\{/color}")

    fun parse(text: String): List<TextSpan> {
        val spans = mutableListOf<TextSpan>()
        var last = 0
        for (m in regex.findAll(text)) {
            if (m.range.first > last)
                spans += TextSpan(text.substring(last, m.range.first), 0xFFFFFFFF.toInt())
            spans += TextSpan(
                m.groupValues[2],
                0xFF000000.toInt() or m.groupValues[1].toInt(16)
            )
            last = m.range.last + 1
        }
        if (last < text.length)
            spans += TextSpan(text.substring(last), 0xFFFFFFFF.toInt())
        return spans
    }
}
