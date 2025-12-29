package me.anno.rempy.animation

sealed class Transition(val durationFrames: Int) {

    var frame = 0

    val alpha get() = ((frame.toFloat() / durationFrames) * 255).toInt().coerceIn(0,255)
    fun update() { if (frame < durationFrames) frame++ }

    class Fade(durationFrames: Int) : Transition(durationFrames) {
        override fun clone() = Fade(durationFrames)
    }

    abstract fun clone(): Transition
}