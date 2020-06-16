package io.github.markusmo3.bm.config

import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.QuickListsManager
import com.intellij.openapi.keymap.impl.ui.ActionsTreeUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.*
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
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

  override fun createCenterPanel(): JComponent? {
    val rootGroup = ActionsTreeUtil.createMainGroup(
      null, null, QuickListsManager.instance.allQuickLists
    )
    val root = ActionsTreeUtil.createNode(rootGroup)
    val model = DefaultTreeModel(root)
    myTree = Tree()
    TreeUIHelper.getInstance().installTreeSpeedSearch(myTree, TreePathStringConvertor(), true)
    myTree.model = model
    myTree.isRootVisible = false
    myTree.cellRenderer = BMTreeCellRenderer()

    myEditPanel = BMEditPanel(true, null)
    myEditPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
    val panel = JPanel(BorderLayout())
    panel.add(myEditPanel, BorderLayout.SOUTH)
    myFilterComponent = setupFilterComponent(myTree)
    panel.add(myFilterComponent, BorderLayout.NORTH)
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
    if (treeSelectedActionIds?.size != 1) {
      return
    }
    val actionId = treeSelectedActionIds?.first() as? String? ?: return
    val actionManager = ActionManager.getInstance()
    val action = actionManager.getAction(actionId)
    if (action == null || action is ActionGroup || action is Separator) {
      return
    }
    lastExpandedPaths = TreeUtil.collectExpandedPaths(myTree)
    super.doOKAction()
  }

  val treeSelectedActionIds: Set<Any>?
    get() {
      val paths = myTree.selectionPaths ?: return null
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

  fun getCustomText(): String? {
    return myEditPanel.getCustomText()
  }

  fun getKeyStroke(): KeyStroke? {
    return myEditPanel.getKeyStroke()
  }

  override fun getDimensionServiceKey(): String? {
    return "#io.github.markusmo3.bm.config.BMFindAvailableActionsDialog"
  }

  private fun setupFilterComponent(tree: JTree): FilterComponent {
    val mySpeedSearch: TreeSpeedSearch = object : TreeSpeedSearch(
      tree, TreePathStringConvertor(), true
    ) {
      override fun isPopupActive(): Boolean {
        return  /*super.isPopupActive()*/true
      }

      override fun showPopup(searchText: String) {
        //super.showPopup(searchText);
      }

      override fun isSpeedSearchEnabled(): Boolean {
        return  /*super.isSpeedSearchEnabled()*/false
      }

      override fun showPopup() {
        //super.showPopup();
      }
    }
    val filterComponent: FilterComponent = object : FilterComponent("CUSTOMIZE_ACTIONS", 5) {
      override fun filter() {
        if (filter.isNotBlank()) {
          mySpeedSearch.findAndSelectElement(filter)
        }
      }
    }
    val textField = filterComponent.textEditor
    val keyCodes = intArrayOf(KeyEvent.VK_HOME, KeyEvent.VK_END, KeyEvent.VK_UP, KeyEvent.VK_DOWN)
    for (keyCode in keyCodes) {
      object : DumbAwareAction() {
        override fun actionPerformed(e: AnActionEvent) {
          val filter = filterComponent.filter
          if (!StringUtil.isEmpty(filter)) {
            mySpeedSearch.adjustSelection(keyCode, filter)
          }
        }
      }.registerCustomShortcutSet(keyCode, 0, textField)
    }
    return filterComponent
  }

  companion object {
    var lastExpandedPaths: MutableList<TreePath>? = null
  }
}