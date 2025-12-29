package me.anno.rempy.vm

import me.anno.rempy.animation.Image
import me.anno.rempy.script.Command

data class SaveState(
    val index: Int,
    val variables: Map<String, Any>,
    val background: Image?,
    val shownImages: List<Image>,
    val currentText: String?,
    val currentMenu: MenuState?
)

data class MenuState(
    val choices: List<Command.MenuChoice>,
    val selected: Int
)
