package io.github.markusmo3.bm.config

import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.QuickListsManager
import com.intellij.openapi.keymap.impl.ui.ActionsTreeUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.FilterComponent
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

internal class BMFindAvailableActionsDialog : DialogWrapper(false) {

  private lateinit var myFilterComponent: FilterComponent
  private lateinit var myTree: JTree
  private lateinit var myEditPanel: BMEditPanel

  init {
    title = IdeBundle.message("action.choose.actions.to.add")
    init()
  }

  override fun createCenterPanel(): JComponent {
    val rootGroup = ActionsTreeUtil.createMainGroup(
      null, null, QuickListsManager.getInstance().allQuickLists
    )
    val root = ActionsTreeUtil.createNode(rootGroup)
    val model = DefaultTreeModel(root)
    myTree = Tree()
    TreeUIHelper.getInstance().installTreeSpeedSearch(myTree, TreePathStringConvertor(), true)
    myTree.model = model
    myTree.isRootVisible = false
    myTree.cellRenderer = BMTreeCellRenderer()

    myEditPanel = BMEditPanel(isShortcutEditingEnabled = true,
            isGlobalShortcutEditingEnabled = false, oldBmNode = null)
    myEditPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
    val panel = JPanel(BorderLayout())
    panel.add(myEditPanel, BorderLayout.SOUTH)
    panel.add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER)
    object : DoubleClickListener() {
      override fun onDoubleClick(event: MouseEvent): Boolean {
        doOKAction()
        return true
      }
    }.installOn(myTree)
    if (lastExpandedPaths != null) {
      TreeUtil.restoreExpandedPaths(myTree, lastExpandedPaths!!)
    }
    return panel
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return myFilterComponent.textEditor
  }

  override fun doOKAction() {
    if (treeSelectedActionIds.isEmpty()) {
      return
    }
    val actionId = treeSelectedActionIds.first() as? String? ?: return
    val actionManager = ActionManager.getInstance()
    val action = actionManager.getAction(actionId)
    if (action == null || action is ActionGroup || action is Separator) {
      return
    }
    lastExpandedPaths = TreeUtil.collectExpandedPaths(myTree)
    super.doOKAction()
  }

  val treeSelectedActionIds: Set<Any>
    get() {
      val paths = myTree.selectionPaths ?: return emptySet()
      val actions: MutableSet<Any> = HashSet()
      for (path in paths) {
        val node = path.lastPathComponent
        if (node is DefaultMutableTreeNode) {
          val userObject = node.userObject
          actions.add(userObject)
        }
      }
      return actions
    }

  override fun getDimensionServiceKey(): String {
    return "#io.github.markusmo3.bm.config.BMFindAvailableActionsDialog"
  }

  companion object {
    var lastExpandedPaths: MutableList<TreePath>? = null
  }
}
