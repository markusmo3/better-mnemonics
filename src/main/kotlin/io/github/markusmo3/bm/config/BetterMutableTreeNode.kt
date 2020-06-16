package io.github.markusmo3.bm.config

import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

/**
 * Better version of the [DefaultMutableTreeNode] giving direct access to the
 * [DefaultMutableTreeNode#children] to allow sorting them.
 */
internal class BetterMutableTreeNode @JvmOverloads constructor(
  userObject: Any? = null, allowsChildren: Boolean = true
) : DefaultMutableTreeNode(userObject, allowsChildren) {

  fun getChildrenVector(): Vector<TreeNode> = children

}