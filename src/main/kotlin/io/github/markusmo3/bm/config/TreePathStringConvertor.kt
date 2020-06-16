package io.github.markusmo3.bm.config

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.util.containers.Convertor
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

internal class TreePathStringConvertor : Convertor<TreePath, String> {
  override fun convert(o: TreePath): String {
    val node = o.lastPathComponent
    if (node is DefaultMutableTreeNode) {
      val obj = node.userObject
      if (obj is BMNode && !obj.isAction()) return obj.customText ?: "sometext"
      val actionId: String
      actionId = if (obj is String) {
        obj
      } else if (obj is Pair<*, *>) {
        obj.first as String
      } else if (obj is BMNode && obj.isAction() && obj.actionId != null) {
        obj.actionId!!
      } else {
        return ""
      }
      val action = ActionManager.getInstance().getAction(actionId)
      if (action != null) {
        return action.templatePresentation.text
      }
    }
    return ""
  }
}