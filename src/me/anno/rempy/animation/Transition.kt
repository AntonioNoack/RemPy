package me.anno.rempy.animation

sealed class Transition(val durationFrames: Int) {

    var frameIndex = 0

    val alpha get() = ((frameIndex.toFloat() / durationFrames) * 255).toInt().coerceIn(0,255)
    fun update() { if (frameIndex < durationFrames) frameIndex++ }

    class Fade(durationFrames: Int) : Transition(durationFrames) {
        override fun clone() = Fade(durationFrames)
    }

    abstract fun clone(): Transition
}