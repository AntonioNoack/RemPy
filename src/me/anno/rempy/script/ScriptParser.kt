package me.anno.rempy.script

import me.anno.rempy.animation.Image
import me.anno.rempy.script.expr.ExprParser

class ScriptParser(val lines: List<String>) {

    val commands = ArrayList<Command>()
    val labels = HashMap<String, Int>()
    var i = 0

    fun readCommandsInIndentedBlock(subIndent: String, endLabel: String): List<String> {
        val list = ArrayList<String>(); i++
        while (i < lines.size && lines[i].startsWith(subIndent)) {
            readLine()
        }
        commands += Command.Jump(endLabel)
        return list
    }

    fun readLine() {
        var line = lines[i].trimEnd()
        var depth = 0
        while (depth < line.length && line[depth].isWhitespace()) {
            depth++
        }
        val indent = line.substring(0, depth)
        line = line.substring(depth)

        when {
            line.isEmpty() || line.startsWith("# ") || line.startsWith("```") -> {}
            line.startsWith("label ") -> {
                val name = line.removePrefix("label ").removeSuffix(":")
                if (labels.put(name, commands.size) != null) {
                    throw IllegalStateException("Duplicate label $name")
                }
                commands += Command.Label(name)
            }
            line.startsWith("$") -> {
                val (name, expr) = line.removePrefix("$").split("=")
                commands += Command.SetVar(name.trim(), parseExpr(expr))
            }
            line.startsWith("scene ") -> {
                val source = line.removePrefix("scene ")
                commands += Command.Scene(Image.parse(source))
            }
            line.startsWith("show ") -> {
                val source = line.removePrefix("show ")
                commands += Command.Show(Image.parse(source))
            }
            line.startsWith("jump ") -> {
                val label = line.removePrefix("jump ")
                commands += Command.Jump(label)
            }
            line.startsWith("music ") -> {
                val label = line.removePrefix("music ")
                commands += Command.Music(label)
            }
            line == "menu:" -> {
                val choices = ArrayList<Command.MenuChoice>()
                val subIndent = "$indent "
                val endLabel = newLabel()
                commands += Command.Menu(choices)
                i++
                while (i < lines.size && lines[i].startsWith(subIndent)) {
                    val option = lines[i++]
                    var optionDepth = 0
                    while (optionDepth < line.length && option[optionDepth].isWhitespace()) {
                        optionDepth++
                    }

                    val ifLabel = newLabel()
                    check(labels.put(ifLabel, commands.size) == null)

                    val idx = option.indexOf(": jump ")
                    if (idx >= 0) {
                        val optionText = option.substring(optionDepth, idx)
                        val jumpTarget = option.substring(idx + ": jump ".length).trim()
                        commands += Command.Jump(jumpTarget)
                        choices += Command.MenuChoice(optionText, ifLabel)
                    } else {
                        val optionText = option.substring(optionDepth)
                        val subIndent = option.substring(0, optionDepth) + " "
                        readCommandsInIndentedBlock(subIndent, endLabel)
                        choices += Command.MenuChoice(optionText, ifLabel)
                    }
                }

                check(labels.put(endLabel, commands.size) == null)
                return
            }
            line.startsWith("if ") -> {
                val branches = ArrayList<Command.IfBranch>()
                val subIndent = "$indent "

                val endLabel = newLabel()
                commands += Command.IfBlock(branches, endLabel)

                val ifLabel = newLabel()
                check(labels.put(ifLabel, commands.size) == null)

                readCommandsInIndentedBlock(subIndent, endLabel)

                val condition = parseExpr(line.removePrefix("if ").removeSuffix(":"))
                branches += Command.IfBranch(condition, ifLabel)

                while (lines[i].startsWith(indent + "elif ")) {
                    val condition = parseExpr(lines[i].substring(depth + 5).removeSuffix(":"))
                    val elifLabel = newLabel()
                    check(labels.put(elifLabel, commands.size) == null)

                    readCommandsInIndentedBlock(subIndent, endLabel)
                    branches += Command.IfBranch(condition, elifLabel)
                }

                if (lines[i].startsWith(indent + "else:")) {
                    val elseLabel = newLabel()
                    check(labels.put(elseLabel, commands.size) == null)

                    readCommandsInIndentedBlock(subIndent, endLabel)
                    branches += Command.IfBranch(null, elseLabel)
                } else {
                    branches += Command.IfBranch(null, endLabel)
                }

                check(labels.put(endLabel, commands.size) == null)
                return
            }
            line.startsWith("\"") -> {
                commands += Command.Say(null, line.trim('"'))
            }
            line[0].isLetter() && (line.contains("+=") || line.contains("-=")) -> {
                val idx = line.indexOf('=')
                val name = line.substring(0, idx - 1).trim()
                val delta = line.substring(idx + 1)
                val symbol = line[idx - 1]
                commands += Command.SetVar(name, parseExpr("$name $symbol $delta"))
            }
            line[0].isLetter() -> {
                val space = line.indexOf(' ')
                check(space >= 0) { "Expected space in '$line'($i)" }
                val speaker = line.substring(0, space)
                val message = line.substring(space + 1).trim()
                commands += Command.Say(speaker, message)
            }
            else -> println("Ignoring line: '$line'")
        }
        i++
    }

    fun parseExpr(s: String) =
        ExprParser(tokenize(s)).parse()

    var ctr = 0
    fun newLabel() = "${ctr++}"

    fun parse(): Script {
        while (i < lines.size) {
            readLine()
        }
        return Script(commands, labels)
    }
}
