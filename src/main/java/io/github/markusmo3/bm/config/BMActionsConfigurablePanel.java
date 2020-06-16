// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package io.github.markusmo3.bm.config;

import com.intellij.icons.AllIcons.*;
import io.github.markusmo3.bm.actions.OpenBMXmlAction;
import io.github.markusmo3.bm.actions.ReloadBMXmlAction;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.*;

import com.intellij.ide.ui.customization.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.*;
import com.intellij.openapi.keymap.impl.ui.*;
import com.intellij.openapi.options.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.text.*;
import com.intellij.packageDependencies.ui.*;
import com.intellij.ui.*;
import org.jetbrains.annotations.*;

/**
 * Copied from {@link CustomizableActionsPanel}. MUST be in java because
 * <a href="https://youtrack.jetbrains.com/issue/KT-6660">IntelliJ GUI Form binding doesn't work
 * with kotlin</a>
 */
public class BMActionsConfigurablePanel {

  private JPanel myPanel;
  private JTree myActionsTree;
  private JPanel myTopPanel;
  private BMActionsSchemaState mySelectedSchemaState;

  public BMActionsConfigurablePanel() {
    Group rootGroup = new Group("root", null, null);
    final BetterMutableTreeNode root = new BetterMutableTreeNode(rootGroup);
    DefaultTreeModel model = new DefaultTreeModel(root);
    myActionsTree.setModel(model);
    myActionsTree.setRootVisible(true);
    myActionsTree.setShowsRootHandles(true);
    myActionsTree.setCellRenderer(new BMTreeCellRenderer());

    patchActionsTreeCorrespondingToSchema(root);

    TreeExpansionMonitor.install(myActionsTree);
    myTopPanel.setLayout(new BorderLayout());
    myTopPanel.add(setupFilterComponent(myActionsTree), BorderLayout.WEST);
    myTopPanel.add(createToolbar(), BorderLayout.CENTER);
  }

  private ActionToolbarImpl createToolbar() {
    ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
        .createActionToolbar(ActionPlaces.TOOLBAR, new DefaultActionGroup(getToolbarActions()),
            true);
    toolbar.setForceMinimumSize(true);
    toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    return toolbar;
  }

  @NotNull
  protected List<AnAction> getToolbarActions() {
    DefaultActionGroup additional = new DefaultActionGroup("Additional Actions", true);
    additional.getTemplatePresentation().setIcon(Actions.More);
    additional.add(new OpenBMXmlAction());
    additional.add(new ReloadBMXmlAction());
    additional.add(new RebuildActionsTreeAction(this));
    return new ArrayList<>(Arrays.asList(
        new AddActionAction(myActionsTree),
        new AddGroupAction(myActionsTree),
        new AddSeparatorAction(myActionsTree),
        new RemoveNodeAction(myActionsTree, myPanel),
        new EditNodeAction(myActionsTree),
        new Separator(),
        new MoveAction(myActionsTree, 1),
        new MoveAction(myActionsTree, -1),
        new MoveLevelAction(myActionsTree, 1),
        new MoveLevelAction(myActionsTree, -1),
        new SortNodesAction(myActionsTree),
        new Separator(),
        additional
//            ,new AnAction("import from vim") {
//              @Override
//              public void actionPerformed(@NotNull AnActionEvent e) {
//                BMActionsSchema.getInstance().importFromVimPlugin();
//              }
//            }
    ));
  }

  private static FilterComponent setupFilterComponent(JTree tree) {
    final TreeSpeedSearch mySpeedSearch = new TreeSpeedSearch(tree, new TreePathStringConvertor(),
        true) {
      @Override
      public boolean isPopupActive() {
        return /*super.isPopupActive()*/true;
      }

      @Override
      public void showPopup(String searchText) {
        //super.showPopup(searchText);
      }

      @Override
      protected boolean isSpeedSearchEnabled() {
        return /*super.isSpeedSearchEnabled()*/false;
      }

      @Override
      public void showPopup() {
        //super.showPopup();
      }
    };
    final FilterComponent filterComponent = new FilterComponent("CUSTOMIZE_ACTIONS", 5) {
      @Override
      public void filter() {
        mySpeedSearch.findAndSelectElement(getFilter());
      }
    };
    JTextField textField = filterComponent.getTextEditor();
    int[] keyCodes = {KeyEvent.VK_HOME, KeyEvent.VK_END, KeyEvent.VK_UP, KeyEvent.VK_DOWN};
    for (int keyCode : keyCodes) {
      new DumbAwareAction() {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          String filter = filterComponent.getFilter();
          if (!StringUtil.isEmpty(filter)) {
            mySpeedSearch.adjustSelection(keyCode, filter);
          }
        }
      }.registerCustomShortcutSet(keyCode, 0, textField);

    }
    return filterComponent;
  }

  public JPanel getPanel() {
    return myPanel;
  }

  public void apply() throws ConfigurationException {
    BMActionsSchema.getInstance().loadState(mySelectedSchemaState.deepCopy());
  }

  public void reset(boolean reload) {
    if (reload) {
      mySelectedSchemaState = BMActionsSchema.getInstance().getState().deepCopy();
    }
    patchActionsTreeCorrespondingToSchema(
        (DefaultMutableTreeNode) myActionsTree.getModel().getRoot());
    myActionsTree.setSelectionRow(0);
  }

  public boolean isModified() {
    return !BMActionsSchema.getInstance().getState().getXmlString()
        .equals(mySelectedSchemaState.getXmlString());
  }

  private DefaultMutableTreeNode createTreeNode(BMNode bmNode) {
    BetterMutableTreeNode treeNode = new BetterMutableTreeNode(bmNode);
    if (bmNode.isGroup()) {
      for (BMNode child : bmNode.getChildren()) {
        treeNode.add(createTreeNode(child));
      }
    }
    return treeNode;
  }

  private void patchActionsTreeCorrespondingToSchema(DefaultMutableTreeNode root) {
    root.removeAllChildren();
    if (mySelectedSchemaState != null) {
      root.setUserObject(mySelectedSchemaState.getRoot());
      for (BMNode rootBmNode : mySelectedSchemaState.getRoot()) {
        root.add(createTreeNode(rootBmNode));
      }
    }
    ((DefaultTreeModel) myActionsTree.getModel()).reload();
  }

}
