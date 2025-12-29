package me.anno.rempy.script.expr

sealed class Expr {
    data class IntVal(val value: Int) : Expr()
    data class BoolVal(val value: Boolean) : Expr()
    data class StringVal(val value: String) : Expr()
    data class Var(val name: String) : Expr()
    data class Call(val name: String, val args: List<Expr>) : Expr()

    data class Add(val a: Expr, val b: Expr) : Expr()
    data class Sub(val a: Expr, val b: Expr) : Expr()
    data class Mul(val a: Expr, val b: Expr) : Expr()
    data class Div(val a: Expr, val b: Expr) : Expr()

    data class Equals(val a: Expr, val b: Expr) : Expr()
    data class NotEquals(val a: Expr, val b: Expr) : Expr()
    data class Less(val a: Expr, val b: Expr) : Expr()
    data class Greater(val a: Expr, val b: Expr) : Expr()

    data class And(val a: Expr, val b: Expr) : Expr()
    data class Or(val a: Expr, val b: Expr) : Expr()
    data class Not(val expr: Expr) : Expr()
    data class Negative(val expr: Expr) : Expr()
}
