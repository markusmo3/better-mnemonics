package io.github.markusmo3.bm.config

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.tree.TreeUtil
import io.github.markusmo3.bm.BMUtils.toSortIndex
import io.github.markusmo3.bm.config.BMNode.Companion.newGroup
import io.github.markusmo3.bm.config.BMNode.Companion.newSeparator
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

internal class RebuildActionsTreeAction(
  private val configurablePanel: BMActionsConfigurablePanel
) : AnAction("Rebuild BetterMnemonics Tree", null, AllIcons.Actions.Refresh) {

  override fun actionPerformed(e: AnActionEvent) {
    configurablePanel.reset(false)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = true
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

}

internal abstract class TreeSelectionAction(
  protected val myActionsTree: JTree,
  text: String? = null,
  description: String? = null,
  icon: Icon? = null
) : DumbAwareAction(text, description, icon) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = true
    val selectionPaths: Array<TreePath>? = myActionsTree.selectionPaths
    if (selectionPaths == null) {
      e.presentation.isEnabled = false
      return
    }
    for (path in selectionPaths) {
      if (!isRootSelectable() && path.path.size <= 1 || isRootSelectable() && path.path.isEmpty()) {
        e.presentation.isEnabled = false
        return
      }
    }
  }

  protected open fun isRootSelectable(): Boolean = false

  protected fun isSingleSelection(): Boolean {
    val selectionPaths: Array<TreePath>? = myActionsTree.selectionPaths
    return selectionPaths != null && selectionPaths.size == 1
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}

internal class SortNodesAction(myActionsTree: JTree) : TreeSelectionAction(
  myActionsTree, "Sort Alphabetically by KeyStroke", null, AllIcons.ObjectBrowser.Sorted
) {

  override fun actionPerformed(e: AnActionEvent) {
    val expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree)
    val selectionPath: TreePath = myActionsTree.leadSelectionPath
    val selectedNode = selectionPath.lastPathComponent as BetterMutableTreeNode
    val selectedBmNode = selectedNode.userObject as BMNode
    if (selectedBmNode.isGroup()) {
      selectedBmNode.sortBy { bmNode -> bmNode.keyStroke.toSortIndex() }
      selectedNode.getChildrenVector().sortBy {
        val bmNode = (it as BetterMutableTreeNode).userObject as BMNode
        bmNode.keyStroke.toSortIndex()
      }
      (myActionsTree.model as DefaultTreeModel).reload()
      TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths)
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isSingleSelection()
    }
    if (e.presentation.isEnabled) {
      val selectedBmNode =
        (myActionsTree.leadSelectionPath?.lastPathComponent as? BetterMutableTreeNode?)?.userObject as? BMNode?
      if (selectedBmNode != null) {
        e.presentation.isEnabled = selectedBmNode.isGroup()
      }
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}

