package me.anno.rempy.text

import me.anno.rempy.script.expr.ExprEvaluator
import me.anno.rempy.script.expr.ExprParser
import me.anno.rempy.script.tokenize

class TextInterpolator(private val eval: ExprEvaluator) {
    private val regex = Regex("\\[(.+?)]")
    fun interpolate(text: String): String =
        regex.replace(text) {
            eval.eval(ExprParser(tokenize(it.groupValues[1])).parse()).toString()
        }
}
