package me.anno.rempy

import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.input.Input
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.rempy.animation.Image
import me.anno.rempy.script.ScriptParser
import me.anno.rempy.vm.RenPyRuntime
import me.anno.ui.Panel
import me.anno.ui.debug.PureTestEngine.Companion.testPureUI
import me.anno.ui.debug.TestDrawPanel
import me.anno.utils.Color
import me.anno.utils.Color.withAlpha
import org.apache.logging.log4j.LogManager

// todo implement something like RenPy, just in Kotlin with Rem's Engine
private val LOGGER = LogManager.getLogger("RemPy")

fun main() {
    val documents = getReference("/media/antonio/58CE075ECE0733B2/Users/Antonio/Documents")
    val project = documents.getChild("IdeaProjects/AKISAv7/assets/visualnovel")
    val sources = project.getChild("chapters/1")
    val images = project.getChild("images/rempy")

    // todo prefix any labels with their file name
    val source = sources.listChildren()
        .sortedBy { it.nameWithoutExtension.toInt() }
        .joinToString("\n") { it.readTextSync() }

    val imageMap = images.listChildren()
        .associateBy { it.nameWithoutExtension }

    LOGGER.info("Source length: ${source.length}")

    val player = RenPyRuntime(ScriptParser(source.lines()).parse())
    LOGGER.info("Command Length: ${player.script.commands.size}")

    fun drawImage(it: Panel, image: Image, size: Float) {
        val source = image.source
            .replace(",", "")
            .replace(' ', '_')
        val file = imageMap[source] ?: InvalidRef

        val texture = TextureCache[file].waitFor() ?: TextureLib.whiteTexture
        val sizeY = it.height * size
        val sizeX = texture.width * sizeY / texture.height
        val alpha = image.transition?.alpha ?: 255
        texture.bind(0, Filtering.LINEAR, Clamping.CLAMP)
        drawTexture(
            it.x + (-sizeX * 0.5f + (image.position.x) * it.width).toInt(),
            it.y + (-sizeY * 0.5f + (image.position.y) * it.height).toInt(),
            sizeX.toInt(), sizeY.toInt(),
            texture, Color.white.withAlpha(alpha)
        )

        if (file == InvalidRef) {
            LOGGER.warn("Missing ${image.source}")
        }
    }

    testPureUI("RenPy", TestDrawPanel {
        player.update(Input.wasKeyPressed(' '))
        showFPS = false

        // todo measure text width
        val background = player.background
        if (background != null) {
            drawImage(it, background, 1f)
        }

        for ((_, image) in player.shownImages) {
            drawImage(it, image, 0.5f)
        }

        // todo draw text box
        // todo draw who says the text

        val text = player.currentText
        if (text != null) {

        }
    })
}