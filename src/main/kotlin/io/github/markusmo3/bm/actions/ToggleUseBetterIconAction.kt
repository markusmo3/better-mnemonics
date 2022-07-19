package io.github.markusmo3.bm.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.impl.IdeFrameImpl
import io.github.markusmo3.bm.BIProjectManagerListener
import io.github.markusmo3.bm.config.BMActionsSchema
import java.awt.Frame

class ToggleUseBetterIconAction: ToggleAction("Use BetterIcon for window", null, AllIcons.Actions.MoveToWindow) {
    override fun isSelected(e: AnActionEvent): Boolean {
        return BMActionsSchema.getInstance().state.useBetterIcon
    }

    @Suppress("UnstableApiUsage")
    override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (BMActionsSchema.getInstance().state.useBetterIcon != state) {
            BMActionsSchema.getInstance().state.useBetterIcon = state
            // update all icons
            for (frame: Frame in IdeFrameImpl.getFrames()) {
                if (frame is IdeFrame && frame.project != null) {
                    if (state) {
                        BIProjectManagerListener.setIdeaWindowIcon(frame,
                            BIProjectManagerListener.getIdentifyingLetters(frame.project))
                    } else {
                        BIProjectManagerListener.setIdeaWindowIcon(frame, null)
                    }
                }
            }
        }

    }
}
