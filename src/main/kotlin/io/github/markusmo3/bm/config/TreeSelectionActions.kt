package io.github.markusmo3.bm.config

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.tree.TreeUtil
import io.github.markusmo3.bm.BMUtils.toSortIndex
import io.github.markusmo3.bm.config.BMNode.Companion.newGroup
import io.github.markusmo3.bm.config.BMNode.Companion.newSeparator
import java.awt.event.KeyEvent
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

internal class RebuildActionsTreeAction(
  private val configurablePanel: BMActionsConfigurablePanel
) : AnAction("Rebuild BetterMnemonics Tree", null, AllIcons.Actions.Refresh) {

  init {
    templatePresentation.isEnabled = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    configurablePanel.reset(false)
  }

  override fun update(e: AnActionEvent) {
    templatePresentation.isEnabled = true
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
}

internal class SortNodesAction(myActionsTree: JTree) : TreeSelectionAction(
  myActionsTree, "Sort Alphabetically by KeyStroke", null, AllIcons.ObjectBrowser.Sorted
) {
  init {
    templatePresentation.isEnabled = true
  }

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
}

internal abstract class AddNodeAction constructor(
  myActionsTree: JTree, text: String? = null, description: String? = null, icon: Icon? = null
) : TreeSelectionAction(myActionsTree, text, description, icon) {

  override fun actionPerformed(e: AnActionEvent) {
    val expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree)
    val selectionPath: TreePath = myActionsTree.leadSelectionPath
    val selectedNode = selectionPath.lastPathComponent as DefaultMutableTreeNode
    val selectedBmNode = selectedNode.userObject as BMNode
    val newBmNode = getNewBmNode() ?: return
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
    (myActionsTree.model as DefaultTreeModel).reload()
    TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths)
    myActionsTree.setSelectionRow(myActionsTree.getRowForPath(selectionPath) + 1)
  }

  abstract fun getNewBmNode(): BMNode?

  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isSingleSelection()
    }
  }
}

internal class AddSeparatorAction(myActionsTree: JTree) : AddNodeAction(
  myActionsTree, IdeBundle.message("button.add.separator"), null, AllIcons.General.SeparatorH
) {
  override fun getNewBmNode(): BMNode? {
    val dlg = BMEditDialog(false, null)
    if (dlg.showAndGet()) {
      return newSeparator(dlg.getCustomText())
    }
    return null
  }
}

internal class AddGroupAction(myActionsTree: JTree) : AddNodeAction(
  myActionsTree, "Add &Group", null, AllIcons.ToolbarDecorator.AddFolder
) {
  override fun getNewBmNode(): BMNode? {
    val dlg = BMEditDialog(true, null)
    if (dlg.showAndGet()) {
      return newGroup(dlg.getKeyStroke(), dlg.getCustomText())
    }
    return null
  }

  override fun isRootSelectable(): Boolean = true
}

internal class AddActionAction(myActionsTree: JTree) : AddNodeAction(
  myActionsTree, IdeBundle.message("button.add.action"), null, AllIcons.General.Add
) {
  override fun getNewBmNode(): BMNode? {
    val dlg = BMFindAvailableActionsDialog()
    if (dlg.showAndGet()) {
      val toAdd: Set<Any> = dlg.treeSelectedActionIds ?: return null
      val first = toAdd.first()
      if (first is AnAction) {
        val id = ActionManager.getInstance().getId(first)
        if (id != null) {
          return BMNode.newAction(id, dlg.getKeyStroke(), dlg.getCustomText())
        }
      } else if (first is String) {
        val action = ActionManager.getInstance().getAction(first)
        if (action != null) {
          return BMNode.newAction(first, dlg.getKeyStroke(), dlg.getCustomText())
        }
      }
    }
    return null
  }
}