internal abstract class AddNodeAction(
  myActionsTree: JTree, text: String? = null, description: String? = null, icon: Icon? = null
) : TreeSelectionAction(myActionsTree, text, description, icon) {

  open fun getNewBmNode(): BMNode? = null

  open fun getNewBmNodes(): Set<BMNode> {
    val newBmNode = getNewBmNode()
    return if (newBmNode != null) {
      setOf(newBmNode)
    } else {
      emptySet()
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val newBmNodes = getNewBmNodes()
    addNodesToTree(newBmNodes, myActionsTree)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isSingleSelection()
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  companion object {
    @JvmStatic
    fun addNodesToTree(
      newBmNodes: Set<BMNode>,
      actionTree: JTree
    ) {
      val expandedPaths = TreeUtil.collectExpandedPaths(actionTree)
      val selectionPath: TreePath = actionTree.leadSelectionPath
      val selectedNode = selectionPath.lastPathComponent as DefaultMutableTreeNode
      val selectedBmNode = selectedNode.userObject as BMNode
      if (newBmNodes.isEmpty()) return
      for (newBmNode in newBmNodes) {
        if (selectedBmNode.isGroup() || selectedBmNode.isRoot()) {
          selectedBmNode.children.add(0, newBmNode)
          selectedNode.insert(BetterMutableTreeNode(newBmNode), 0)
          expandedPaths.add(selectionPath)
        } else {
          val parentNode = selectedNode.parent as DefaultMutableTreeNode
          val parentBmNode = parentNode.userObject as BMNode
          val newIndex = parentNode.getIndex(selectedNode) + 1
          parentBmNode.children.add(newIndex, newBmNode)
          parentNode.insert(BetterMutableTreeNode(newBmNode), newIndex)
        }
      }
      (actionTree.model as DefaultTreeModel).reload()
      TreeUtil.restoreExpandedPaths(actionTree, expandedPaths)
      actionTree.setSelectionRow(actionTree.getRowForPath(selectionPath) + newBmNodes.size)
    }
  }
}

internal class AddSeparatorAction(myActionsTree: JTree) : AddNodeAction(
  myActionsTree, "Add &Separator", null, AllIcons.General.SeparatorH
) {
  override fun getNewBmNode(): BMNode? {
    val dlg = BMEditDialog(isShortcutEditingEnabled = false, isGlobalShortcutEditingEnabled = false,
            oldBmNode = null)
    if (dlg.showAndGet()) {
      return newSeparator(dlg.getCustomText())
    }
    return null
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}

internal class AddGroupAction(myActionsTree: JTree) : AddNodeAction(
  myActionsTree, "Add &Group", null, AllIcons.ToolbarDecorator.AddFolder
) {
  override fun getNewBmNode(): BMNode? {
    val dlg = BMEditDialog(isShortcutEditingEnabled = true, isGlobalShortcutEditingEnabled = true,
            oldBmNode = null)
    if (dlg.showAndGet()) {
      val newGroup = newGroup(dlg.getKeyStroke(), dlg.getCustomText())
      newGroup.globalKeyStroke = dlg.getGlobalKeyStroke()
      return newGroup
    }
    return null
  }

  override fun isRootSelectable(): Boolean = true

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}

//internal class AddActionAction(myActionsTree: JTree) : AddNodeAction(
//  myActionsTree, IdeBundle.message("button.add.action"), null, AllIcons.General.Add
//) {
//  override fun getNewBmNodes(): Set<BMNode> {
//    val dlg = BMFindAvailableActionsDialog()
//    if (dlg.showAndGet()) {
//      val toAddSet: Set<Any> = dlg.treeSelectedActionIds
//      return toAddSet.mapNotNull { toAdd: Any ->
//        if (toAdd is AnAction) {
//          val id = ActionManager.getInstance().getId(toAdd)
//          if (id != null) {
//            return@mapNotNull BMNode.newAction(id, dlg.getKeyStroke(), dlg.getCustomText())
//          }
//        } else if (toAdd is String) {
//          val action = ActionManager.getInstance().getAction(toAdd)
//          if (action != null) {
//            return@mapNotNull BMNode.newAction(toAdd, dlg.getKeyStroke(), dlg.getCustomText())
//          }
//        }
//        return@mapNotNull null
//      }.toSet()
//    }
//    return emptySet()
//  }
//}

internal class RemoveNodeAction(myActionsTree: JTree) : TreeSelectionAction(
  myActionsTree, "R&emove", null, AllIcons.General.Remove
) {

  override fun actionPerformed(e: AnActionEvent) {
    val selectedRowIndex = myActionsTree.minSelectionRow
    val expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree)
    val selectionPaths: Array<TreePath>? = myActionsTree.selectionPaths
    if (selectionPaths != null) {
      for (path in selectionPaths) {
        val selectedNode = path.lastPathComponent as DefaultMutableTreeNode
        val parentNode = selectedNode.parent as DefaultMutableTreeNode
        val parentBmNode = parentNode.userObject as BMNode
        val index = parentNode.getIndex(selectedNode)
        selectedNode.removeFromParent()
        parentBmNode.removeAt(index)
      }
      (myActionsTree.model as DefaultTreeModel).reload()
    }
    TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths)
    myActionsTree.setSelectionRow(selectedRowIndex)
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}

internal class EditNodeAction(myActionsTree: JTree) : TreeSelectionAction(
  myActionsTree, "&Edit", null, AllIcons.Actions.Edit
) {

  override fun actionPerformed(e: AnActionEvent) {
    val selectedNode =
      myActionsTree.leadSelectionPath?.lastPathComponent as? DefaultMutableTreeNode?
    val selectedBmNode = selectedNode?.userObject as? BMNode?
    if (selectedBmNode != null) {
      editNode(selectedBmNode)
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isSingleSelection()
    }
  }

  companion object {
    fun editNode(selectedBmNode: BMNode) {
      val dlg = BMEditDialog(!selectedBmNode.isSeparator(), selectedBmNode.isGroup(), selectedBmNode)
      if (dlg.showAndGet()) {
        selectedBmNode.customText = dlg.getCustomText()
        dlg.getKeyStroke()?.let { selectedBmNode.keyStroke = it }
        selectedBmNode.globalKeyStroke = dlg.getGlobalKeyStroke()
      }
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}

internal class MoveLevelAction(myActionsTree: JTree, private val dir: Int) : TreeSelectionAction(
  myActionsTree,
  text = if (dir > 0) "Move &Right" else "Move &Left",
  icon = if (dir > 0) AllIcons.General.ArrowRight else AllIcons.General.ArrowLeft
) {
  override fun actionPerformed(e: AnActionEvent) {
    val expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree)
    val selectionPath = myActionsTree.leadSelectionPath
    if (selectionPath != null) {
      val node = selectionPath.lastPathComponent as DefaultMutableTreeNode
      var newSelectionPath: TreePath? = null
      if (dir > 0) {
        node.previousSibling?.let { sibling ->
          val siblingBmNode = sibling.userObject as BMNode
          if (!siblingBmNode.isGroup()) {
            return@let
          }
          val parentBmNode = (node.parent as DefaultMutableTreeNode).userObject as BMNode
          val selectedBmNode = node.userObject as BMNode
          sibling.add(node)
          parentBmNode.remove(selectedBmNode)
          siblingBmNode.add(selectedBmNode)

          newSelectionPath =
            selectionPath.parentPath.pathByAddingChild(sibling).pathByAddingChild(node)
        }
      } else {
        (node.parent?.parent as? DefaultMutableTreeNode?)?.let { grandParent ->
          val parentOfParentBmNode = grandParent.userObject as BMNode
          val parentBmNode = (node.parent as DefaultMutableTreeNode).userObject as BMNode
          val selectedBmNode = node.userObject as BMNode
          val indexOfParentInGrandParent = parentOfParentBmNode.indexOf(parentBmNode) + 1
          grandParent.insert(node, indexOfParentInGrandParent)
          parentBmNode.remove(selectedBmNode)
          parentOfParentBmNode.add(indexOfParentInGrandParent, selectedBmNode)

          newSelectionPath = selectionPath.parentPath.parentPath.pathByAddingChild(node)
        }
      }
      (myActionsTree.model as DefaultTreeModel).reload()
      TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths)
      newSelectionPath?.let { myActionsTree.setSelectionRow(myActionsTree.getRowForPath(it)) }
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isSingleSelection()
    }
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isMoveLevelSupported()
    }
  }

  private fun isMoveLevelSupported(): Boolean {
    val node = myActionsTree.leadSelectionPath?.lastPathComponent as DefaultMutableTreeNode?
        ?: return false
    return if (dir > 0) {
      node.previousSibling != null && (node.previousSibling.userObject as BMNode).isGroup()
    } else {
      (node.parent?.parent as? DefaultMutableTreeNode) != null
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}

internal class MoveAction(myActionsTree: JTree, private val dir: Int) : TreeSelectionAction(
  myActionsTree,
  text = if (dir > 0) IdeBundle.message("button.move.down") else IdeBundle.message("button.move.up"),
  icon = if (dir > 0) AllIcons.Actions.MoveDown else AllIcons.Actions.MoveUp
) {
  override fun actionPerformed(e: AnActionEvent) {
    val expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree)
    val selectionPaths = myActionsTree.selectionPaths
    if (selectionPaths != null && selectionPaths.isNotEmpty()) {
      for (selectionPath in selectionPaths) {
        val node = selectionPath.lastPathComponent as DefaultMutableTreeNode
        val parent = node.parent as DefaultMutableTreeNode
        parent.getIndex(node)
      }

      selectionPaths.map {
        val node = it.lastPathComponent as DefaultMutableTreeNode
        val parent = node.parent as DefaultMutableTreeNode
        Triple(node, parent, parent.getIndex(node))
      }.sortedBy { if (dir > 0) -it.third else it.third }.forEach { (node, parent, indexOf) ->
          val children = parent.userObject as BMNode
          val bmNode = node.userObject as BMNode
          children.remove(bmNode)
          val adder = if (dir > 0) 1 else -1
          val newIndex = (indexOf + adder).coerceIn(0, parent.childCount - 1)
          children.add(newIndex, bmNode)
          parent.insert(node, newIndex)
        }
      (myActionsTree.model as DefaultTreeModel).reload()
      TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths)
      myActionsTree.selectionPaths = selectionPaths
    }
  }


  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isMoveSupported()
    }
  }

  private fun isMoveSupported(): Boolean {
    val selectionPaths = myActionsTree.selectionPaths
    if (selectionPaths != null) {
      var parent: DefaultMutableTreeNode? = null
      for (treePath in selectionPaths) {
        if (treePath.lastPathComponent != null) {
          val node = treePath.lastPathComponent as DefaultMutableTreeNode
          if (parent == null) {
            parent = node.parent as? DefaultMutableTreeNode
          }
          if (parent == null || parent !== node.parent) {
            return false
          }
          if (dir > 0) {
            if (parent.getIndex(node) == parent.childCount - 1) {
              return false
            }
          } else {
            if (parent.getIndex(node) == 0) {
              return false
            }
          }
        }
      }
      return true
    }
    return false
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

}
