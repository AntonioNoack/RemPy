package me.anno.rempy.vm

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
    var images = LinkedHashMap<String, Image>()

    var text: Command.Say? = null
    var menu: Command.Menu? = null
    var menuIndex = 0

    fun update(advance: Boolean) {
        background?.transition?.update()
        for ((_, image) in images) {
            image.transition.update()
        }

        val menu = menu
        if (menu != null) {
            val choice = menu.choices.getOrNull(menuIndex)
            if (advance && choice != null) {
                // println("Options & targets: ${menu.choices.map { "${it.target}->${script.labels[it.target]}" }}")
                index = script.labels[choice.target]!!
                // println("Chose option $menuIndex -> ${choice.target} -> jumped to $index")
                this.menu = null
                this.text = null
                runCommands()
            } else return
        }

        if (text != null && !advance) return

        text = null
        runCommands()
    }

    fun runCommands() {
        while ((text == null && menu == null) && index < script.commands.size) {
            val command = script.commands[index++]
            runNextCommand(command)
        }
    }

    fun runNextCommand(command: Command) {
        // println("Executing command[${index-1}]: $command")
        when (command) {
            is Command.Scene -> {
                background = command.image.clone()
                images.clear()
            }
            is Command.Say -> {
                text = Command.Say(
                    if (command.speaker != null) interpolator.interpolate(command.speaker) else null,
                    interpolator.interpolate(command.text)
                )
            }
            is Command.Show -> {
                val image = command.image
                val oldImage = images.remove(image.prefix)
                val newImage = image.clone()
                if (oldImage != null) newImage.transition.frameIndex = oldImage.transition.frameIndex
                images[image.prefix] = newImage
            }
            is Command.Menu -> {
                menu = command
                menuIndex = -1
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
            shownImages = images.values.toList(),
            currentText = text?.text,
            currentMenu = menu?.let { MenuState(it.choices, menuIndex) }
        )

    fun loadState(state: SaveState) {
        index = state.index
        vars.restore(state.variables)
        background = state.background
        check(images !== state.shownImages)
        images.clear()
        for (image in state.shownImages) {
            images[image.prefix] = image
        }
        text = state.currentText?.let { Command.Say(null, it) }
        menu = state.currentMenu?.let { Command.Menu(it.choices) }
        menuIndex = state.currentMenu?.selected ?: 0
    }
}
