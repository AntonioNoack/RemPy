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
import me.anno.io.files.FileReference
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
import me.anno.utils.types.Strings.camelCaseToTitle
import org.apache.logging.log4j.LogManager
import kotlin.math.abs

private val LOGGER = LogManager.getLogger("RemPy")

fun main() {
    val documents = getReference("/media/antonio/58CE075ECE0733B2/Users/Antonio/Documents")
    val project = documents.getChild("IdeaProjects/AKISAv7/assets/visualnovel")
    val sources = project.getChild("chapters/1")
    val images = project.getChild("images/rempy")

    val source = sources.listChildren()
        .sortedBy { it.nameWithoutExtension.toInt() }
        .joinToString("\n") { it.readTextSync() }

    imageMap = images.listChildren()
        .associateBy { it.nameWithoutExtension }

    LOGGER.info("Source length: ${source.length}")

    val runtime = RenPyRuntime(ScriptParser(source).parse())
    LOGGER.info("Command Length: ${runtime.script.commands.size}")

    // todo load/save system
    // todo take screenshots for them

    // todo at the start, ask for the player's name
    runtime.vars.set("name", "Antonio")

    testPureUI("RenPy", TestDrawPanel {
        showFPS = false
        drawRemPyPlayer(runtime, it)
    })
}

var deltaTime = 1f
val frameRate = 30f

val font = Font("Times New Roman", 20f)
val menuFont = font.withRelativeLineSpacing(2f)

val history = History()
var hoveredOption = -1
var lastMessage = ""
var textProgress = 0f

lateinit var imageMap: Map<String, FileReference>

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

fun drawRemPyPlayer(runtime: RenPyRuntime, it: Panel) {
    val forward = !history.isEmpty() && if (runtime.menu == null) {
        (Input.wasKeyPressed(' ') ||
                Input.wasKeyPressed(Key.BUTTON_LEFT) ||
                Input.wasKeyPressed(Key.KEY_ARROW_RIGHT))
    } else {
        Input.wasKeyPressed(Key.BUTTON_LEFT) && hoveredOption != -1
    }

    val backward = !forward && (
            Input.wasKeyPressed(Key.BUTTON_RIGHT) ||
                    Input.wasKeyPressed(Key.KEY_ARROW_LEFT) ||
                    Input.wasKeyPressed(Key.KEY_BACKSPACE))

    if (backward) {

        val newState = history.undo()
        runtime.loadState(newState)

    } else {
        deltaTime += Time.deltaTime.toFloat()
        if (deltaTime > 1f / frameRate || forward) {
            runtime.menuIndex = hoveredOption
            runtime.update(forward)

            if (history.isEmpty() || forward) {
                history.push(runtime.saveState())
            }

            deltaTime = 0f
        }
    }

    val background = runtime.background
    if (background != null) {
        drawImage(it, background, 1f)
    }

    for ((_, image) in runtime.images) {
        drawImage(it, image, 0.7f)
    }

    hoveredOption = -1
    val menu = runtime.menu
    if (menu != null) {

        val text = menu.choices.joinToString("\n") { choice -> choice.text }

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

    val text = runtime.text
    if (menu == null && text != null) {

        fun drawText(alpha: Int, text: String): Int {
            return DrawTexts.drawText(
                it.x + it.width * 1 / 10, it.y + it.height, 2,
                font, text,
                Color.white.withAlpha(alpha),
                it.background.color.withAlpha(0),
                it.width * 8 / 10, it.height,
                AxisAlignment.MIN,
                AxisAlignment.MAX
            )
        }

        // todo speaking-text-animation

        val size = drawText(0, text.text)
        val sp = font.lineSpacingI

        DrawRectangles.drawRect(
            it.x, it.y + it.height - (getSizeY(size) + sp),
            it.width, (getSizeY(size) + sp),
            it.background.color.withAlpha(200)
        )

        // draw who says it
        if (text.speaker != null) {
            // todo hard-coded color for speakers
            val color = text.speaker.hashCode() or 0x606060
            DrawTexts.drawText(
                it.x + it.width * 1 / 20, it.y + it.height - getSizeY(size), 2,
                font, text.speaker.camelCaseToTitle(), color.withAlpha(255),
                it.background.color.withAlpha(0),
                -1, -1,
                AxisAlignment.MIN, AxisAlignment.MAX
            )
        }

        // todo first layout, then substring, so overflow on the right side is immediately accounted for
        var text = text.text
        if (text != lastMessage) {
            lastMessage = text
            textProgress = 0f
            text = ""
        } else {
            textProgress += Time.deltaTime.toFloat()
            val length = (textProgress * 120f).toInt()
            if (length < text.length) text = text.substring(0, length)
        }

        DrawTexts.drawText(
            it.x + it.width * 1 / 10, it.y + it.height - getSizeY(size), 2,
            font, text, Color.white,
            it.background.color.withAlpha(0),
            it.width * 8 / 10, it.height,
            AxisAlignment.MIN, AxisAlignment.MIN
        )
    }
}