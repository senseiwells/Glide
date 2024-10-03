package me.senseiwells.glide.datagen

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer
import net.casual.arcade.datagen.language.LanguageEntryGenerator
import net.casual.arcade.resources.lang.LanguageEntry
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream

class TextboxGenerator(
    private val component: Component,
    private val location: (String) -> Path,
): LanguageEntryGenerator {
    override fun run(font: Font, lang: String, consumer: Consumer<LanguageEntry>) {
        val width = font.width(this.component) / 2

        val image = BufferedImage(width, 8, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color(0xFFFFFF)
        graphics.fillRect(0, 1, width, 4)
        graphics.color = Color(0x444444)
        graphics.drawRect(0, 0, width, 0)
        graphics.drawRect(0, 5, width, 0)
        graphics.dispose()
        this.location.invoke(lang).createParentDirectories().outputStream().use {
            ImageIO.write(image, "png", it)
        }
    }
}