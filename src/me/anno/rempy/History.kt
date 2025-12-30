package me.anno.rempy

import me.anno.rempy.vm.SaveState
import kotlin.math.max
import kotlin.math.min

class History {

    val sizeLimit = 500
    val states = ArrayList<SaveState>()
    var index = -1

    fun isEmpty() = index <= 0

    fun push(state: SaveState) {
        while (index >= states.size) states.removeLast()
        if (states.size >= sizeLimit) {
            states.removeAt(0)
            index = max(index - 1, 0)
        }
        states.add(state)
        index = min(index + 1, states.lastIndex)
    }

    fun undo(): SaveState {
        index = max(index - 1, 0)
        return states[index]
    }
}