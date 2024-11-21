package io.github.markusmo3.bm.config;

import com.intellij.icons.AllIcons.*;
import com.intellij.ide.ui.customization.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.*;
import com.intellij.openapi.actionSystem.impl.*;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.keymap.impl.ui.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.text.*;
import com.intellij.packageDependencies.ui.*;
import com.intellij.ui.*;
import com.intellij.ui.treeStructure.*;
import io.github.markusmo3.bm.actions.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.stream.*;
import javax.swing.*;
import javax.swing.tree.*;
import org.jetbrains.annotations.*;

/**
 * Copied from {@link CustomizableActionsPanel}. MUST be in java because
 * <a href="https://youtrack.jetbrains.com/issue/KT-6660">IntelliJ GUI Form binding doesn't work
 * with kotlin</a>
 */
public class BMActionsConfigurablePanel {

  private JSplitPane myRoot;

  private JPanel myLeftRootPanel;
  private JPanel myTopLeftPanel;
  private JTree myActionsTree;

  private JPanel myRightRootPanel;
  private JPanel myTopRightPanel;
  private Tree myChooserActionsTree;
  private JTextField customTextTextfield;
  private JButton addToTheLeftButton;
  private BMKeyStrokeTextfield shortcutTextField;

  private BMActionsSchemaState mySelectedSchemaState;

  public BMActionsConfigurablePanel() {
    myRoot.setDividerLocation(1.0);
    myRoot.setBorder(BorderFactory.createEmptyBorder());
//    BasicSplitPaneUI flatDividerSplitPaneUI = new BasicSplitPaneUI() {
//      @Override
//      public BasicSplitPaneDivider createDefaultDivider() {
//        return new BasicSplitPaneDivider(this) {
//          @Override
//          public void setBorder(Border b) {
//          }
//        };
//      }
//    };
//    myRoot.setUI(flatDividerSplitPaneUI);
    initLeft();
    initRight();
  }

  private void initRight() {
    Group rootGroup = ActionsTreeUtil.createMainGroup(
        null, null, QuickListsManager.getInstance().getAllQuickLists()
    );
    DefaultMutableTreeNode rootNode = ActionsTreeUtil.createNode(rootGroup);
    DefaultTreeModel model = new DefaultTreeModel(rootNode);
    TreeUIHelper.getInstance().installTreeSpeedSearch(myChooserActionsTree, new TreePathStringConvertor(), true);
    myChooserActionsTree.setModel(model);
    myChooserActionsTree.setRootVisible(false);
    myChooserActionsTree.setCellRenderer(new BMTreeCellRenderer());
    myTopRightPanel.setLayout(new BorderLayout());
    myTopRightPanel.add(setupFilterComponent(myChooserActionsTree), BorderLayout.CENTER);
    new DoubleClickListener() {
      public boolean onDoubleClick(MouseEvent event) {
        doRightOKAction();
        return true;
      }
    }.installOn(myChooserActionsTree);
    addToTheLeftButton.addActionListener(a -> doRightOKAction());
  }

  private void createUIComponents() {
    shortcutTextField = new BMKeyStrokeTextfield();
  }

  private void doRightOKAction() {
    TreePath[] selectionPaths = myChooserActionsTree.getSelectionPaths();
    if (selectionPaths == null) {
      return;
    }
    String customText = customTextTextfield.getText();
    if (customText.isEmpty()) {
      customText = null;
    }
    KeyStroke keyStroke = shortcutTextField.getKeyStroke();

    String finalCustomText = customText;
    Set<BMNode> actionSet = Arrays.stream(selectionPaths)
        .map(TreePath::getLastPathComponent)
        .filter(it -> it instanceof DefaultMutableTreeNode)
        .map(it -> ((DefaultMutableTreeNode) it).getUserObject())
        .map(it -> {
          if (it instanceof String) {
            return (String) it;
          } else if (it instanceof QuickList) {
            return ((QuickList) it).getActionId();
          } else {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .map(it -> BMNode.newAction(it, keyStroke, finalCustomText))
        .collect(Collectors.toSet());
    AddNodeAction.addNodesToTree(actionSet, myActionsTree);
  }

  private void initLeft() {
    Group rootGroup = new Group("root", null, (Icon) null);
    final BetterMutableTreeNode root = new BetterMutableTreeNode(rootGroup);
    DefaultTreeModel model = new DefaultTreeModel(root);
    myActionsTree.setModel(model);
    myActionsTree.setRootVisible(true);
    myActionsTree.setShowsRootHandles(true);
    myActionsTree.setCellRenderer(new BMTreeCellRenderer());
    patchActionsTreeCorrespondingToSchema(root);
    TreeExpansionMonitor.install(myActionsTree);
    myTopLeftPanel.setLayout(new BorderLayout());
    myTopLeftPanel.add(setupFilterComponent(myActionsTree), BorderLayout.WEST);
    myTopLeftPanel.add(createToolbar(), BorderLayout.CENTER);
  }

  private ActionToolbarImpl createToolbar() {
    DefaultActionGroup additional = new DefaultActionGroup("Additional Actions", true);
    additional.getTemplatePresentation().setIcon(Actions.More);
    additional.add(new OpenBMXmlAction());
    additional.add(new ReloadBMXmlAction());
    additional.add(new RebuildActionsTreeAction(this));
    List<AnAction> toolbarActions = new ArrayList<>(Arrays.asList(
//        new AddActionAction(myActionsTree),
        new AddGroupAction(myActionsTree),
        new AddSeparatorAction(myActionsTree),
        new RemoveNodeAction(myActionsTree),
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

    ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
        .createActionToolbar(ActionPlaces.TOOLBAR, new DefaultActionGroup(toolbarActions),
            true);
    toolbar.setForceMinimumSize(true);
    toolbar.setLayoutStrategy(ToolbarLayoutStrategy.NOWRAP_STRATEGY);
    return toolbar;
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

  public JComponent getPanel() {
    return myRoot;
  }

  public void apply() {
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

  private static DefaultMutableTreeNode createTreeNode(BMNode bmNode) {
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
