package me.anno.rempy.script

import me.anno.rempy.animation.Image
import me.anno.rempy.script.expr.Expr

sealed class Command {
    data class Label(val name: String) : Command()
    data class Say(val speaker: String?, val text: String) : Command()
    data class Scene(val image: Image) : Command()
    data class Show(val image: Image) : Command()
    data class Jump(val label: String) : Command()

    data class Menu(val choices: List<MenuChoice>) : Command()
    data class MenuChoice(val text: String, val target: String)

    data class SetVar(val name: String, val expr: Expr) : Command()
    data class IfBlock(val branches: List<IfBranch>, val endLabel: String) : Command()
    data class IfBranch(val condition: Expr?, val targetLabel: String)

    data class Music(val label: String) : Command()
}