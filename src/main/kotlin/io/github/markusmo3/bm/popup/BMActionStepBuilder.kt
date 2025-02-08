package io.github.markusmo3.bm.popup

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.actionSystem.impl.Utils
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Key
import com.intellij.ui.SizedIcon
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.LafIconLookup
import com.intellij.util.ui.LafIconLookup.getDisabledIcon
import com.intellij.util.ui.LafIconLookup.getSelectedIcon
import io.github.markusmo3.bm.BMUtils.toShortString
import io.github.markusmo3.bm.config.BMNode
import javax.swing.Icon
import javax.swing.JComponent

class BMActionStepBuilder(
  private val myDataContext: DataContext, private val myShowDisabled: Boolean
) : PresentationFactory() {

  private val keyToolTipText: Key<String?> = Key.create("ToolTipText")

  private val myListModel: MutableList<BMActionItem> = ArrayList()
  private var myPrependWithSeparator: Boolean = false
  private var mySeparatorText: String? = null
  private var myEmptyIcon: Icon? = null
  private var myMaxIconWidth = 16
  private var myMaxIconHeight = 16
  private var myActionPlace: String = ActionPlaces.UNKNOWN

  fun setActionPlace(actionPlace: String) {
    myActionPlace = actionPlace
  }

  val items: List<BMActionItem>
    get() = myListModel

  @Suppress("UnstableApiUsage", "UnstableApiUsage")
  fun buildGroup(bmNode: BMNode) {
    myEmptyIcon = if (myMaxIconHeight != -1 && myMaxIconWidth != -1) EmptyIcon.create(
      myMaxIconWidth, myMaxIconHeight
    ) else null
    appendActionsFromGroup(bmNode)
    if (myListModel.isEmpty()) {
      myListModel.add(
        BMActionItem(
          BMNode.newUndefined(),
          Utils.EMPTY_MENU_FILLER,
          CommonBundle.messagePointer("empty.menu.filler").get(),
          null,
          false,
          null,
          null,
          false,
          null,
          null
        )
      )
    }
  }

  private fun createActionEvent(actionGroup: AnAction): AnActionEvent {
    val actionEvent = AnActionEvent(
      myDataContext,
      getPresentation(actionGroup),
      myActionPlace,
      ActionUiKind.POPUP,
      null,
      0,
      ActionManager.getInstance()
    )
    actionEvent.setInjectedContext(actionGroup.isInInjectedContext)
    return actionEvent
  }

  @Suppress("UnstableApiUsage")
  private fun appendActionsFromGroup(bmNode: BMNode) {
    val actionGroup: ActionGroup =
      DefaultActionGroup(bmNode.children.mapNotNull { it.action })
    Utils.expandActionGroup(actionGroup, this, myDataContext, myActionPlace, ActionUiKind.POPUP)
    for (bmChild in bmNode.children) {
      if (bmChild.isSeparator()) {
        myPrependWithSeparator = true
        mySeparatorText = bmChild.customText
      } else {

        appendAction(bmChild)
      }
    }
  }

  @Suppress("UnstableApiUsage")
  private fun appendAction(bmNode: BMNode) {
    val action: AnAction = bmNode.action ?: return
    val presentation = getPresentation(action)
    val enabled = presentation.isEnabled
    if ((myShowDisabled || enabled) && presentation.isVisible) {
      var text = bmNode.customText ?: presentation.text
      val keyStroke = bmNode.keyStroke
      if (keyStroke != null) {
        text = "${keyStroke.toShortString()}. $text"
      }
      var icon = presentation.icon
      var selectedIcon = presentation.selectedIcon
      var disabledIcon = presentation.disabledIcon
      if (icon == null && selectedIcon == null) {
        if (action is Toggleable && java.lang.Boolean.TRUE == presentation.getClientProperty(
            Toggleable.SELECTED_KEY
          )
        ) {
          icon = LafIconLookup.getIcon("checkmark")
          selectedIcon = getSelectedIcon("checkmark")
          disabledIcon = getDisabledIcon("checkmark")
        }
      }
      if (!enabled) {
        icon = disabledIcon ?: icon?.let { IconLoader.getDisabledIcon(it) }
        selectedIcon = disabledIcon ?: selectedIcon?.let { IconLoader.getDisabledIcon(it) }
      }
      if (myMaxIconWidth != -1 && myMaxIconHeight != -1) {
        if (icon != null) icon = SizedIcon(icon, myMaxIconWidth, myMaxIconHeight)
        if (selectedIcon != null) selectedIcon =
          SizedIcon(selectedIcon, myMaxIconWidth, myMaxIconHeight)
      }
      if (icon == null) icon = selectedIcon ?: myEmptyIcon
      val prependSeparator =
        (myListModel.isNotEmpty() || mySeparatorText != null) && myPrependWithSeparator
      assert(text != null) { "$action has no presentation" }
      myListModel.add(
        BMActionItem(
          bmNode,
          action,
          text!!,
          presentation.getClientProperty(keyToolTipText),
          enabled,
          icon,
          selectedIcon,
          prependSeparator,
          mySeparatorText,
          keyStroke
        )
      )
      myPrependWithSeparator = false
      mySeparatorText = null
    }
  }
}
