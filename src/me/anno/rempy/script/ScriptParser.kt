package me.anno.rempy.script

import me.anno.rempy.animation.Image
import me.anno.rempy.script.expr.ExprParser
import org.apache.logging.log4j.LogManager

class ScriptParser(text: String) {

    companion object {
        private val LOGGER = LogManager.getLogger(ScriptParser::class)
    }

    private val lines = text
        .lines().filter { it.isNotBlank() }

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

    fun getDepth(line: String): Int {
        var lineDepth = 0
        while (lineDepth < line.length && line[lineDepth].isWhitespace()) {
            lineDepth++
        }
        return lineDepth
    }

    fun readLine() {
        var line = lines[i].trimEnd()
        val lineDepth = getDepth(line)
        val indent = line.substring(0, lineDepth)
        line = line.substring(lineDepth)

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
                val subIndent = "$indent  "
                val endLabel = newLabel()
                commands += Command.Menu(choices)
                i++
                while (i < lines.size && lines[i].startsWith(subIndent)) {
                    val option = lines[i]
                    check(':' in option)
                    val optionDepth = getDepth(option)

                    val ifLabel = newLabel()
                    check(labels.put(ifLabel, commands.size) == null)

                    val idx = option.indexOf(": jump ")
                    if (idx >= 0) {
                        val optionText = option.substring(optionDepth + 1, idx - 1)
                        val jumpTarget = option.substring(idx + ": jump ".length).trim()
                        commands += Command.Jump(jumpTarget)
                        choices += Command.MenuChoice(optionText, ifLabel)
                        i++ // skip this line
                    } else {
                        val optionText = option.substring(optionDepth + 1, option.length - 2) // +1,-1 to remove quotes
                        val subIndent = option.substring(0, optionDepth) + " "
                        readCommandsInIndentedBlock(subIndent, endLabel) // line itself is skipped inside here
                        choices += Command.MenuChoice(optionText, ifLabel)
                    }
                }

                if (choices.size <= 1) LOGGER.warn("Only ${choices.size} choices? ${choices.map { it.text }}")
                check(labels.put(endLabel, commands.size) == null)
                return
            }
            line.startsWith("if ") -> {
                val branches = ArrayList<Command.IfBranch>()
                val subIndent = "$indent  "

                val endLabel = newLabel()
                commands += Command.IfBlock(branches, endLabel)

                val ifLabel = newLabel()
                check(labels.put(ifLabel, commands.size) == null)

                readCommandsInIndentedBlock(subIndent, endLabel)

                val condition = parseExpr(line.removePrefix("if ").removeSuffix(":"))
                branches += Command.IfBranch(condition, ifLabel)

                while (lines[i].startsWith(indent + "elif ")) {
                    val condition = parseExpr(lines[i].substring(lineDepth + 5).removeSuffix(":"))
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
