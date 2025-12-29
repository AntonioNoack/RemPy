package me.anno.rempy.script

fun tokenize(expr: String): List<String> =
    Regex("\"[^\"]*\"|==|!=|[()*/,+-]|\\w+")
        .findAll(expr).map { it.value }.toList()
