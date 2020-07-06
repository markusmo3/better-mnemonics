package io.github.markusmo3.bm.popup

import com.intellij.ui.popup.list.ListPopupImpl
import com.intellij.ui.popup.list.PopupListElementRenderer

class BMPopupListElementRenderer<E>(myPopup: ListPopupImpl)
    : PopupListElementRenderer<E>(myPopup) {

//    private lateinit var myMnemonicLabel: JLabel
//    private lateinit var myShortcutLabel: JLabel
//
//    override fun createItemComponent(): JComponent? {
//        val panel = JPanel(BorderLayout())
//        createLabel()
//        panel.add(myTextLabel, BorderLayout.CENTER)
//        myMnemonicLabel = JLabel()
//        myMnemonicLabel.border = JBUI.Borders.emptyLeft(3)
//        myMnemonicLabel.foreground = UIManager.getColor("MenuItem.acceleratorForeground")
//        myShortcutLabel = JLabel()
//        myShortcutLabel.border = JBUI.Borders.emptyRight(3)
//        myShortcutLabel.foreground = UIManager.getColor("MenuItem.acceleratorForeground")
//        panel.add(myMnemonicLabel, BorderLayout.WEST)
//        panel.add(myShortcutLabel, BorderLayout.EAST)
//        return layoutComponent(panel)
//    }
//
//    override fun customizeComponent(list: JList<out E>, value: E, isSelected: Boolean) {
//        val step: ListPopupStep<Any> = myPopup.listStep
//        val isSelectable = step.isSelectable(value)
//        myTextLabel.isEnabled = isSelectable
//
//        if (step is BaseListPopupStep<*>) {
//            val bg = (step as BaseListPopupStep<E>).getBackgroundFor(value)
//            val fg = (step as BaseListPopupStep<E>).getForegroundFor(value)
//            if (!isSelected && fg != null) myTextLabel.foreground = fg
//            if (!isSelected && bg != null) UIUtil.setBackgroundRecursively(myComponent, bg)
//            if (bg != null && mySeparatorComponent.isVisible && myCurrentIndex > 0) {
//                val prevValue = list.model.getElementAt(myCurrentIndex - 1)
//                // separator between 2 colored items shall get color too
//                if (Comparing.equal(bg, (step as BaseListPopupStep<E>).getBackgroundFor(prevValue))) {
//                    myRendererComponent.background = bg
//                }
//            }
//        }
////        if (step.isMnemonicsNavigationEnabled) {
////            val filter = step.mnemonicNavigationFilter
////            val pos = filter?.getMnemonicPos(value) ?: -1
////            if (pos != -1) {
////                var text = myTextLabel.text
////                text = text.substring(0, pos) + text.substring(pos + 1)
////                myTextLabel.text = text
////                myTextLabel.displayedMnemonicIndex = pos
////            }
////        } else {
//            myTextLabel.displayedMnemonicIndex = -1
////        }
//        if (step.hasSubstep(value) && isSelectable) {
//            myNextStepLabel.isVisible = true
//            val isDark = ColorUtil.isDark(UIUtil.getListSelectionBackground())
//            myNextStepLabel.icon = if (isSelected) if (isDark) AllIcons.Icons.Ide.NextStepInverted else AllIcons.Icons.Ide.NextStep else AllIcons.Icons.Ide.NextStep
//        } else {
//            myNextStepLabel.isVisible = false
//            //myNextStepLabel.setIcon(PopupIcons.EMPTY_ICON);
//        }
//        setSelected(myComponent, isSelected && isSelectable)
//        setSelected(myTextLabel, isSelected && isSelectable)
//        setSelected(myNextStepLabel, isSelected && isSelectable)
//        if (myShortcutLabel != null) {
//            myShortcutLabel.isEnabled = isSelectable
//            myShortcutLabel.text = ""
//            if (value is ShortcutProvider) {
//                val set = (value as ShortcutProvider).shortcut
//                if (set != null) {
//                    val shortcut = ArrayUtil.getFirstElement(set.shortcuts)
//                    if (shortcut != null) {
//                        myShortcutLabel.text = "     " + KeymapUtil.getShortcutText(shortcut)
//                    }
//                }
//            }
//            setSelected(myShortcutLabel, isSelected && isSelectable)
//            myShortcutLabel.foreground = if (isSelected && isSelectable) UIManager.getColor("MenuItem.acceleratorSelectionForeground") else UIManager.getColor("MenuItem.acceleratorForeground")
//        }
//        if (myMnemonicLabel != null) {
//            myMnemonicLabel.isEnabled = isSelectable
//            myMnemonicLabel.text = ""
//            if (value is BMActionItem) {
//                if (value.keyStroke != null) {
//                    myMnemonicLabel.text = BMExtensions.toShortString(value.keyStroke)
//                }
//            }
//            setSelected(myMnemonicLabel, isSelected && isSelectable)
//            myMnemonicLabel.foreground = if (isSelected && isSelectable) UIManager.getColor("MenuItem.acceleratorSelectionForeground") else UIManager.getColor("MenuItem.acceleratorForeground")
//        }
//    }
}