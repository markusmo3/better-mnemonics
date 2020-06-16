package io.github.markusmo3.bm.popup

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.actionSystem.impl.Utils
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.SizedIcon
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.LafIconLookup
import com.intellij.util.ui.LafIconLookup.getDisabledIcon
import com.intellij.util.ui.LafIconLookup.getSelectedIcon
import io.github.markusmo3.bm.config.BMNode
import io.github.markusmo3.bm.BMUtils.toShortString
import java.util.*
import javax.swing.Icon
import javax.swing.JComponent

class BMActionStepBuilder(
  private val myDataContext: DataContext, private val myShowDisabled: Boolean
) : PresentationFactory() {

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
          Utils.NOTHING_HERE,
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

  private fun calcMaxIconSize(actionGroup: ActionGroup) {
    val actions = actionGroup.getChildren(createActionEvent(actionGroup))
    for (action in actions) {
      if (action == null) continue
      if (action is ActionGroup) {
        if (!action.isPopup) {
          calcMaxIconSize(action)
          continue
        }
      }
      var icon = action.templatePresentation.icon
      if (icon == null && action is Toggleable) icon = EmptyIcon.ICON_16
      if (icon != null) {
        val width = icon.iconWidth
        val height = icon.iconHeight
        if (myMaxIconWidth < width) {
          myMaxIconWidth = width
        }
        if (myMaxIconHeight < height) {
          myMaxIconHeight = height
        }
      }
    }
  }

  private fun createActionEvent(actionGroup: AnAction): AnActionEvent {
    val actionEvent = AnActionEvent(
      null,
      myDataContext,
      myActionPlace,
      getPresentation(actionGroup),
      ActionManager.getInstance(),
      0
    )
    actionEvent.setInjectedContext(actionGroup.isInInjectedContext)
    return actionEvent
  }

  private fun appendActionsFromGroup(bmNode: BMNode) {
    val actionGroup: ActionGroup =
      DefaultActionGroup(bmNode.children.mapNotNull { it.action })
    val actionManager = ActionManager.getInstance()
    val newVisibleActions = Utils.expandActionGroup(
      false, actionGroup, this, myDataContext, myActionPlace
    ).associateBy { actionManager.getId(it) }
    for (bmChild in bmNode.children) {
      if (bmChild.isSeparator()) {
        myPrependWithSeparator = true
        mySeparatorText = bmChild.customText
      } else {

        appendAction(bmChild, newVisibleActions[bmChild.actionId])
      }
    }
  }

  private fun appendAction(bmNode: BMNode, actionParam: AnAction?) {
    var action: AnAction? = actionParam
    if (action == null) {
      action = bmNode.action ?: return
    }
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
            Toggleable.SELECTED_PROPERTY
          )
        ) {
          icon = LafIconLookup.getIcon("checkmark")
          selectedIcon = getSelectedIcon("checkmark")
          disabledIcon = getDisabledIcon("checkmark")
        }
      }
      if (!enabled) {
        icon = disabledIcon ?: IconLoader.getDisabledIcon(icon)
        selectedIcon = disabledIcon ?: IconLoader.getDisabledIcon(selectedIcon)
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
          presentation.getClientProperty(JComponent.TOOL_TIP_TEXT_KEY) as String?,
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
