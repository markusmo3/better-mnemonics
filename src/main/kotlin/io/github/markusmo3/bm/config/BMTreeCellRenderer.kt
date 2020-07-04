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
import io.github.markusmo3.bm.BMUtils.blend
import io.github.markusmo3.bm.BMUtils.toShortString
import java.awt.Color
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

    toolTipText = null
    foreground = UIUtil.getTreeForeground(selected, hasFocus)

    val obj = value.userObject
    var icon: Icon? = null
    if (obj is Group) {
      val name = obj.name
      append(name ?: ObjectUtils.notNull(obj.id, "<unnamed group>"))
      icon = ObjectUtils.notNull(obj.icon, AllIcons.Nodes.Folder)
    } else if (obj is BMNode) {
      val customText = obj.customText
      val keyStroke = obj.keyStroke
      var nodeName: String? = "<unknown>"

      if (obj.isRoot()) {
        append("Root", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        icon = AllIcons.Modules.ExcludeRoot
      } else if (obj.isSeparator()) {
        if (customText != null) {
          nodeName = customText
          append("----- $customText -----")
        } else {
          nodeName = "<unnamed separator>"
          append("---------------")
        }
      } else if (obj.isGroup()) {
        if (obj.globalKeyStroke != null) {
          append(obj.globalKeyStroke.toShortString() + ": ", SimpleTextAttributes.LINK_BOLD_ATTRIBUTES)
        }
        append(keyStroke.toShortString() + ". ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        nodeName = customText ?: "<unnamed group>"
        append(nodeName)
        icon = AllIcons.Nodes.Folder
      } else if (obj.isAction() && obj.actionId != null) {
        append(keyStroke.toShortString() + ". ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        val action = ActionManager.getInstance().getAction(obj.actionId!!)
        if (customText != null) {
          append(customText, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES)
        }

        nodeName = action?.templatePresentation?.text
        if (nodeName != null && !StringUtil.isEmptyOrSpaces(nodeName)) {
          if (customText != null) {
            append("  ($nodeName)")
          } else {
            append(nodeName)
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

      val existingKeystrokeBmNode = getAlreadyExistingKeystrokeBMNode(value, obj)
      if (existingKeystrokeBmNode != null) {
        icon = AllIcons.General.Warning
        foreground = UIUtil.getTreeForeground(selected, hasFocus).blend(Color.RED)
        toolTipText = "KeyStroke ${keyStroke.toShortString()} already assigned to $nodeName"
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
    setIcon(icon)
  }

  private fun getAlreadyExistingKeystrokeBMNode(node: DefaultMutableTreeNode, bmNode: BMNode): BMNode? {
    val keyStroke = bmNode.keyStroke ?: return null

    for (child in node.parent.children()) {
      val siblingBmNode = (child as DefaultMutableTreeNode).userObject as? BMNode ?: continue
      if (siblingBmNode == bmNode) continue
      val siblingKeyStroke = siblingBmNode.keyStroke ?: continue
      if (keyStroke == siblingKeyStroke) {
        return siblingBmNode
      }
    }
    return null
  }
}