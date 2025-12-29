package me.anno.rempy.animation

import org.joml.Vector2f

class Image(
    val prefix: String,
    var source: String
) {

    val position = Vector2f(0.5f)
    var transition: Transition = Transition.Fade(5)

    override fun toString(): String = source

    fun clone(): Image = parse(source)

    companion object {
        fun parse(source: String): Image {
            val parts = source.split(" with ")
            val name = parts[0].trim('"')
            val image = Image(name.split(' ')[0], name)
            if (parts.size > 1 && parts[1] == "fade") {
                image.transition = Transition.Fade(30)
            }
            return image
        }
    }
}