package me.anno.rempy.vm

import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.clamp
import me.anno.rempy.animation.Image
import me.anno.rempy.script.Command
import me.anno.rempy.script.Script
import me.anno.rempy.script.expr.ExprEvaluator
import me.anno.rempy.text.TextInterpolator

class RenPyRuntime(val script: Script) {

    var index = 0

    val vars = VariableStore()
    private val eval = ExprEvaluator(vars)
    private val interpolator = TextInterpolator(eval)

    var background: Image? = null
    var shownImages = LinkedHashMap<String, Image>()

    var currentText: Command.Say? = null
    var currentMenu: Command.Menu? = null
    var menuIndex = 0

    fun update(advance: Boolean) {
        background?.transition?.update()
        for ((_, image) in shownImages) {
            image.transition?.update()
        }

        if (currentMenu != null) {
            if (Input.wasKeyPressed(Key.KEY_ARROW_UP)) menuIndex--
            if (Input.wasKeyPressed(Key.KEY_ARROW_DOWN)) menuIndex++
            menuIndex = clamp(menuIndex, 0, currentMenu!!.choices.lastIndex)
            if (advance) {
                index = script.labels[currentMenu!!.choices[menuIndex].target]!!
                currentMenu = null
            }
            return
        }

        if (currentText != null && !advance) return

        currentText = null
        while (currentText == null && index < script.commands.size) {
            val command = script.commands[index++]
            runNextCommand(command)
        }
    }

    fun runNextCommand(command: Command) {
        when (command) {
            is Command.Scene -> {
                background = command.image.clone()
            }
            is Command.Say ->
                currentText = command.copy(text = interpolator.interpolate(command.text))
            is Command.Show -> {
                val image = command.image
                shownImages[image.prefix] = image.clone()
            }
            is Command.Menu -> {
                currentMenu = command
                menuIndex = 0
            }
            is Command.SetVar -> vars.set(command.name, eval.eval(command.expr))
            is Command.IfBlock -> {
                for (b in command.branches) {
                    if (b.condition == null || eval.eval(b.condition) == true) {
                        index = script.labels[b.targetLabel]!!
                        break
                    }
                }
            }
            is Command.Jump -> index = script.labels[command.label]!!
            is Command.Label -> { /* just jump over it */
            }
            is Command.Music -> {
                // todo stop previous music
                // todo start playing music
            }
            else -> throw NotImplementedError(command.toString())
        }
    }

    fun saveState(): SaveState =
        SaveState(
            index = index,
            variables = vars.snapshot(),
            background = background,
            shownImages = shownImages.values.toList(),
            currentText = currentText?.text,
            currentMenu = currentMenu?.let { MenuState(it.choices, menuIndex) }
        )

    fun loadState(state: SaveState) {
        index = state.index
        vars.restore(state.variables)
        background = state.background
        check(shownImages !== state.shownImages)
        shownImages.clear()
        for (image in state.shownImages) {
            shownImages[image.prefix] = image
        }
        currentText = state.currentText?.let { Command.Say(null, it) }
        currentMenu = state.currentMenu?.let { Command.Menu(it.choices) }
        menuIndex = state.currentMenu?.selected ?: 0
    }
}
