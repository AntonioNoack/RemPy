package me.anno.rempy.script.expr

import me.anno.maths.Maths.clamp
import me.anno.rempy.vm.VariableStore

class ExprEvaluator(private val vars: VariableStore) {
    fun eval(e: Expr): Any = when (e) {
        is Expr.IntVal -> e.value
        is Expr.BoolVal -> e.value
        is Expr.StringVal -> e.value
        is Expr.Var -> vars.get(e.name)

        is Expr.Add -> {
            val a = eval(e.a)
            val b = eval(e.b)
            if (a is String || b is String) a.toString() + b.toString()
            else (a.anyToInt()) + (b.anyToInt())
        }

        is Expr.Sub -> eval(e.a).anyToInt() - eval(e.b).anyToInt()
        is Expr.Mul -> eval(e.a).anyToInt() * eval(e.b).anyToInt()
        is Expr.Div -> eval(e.a).anyToInt() / eval(e.b).anyToInt()
        is Expr.Negative -> -eval(e.expr).anyToInt()

        is Expr.Equals -> eval(e.a) == eval(e.b)
        is Expr.NotEquals -> eval(e.a) != eval(e.b)
        is Expr.Less -> eval(e.a).anyToInt() < eval(e.b).anyToInt()
        is Expr.Greater -> eval(e.a).anyToInt() > eval(e.b).anyToInt()

        is Expr.And -> eval(e.a).anyToBool() && eval(e.b).anyToBool()
        is Expr.Or -> eval(e.a).anyToBool() || eval(e.b).anyToBool()
        is Expr.Not -> !eval(e.expr).anyToBool()

        is Expr.Call -> when (e.name) {
            "addFriendly" -> addByLimit(e, -4)
            "addRomantically" -> addByLimit(e, 0)
            "addSexually" -> addByLimit(e, 2)
            "subtract" -> {
                val (a, b) = e.args
                clamp(eval(a).anyToInt() - eval(b).anyToInt(), -5, +5)
            }
            "addSkill" -> addByLimit(e, -100)
            else -> throw IllegalStateException("Unknown function ${e.name}")
        }
    }

    private fun addByLimit(e: Expr.Call, limit: Int): Int {
        check(e.args.size == 2)
        val base = eval(e.args[0]).anyToInt()
        val delta = eval(e.args[1]).anyToInt()
        return clamp(if (delta < 0) base + delta else if (base >= limit) base + delta else -5, -5, +5)
    }

    private fun Any.anyToInt(): Int = this as? Int ?: if (this == "") 0 else this.toString().toInt()
    private fun Any.anyToBool(): Boolean = this == true || this == "true" || (this is Int && this != 0)
}
