package io.github.markusmo3.bm

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.impl.IdeFrameImpl
import io.github.markusmo3.bm.config.BMActionsSchema
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import kotlin.math.min


@Suppress("UnstableApiUsage")
class BIProjectManagerListener: ProjectManagerListener {

    override fun projectOpened(project: Project) {
        if (BMActionsSchema.getInstance().state.useBetterIcon) {
            setIdeaWindowIcon(findFrameForProject(project), getIdentifyingLetters(project))
        } else {
            setIdeaWindowIcon(findFrameForProject(project), null)
        }
    }

    companion object {
        private val identLettersSplitRegexes = listOf("[-]+", "[_]+", "[-_]+")
        private fun findFrameForProject(project: Project): Frame? {
            for (frame: Frame in IdeFrameImpl.getFrames()) {
                if (frame is IdeFrame && frame.project == project) {
                    return frame
                }
            }
            return null
        }

        private var initialFrameIcon: BufferedImage? = null
        private fun getInitialFrameIcon(frame: Frame): BufferedImage {
            if (initialFrameIcon == null) {
                initialFrameIcon = frame.iconImage.toBufferedImage()
            }
            return initialFrameIcon!!.deepCopy()
        }

        /**
         * Converts a given Image into a BufferedImage
         *
         * @return The converted BufferedImage
         */
        fun Image.toBufferedImage(): BufferedImage {
            if (this is BufferedImage) {
                return this
            }

            // Create a buffered image with transparency
            val bufferedImage = BufferedImage(getWidth(null), getHeight(null), BufferedImage.TYPE_INT_ARGB)

            // Draw the image on to the buffered image
            val bGr = bufferedImage.createGraphics()
            bGr.drawImage(this, 0, 0, null)
            bGr.dispose()

            // Return the buffered image
            return bufferedImage
        }

        fun BufferedImage.deepCopy(): BufferedImage {
            val cm: ColorModel = colorModel
            val isAlphaPremultiplied: Boolean = cm.isAlphaPremultiplied()
            val raster = copyData(raster.createCompatibleWritableRaster())
            return BufferedImage(cm, raster, isAlphaPremultiplied, null)
        }

        fun setIdeaWindowIcon(frame: Frame?, overlayText: String?) {
            if (frame == null) {
                return
            }
            val img = getInitialFrameIcon(frame)
            if (overlayText == null) {
                frame.iconImage = img
                println("### set initial frame icon")
                return
            }
            val moreThanTwoChars = overlayText.length > 2
            val g = img.createGraphics()
            g.color = Color.BLACK
            g.fillRect(5, 5, 20, if (moreThanTwoChars) 20 else 16)
            g.color = Color.WHITE
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            if (moreThanTwoChars) {
                g.font = Font("Consolas", Font.BOLD, 16)
                g.drawString(overlayText.substring(0, 2), 6, 13)
                g.drawString(overlayText.substring(2), 6, 26)
            } else {
                g.font = Font("Consolas", Font.BOLD, 16)
                g.drawString(overlayText, 6, 16)
            }
            frame.iconImage = img
        }

        fun getIdentifyingLetters(project: Project?, i: Int = 0): String {
            if (project == null) {
                return "???"
            }
            val prjName = project.name
            if (i >= identLettersSplitRegexes.size) {
                return prjName.substring(0, min(4, prjName.length))
            }
            val regex = identLettersSplitRegexes[i]
            val split = prjName.split(Regex(regex))
            if (split.size >= 4) {
                return split[0].substring(0, 1) + split[1].substring(0, 1) + split[2].substring(0, 1) + split[3].substring(0, 1)
            } else if (split.size == 3) {
                return split[0].substring(0, 1) + split[1].substring(0, 1) + split[2].substring(0, 1)
            } else if (split.size == 2) {
                return split[0].substring(0, 1) + split[1].substring(0, 1)
            } else {
                return getIdentifyingLetters(project, i + 1)
            }
        }
    }
}
