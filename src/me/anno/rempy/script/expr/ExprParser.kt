package me.anno.rempy.script.expr

class ExprParser(private val tokens: List<String>) {
    private var pos = 0
    private fun peek() = tokens.getOrNull(pos)
    private fun take() = tokens[pos++]

    fun parse(): Expr = parseOr()

    private fun parseOr(): Expr {
        var e = parseAnd()
        while (peek() == "or") {
            take(); e = Expr.Or(e, parseAnd())
        }
        return e
    }

    private fun parseAnd(): Expr {
        var e = parseCompare()
        while (peek() == "and") {
            take(); e = Expr.And(e, parseCompare())
        }
        return e
    }

    private fun parseCompare(): Expr {
        var e = parseAdd()
        when (peek()) {
            "==" -> {
                take(); e = Expr.Equals(e, parseAdd())
            }
            "!=" -> {
                take(); e = Expr.NotEquals(e, parseAdd())
            }
            "<" -> {
                take(); e = Expr.Less(e, parseAdd())
            }
            ">" -> {
                take(); e = Expr.Greater(e, parseAdd())
            }
        }
        return e
    }

    private fun parseAdd(): Expr {
        var e = parseMul()
        while (peek() in listOf("+", "-")) {
            e = if (take() == "+") Expr.Add(e, parseMul())
            else Expr.Sub(e, parseMul())
        }
        return e
    }

    private fun parseMul(): Expr {
        var e = parseUnary()
        while (peek() in listOf("*", "/")) {
            e = if (take() == "*") Expr.Mul(e, parseUnary())
            else Expr.Div(e, parseUnary())
        }
        return e
    }

    private fun parseUnary(): Expr {
        return if (peek() == "not") {
            take(); Expr.Not(parseNegative())
        } else parseNegative()
    }

    private fun parseNegative(): Expr {
        return if (peek() == "-") {
            take(); Expr.Negative(parseNegative())
        } else parseAtom()
    }

    private fun parseAtom(): Expr {
        val t = take()
        return when {
            t == "(" -> {
                val e = parse(); take(); e
            }
            t.startsWith("\"") -> Expr.StringVal(t.trim('"'))
            t == "true" -> Expr.BoolVal(true)
            t == "false" -> Expr.BoolVal(false)
            t.matches(Regex("\\d+")) -> Expr.IntVal(t.toInt())
            peek() == "(" -> {
                take() // consume '('
                val params = ArrayList<Expr>()
                while (true) {
                    if (peek() == ",") take()
                    if (peek() == ")") break
                    params.add(parse())
                }
                take() // consume ')'
                Expr.Call(t, params)
            }
            else -> Expr.Var(t)
        }
    }
}