internal class RemoveNodeAction(myActionsTree: JTree, myPanel: JPanel) : TreeSelectionAction(
  myActionsTree, IdeBundle.message("button.remove"), null, AllIcons.General.Remove
) {

  init {
    val shortcutSet = KeymapUtil.filterKeyStrokes(
      CommonShortcuts.getDelete(),
      KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
      KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
    )
    shortcutSet?.let { registerCustomShortcutSet(it, myPanel) }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree)
    val selectionPaths: Array<TreePath>? = myActionsTree.selectionPaths
    if (selectionPaths != null) {
      var lastSelectedRow = 0
      for (path in selectionPaths) {
        val selectedNode = path.lastPathComponent as DefaultMutableTreeNode
        val selectedBmNode = selectedNode.userObject as BMNode
        val parentNode = selectedNode.parent as DefaultMutableTreeNode
        val parentBmNode = parentNode.userObject as BMNode
        lastSelectedRow = myActionsTree.getRowForPath(path)
        selectedNode.removeFromParent()
        parentBmNode.remove(selectedBmNode)
      }
      (myActionsTree.model as DefaultTreeModel).reload()
      myActionsTree.setSelectionRow(lastSelectedRow)
    }
    TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths)
  }

}

internal class EditNodeAction(myActionsTree: JTree) : TreeSelectionAction(
  myActionsTree, IdeBundle.message("button.edit"), null, AllIcons.Actions.Edit
) {

  override fun actionPerformed(e: AnActionEvent) {
    val selectedNode =
      myActionsTree.leadSelectionPath?.lastPathComponent as? DefaultMutableTreeNode?
    val selectedBmNode = selectedNode?.userObject as? BMNode?
    if (selectedBmNode != null) {
      val dlg = BMEditDialog(!selectedBmNode.isSeparator(), selectedBmNode)
      if (dlg.showAndGet()) {
        selectedBmNode.customText = dlg.getCustomText()
        dlg.getKeyStroke()?.let { selectedBmNode.keyStroke = it }
      }
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isSingleSelection()
    }
  }

}

internal class MoveLevelAction(myActionsTree: JTree, val dir: Int) : TreeSelectionAction(
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
          grandParent.add(node)
          parentBmNode.remove(selectedBmNode)
          parentOfParentBmNode.add(selectedBmNode)

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
    val node =
      myActionsTree.leadSelectionPath?.lastPathComponent as DefaultMutableTreeNode? ?: return false
    if (dir > 0) {
      return node.previousSibling != null && (node.previousSibling.userObject as BMNode).isGroup()
    } else {
      return (node.parent?.parent as? DefaultMutableTreeNode) != null
    }
  }
}

internal class MoveAction(myActionsTree: JTree, val dir: Int) : TreeSelectionAction(
  myActionsTree,
  text = if (dir > 0) IdeBundle.message("button.move.down") else IdeBundle.message("button.move.up"),
  icon = if (dir > 0) AllIcons.Actions.MoveDown else AllIcons.Actions.MoveUp
) {
  override fun actionPerformed(e: AnActionEvent) {
    val expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree)
    val selectionPath = myActionsTree.leadSelectionPath
    if (selectionPath != null) {
      val node = selectionPath.lastPathComponent as DefaultMutableTreeNode
      val parent = node.parent as DefaultMutableTreeNode
      val (_, _, _, _, children) = parent.userObject as BMNode
      val bmNode = node.userObject as BMNode
      val indexOf = children.indexOf(bmNode)
      children.remove(bmNode)
      val adder = if (dir > 0) 1 else -1
      val newIndex = (indexOf + adder).coerceIn(0, parent.childCount - 1)
      children.add(newIndex, bmNode)
      parent.insert(node, newIndex)
      (myActionsTree.model as DefaultTreeModel).reload()
      TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths)
      myActionsTree.setSelectionRow(myActionsTree.getRowForPath(selectionPath))
    }
  }


  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isSingleSelection()
    }
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = isMoveSupported()
    }
  }

  private fun isMoveSupported(): Boolean {
    val selectionPaths = myActionsTree.selectionPaths
    if (selectionPaths != null) {
      var parent: DefaultMutableTreeNode? = null
      for (treePath in selectionPaths) if (treePath.lastPathComponent != null) {
        val node = treePath.lastPathComponent as DefaultMutableTreeNode
        if (parent == null) {
          parent = node.parent as DefaultMutableTreeNode
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
      return true
    }
    return false
  }

}