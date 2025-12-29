package me.anno.rempy

import me.anno.Time
import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.fonts.Font
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawRounded
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.rempy.animation.Image
import me.anno.rempy.script.ScriptParser
import me.anno.rempy.vm.RenPyRuntime
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.PureTestEngine.Companion.testPureUI
import me.anno.ui.debug.TestDrawPanel
import me.anno.utils.Color
import me.anno.utils.Color.withAlpha
import org.apache.logging.log4j.LogManager
import kotlin.math.abs

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
        val alpha = image.transition.alpha
        texture.bind(0, Filtering.LINEAR, Clamping.CLAMP)
        val f = 0.5f
        drawTexture(
            it.x + (-sizeX * 0.5f + (image.position.x) * it.width).toInt(),
            it.y + (-sizeY * (0.5f + f) + (image.position.y + f) * it.height).toInt(),
            sizeX.toInt(), sizeY.toInt(),
            texture, Color.white.withAlpha(alpha)
        )

        if (file == InvalidRef) {
            LOGGER.warn("Missing ${image.source}")
        }
    }

    var deltaTime = 0f
    val frameRate = 30f

    // todo support extra line height...
    val font = Font("Times New Roman", 20f)
    val menuFont = font.withRelativeLineSpacing(2f)
    var hoveredOption = -1

    testPureUI("RenPy", TestDrawPanel {
        val forward = if (player.menu == null) {
            (Input.wasKeyPressed(' ') ||
                    Input.wasKeyPressed(Key.BUTTON_LEFT) ||
                    Input.wasKeyPressed(Key.KEY_ARROW_RIGHT))
        } else {
            Input.wasKeyPressed(Key.BUTTON_LEFT) && hoveredOption != -1
        }

        deltaTime += Time.deltaTime.toFloat()
        if (deltaTime > 1f / frameRate || forward) {
            player.menuIndex = hoveredOption
            player.update(forward)
            deltaTime = 0f
        }

        showFPS = false

        val background = player.background
        if (background != null) {
            drawImage(it, background, 1f)
        }

        for ((_, image) in player.images) {
            drawImage(it, image, 0.7f)
        }

        hoveredOption = -1
        val menu = player.menu
        if (menu != null) {

            val text = menu.choices.joinToString("\n") { choice -> choice.text }

            // todo highlight the hovered line
            // todo small space between lines/background...
            fun drawText(alpha: Int): Int {
                return DrawTexts.drawText(
                    it.x + it.width / 2, it.y + it.height / 2, 2,
                    menuFont, text,
                    Color.white.withAlpha(alpha),
                    it.background.color.withAlpha(0),
                    -1, -1, // no automatic line breaks
                    AxisAlignment.CENTER,
                    AxisAlignment.CENTER
                )
            }

            val size = drawText(0)

            DrawRectangles.drawRect(it.x, it.y, it.width, it.height, Color.black.withAlpha(180))

            val lineHeight = menuFont.lineSpacingI
            val padding = 4
            val windowStack = it.windowStack
            val hoverX = abs(windowStack.mouseXi - (it.x + it.width / 2)) <= getSizeX(size)
            for (i in menu.choices.indices) {
                val y0 = it.y + (it.height - getSizeY(size)) / 2 + lineHeight * i - 4
                val h = lineHeight - 4
                val isHovered = hoverX && windowStack.mouseYi - y0 in 0 until h
                if (isHovered) hoveredOption = i
                val color = if (isHovered) Color.black else it.background.color.withAlpha(200)
                val radius = 6f
                DrawRounded.drawRoundedRect(
                    it.x + (it.width - getSizeX(size)) / 2 - padding, y0,
                    getSizeX(size) + 2 * padding, h,
                    radius, radius, radius, radius,
                    1f, color, color,
                    color.withAlpha(0), 1f
                )
            }

            drawText(255)
        }

        // todo blur the background, when someone is speaking?
        // todo or just some dark overlay, that doesn't effect the speaker?

        val text = player.text
        if (menu == null && text != null) {

            fun drawText(alpha: Int): Int {
                return DrawTexts.drawText(
                    it.x + it.width * 1 / 10, it.y + it.height, 2,
                    font, text.text,
                    Color.white.withAlpha(alpha),
                    it.background.color.withAlpha(0),
                    it.width * 8 / 10, it.height,
                    AxisAlignment.MIN,
                    AxisAlignment.MAX
                )
            }

            // todo draw who says it
            // todo speaking-text-animation

            val size = drawText(0)

            DrawRectangles.drawRect(
                it.x, it.y + it.height - getSizeY(size),
                it.width, getSizeY(size),
                it.background.color.withAlpha(200)
            )

            drawText(255)
        }
    })
}