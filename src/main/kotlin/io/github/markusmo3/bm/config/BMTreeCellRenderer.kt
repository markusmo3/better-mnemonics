package io.github.markusmo3.bm.config

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.QuickList
import com.intellij.openapi.keymap.impl.ui.ActionsTree
import com.intellij.openapi.keymap.impl.ui.Group
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ObjectUtils
import com.intellij.util.ui.UIUtil
import io.github.markusmo3.bm.BMUtils.toShortString
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

internal class BMTreeCellRenderer : ColoredTreeCellRenderer() {
  override fun customizeCellRenderer(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ) {
    if (value !is DefaultMutableTreeNode) return

    val obj = value.userObject
    var icon: Icon? = null
    if (obj is Group) {
      val name = obj.name
      append(name ?: ObjectUtils.notNull(obj.id, "<unnamed group>"))
      icon = ObjectUtils.notNull(obj.icon, AllIcons.Nodes.Folder)
    } else if (obj is BMNode) {
      val customText = obj.customText
      if (obj.isRoot()) {
        append("Root", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        icon = AllIcons.Modules.ExcludeRoot
      } else if (obj.isSeparator()) {
        if (customText != null) {
          append("----- $customText -----")
        } else {
          append("---------------")
        }
      } else if (obj.isGroup()) {
        append(obj.keyStroke.toShortString() + ". ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        append(customText ?: "<unnamed group>")
        icon = AllIcons.Nodes.Folder
      } else if (obj.isAction() && obj.actionId != null) {
        append(obj.keyStroke.toShortString() + ". ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        val action = ActionManager.getInstance().getAction(obj.actionId!!)
        if (customText != null) {
          append(customText, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES)
        }

        val name = action?.templatePresentation?.text
        if (name != null && !StringUtil.isEmptyOrSpaces(name)) {
          if (customText != null) {
            append("  ($name)")
          } else {
            append(name)
          }
        }
        toolTipText = "Action-ID: " + obj.actionId
        if (action != null) {
          val actionIcon = action.templatePresentation.icon
          if (actionIcon != null) {
            icon = actionIcon
          }
        }
      }
    } else if (obj is String) {
      val action = ActionManager.getInstance().getAction(obj)
      val name = action?.templatePresentation?.text
      append((if (!StringUtil.isEmptyOrSpaces(name)) name else obj)!!)
      if (action != null) {
        val actionIcon = action.templatePresentation.icon
        if (actionIcon != null) {
          icon = actionIcon
        }
      }
    } else if (obj is Pair<*, *>) {
      val actionId = obj.first as String
      val action = ActionManager.getInstance().getAction(actionId)
      append(if (action != null) action.templatePresentation.text else actionId)
      icon = obj.second as Icon
    } else if (obj is Separator) {
      append("-------------")
    } else if (obj is QuickList) {
      append(obj.name)
      icon = null // AllIcons.Actions.QuickList;
    } else require(obj == null) { "unknown userObject: $obj" }
    setIcon(ActionsTree.getEvenIcon(icon))
    foreground = UIUtil.getTreeForeground(selected, hasFocus)
    setIcon(icon)
  }
}