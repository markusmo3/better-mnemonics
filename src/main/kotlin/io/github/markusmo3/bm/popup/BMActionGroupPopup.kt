package io.github.markusmo3.bm.popup

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.KeepPopupOnPerform
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ActionMenu
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.util.Condition
import com.intellij.ui.popup.WizardPopup
import com.intellij.ui.popup.list.ListPopupImpl
import com.intellij.util.ObjectUtils
import com.intellij.util.containers.ContainerUtil
import io.github.markusmo3.bm.config.BMActionsSchema
import io.github.markusmo3.bm.config.BMNode
import io.github.markusmo3.bm.config.EditNodeAction
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.function.Supplier
import javax.swing.AbstractAction
import javax.swing.JList
import javax.swing.KeyStroke
import javax.swing.ListCellRenderer
import javax.swing.event.ListSelectionEvent

class BMActionGroupPopup : ListPopupImpl {
  private var myDisposeCallback: Runnable?
  private val myComponent: Component?
  private val myActionPlace: String

  constructor(
    project: Project?,
    parent: BMActionGroupPopup,
    listPopupStep: ListPopupStep<*>,
    parentValue: Any?
  ) : super(project, parent, listPopupStep, parentValue) {
    myDisposeCallback = null
    myComponent = null
    myActionPlace = ActionPlaces.UNKNOWN
    initSomeStuff()
    setMaxRowCount(BMActionsSchema.getInstance().state.maxRowCount)
  }

  constructor(
    aParent: WizardPopup?,
    step: ListPopupStep<*>,
    disposeCallback: Runnable?,
    dataContext: DataContext,
    actionPlace: String?,
    parentValue: Any? = null
  ) : super(
    CommonDataKeys.PROJECT.getData(dataContext), aParent, step, parentValue
  ) {
    myDisposeCallback = disposeCallback
    myComponent = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext)
    myActionPlace = actionPlace ?: ActionPlaces.UNKNOWN
    initSomeStuff()
    setMaxRowCount(BMActionsSchema.getInstance().state.maxRowCount)
  }

  private fun initSomeStuff() {
    registerAction("handleActionToggle1", KeyEvent.VK_SPACE, 0, object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        handleToggleAction()
      }
    })

    addListSelectionListener { e: ListSelectionEvent ->
      val list = e.source as JList<*>
      val actionItem = list.selectedValue as BMActionItem? ?: return@addListSelectionListener
      val presentation: Presentation = updateActionItem(actionItem)
      ActionMenu.showDescriptionInStatusBar(true, myComponent, presentation.description)
    }
  }

  constructor(
    title: String?,
    bmNode: BMNode,
    dataContext: DataContext,
    showDisabledActions: Boolean,
    disposeCallback: Runnable?,
    maxRowCount: Int,
    preselectActionCondition: Condition<in AnAction?>?,
    actionPlace: String?,
    autoSelection: Boolean
  ) : this(
    null, createStep(
      title,
      bmNode,
      dataContext,
      showDisabledActions,
      preselectActionCondition,
      actionPlace,
      autoSelection
    ), disposeCallback, dataContext, actionPlace, maxRowCount
  )

  constructor(
    bmNode: BMNode, dataContext: DataContext
  ) : this(
    bmNode.customText, bmNode, dataContext, true, null, 10, null, null, false
  )

  override fun process(aEvent: KeyEvent?) {
    if (aEvent == null || aEvent.keyCode in PASSTHROUGH_KEYS && aEvent.modifiersEx == 0) {
      return super.process(aEvent)
    }
    if (aEvent.keyCode == KeyEvent.VK_F12 && aEvent.modifiersEx == 0) {
      val selectedValue = list.selectedValue
      if (selectedValue is BMActionItem) {
        ApplicationManager.getApplication().invokeLater {
          EditNodeAction.editNode(selectedValue.bmNode)
          BMActionsSchema.getInstance().save()
        }
        aEvent.consume()
        aEvent.source = list
        closeOk(aEvent)
      }
    }

    val eventKeyStroke = KeyStroke.getKeyStrokeForEvent(aEvent)
    val bmStep = step as BMActionPopupStep
    for (actionItem in bmStep.values) {
      if (eventKeyStroke == actionItem.keyStroke) {
        list.setSelectedValue(actionItem, true)
        list.repaint()
        aEvent.consume()
        aEvent.source = list
        handleSelect(true, aEvent)
        break
      }
    }
    aEvent.consume()
    aEvent.source = list
    // HACK: this sets the event source and somehow prevents the event from reaching the ListUI
    //   and causing problems there...
//    super.process(aEvent)
  }

  private fun updateActionItem(actionItem: BMActionItem): Presentation {
    val action: AnAction = actionItem.action
    val presentation = Presentation()
    presentation.description = action.templatePresentation.description

    val actionEvent = AnActionEvent(
      null,
      DataManager.getInstance().getDataContext(myComponent),
      myActionPlace,
      presentation,
      ActionManager.getInstance(),
      0
    )
    actionEvent.setInjectedContext(action.isInInjectedContext)
    ActionUtil.performDumbAwareUpdate(action, actionEvent, false)
    return presentation
  }

  override fun handleSelect(handleFinalChoices: Boolean, e: InputEvent?) {
    val selectedValue = list.selectedValue
    val actionPopupStep = ObjectUtils.tryCast(listStep, BMActionPopupStep::class.java)
    if (actionPopupStep != null) {
      if (isKeepOpenAction(selectedValue, actionPopupStep)) {
        actionPopupStep.performAction(((selectedValue as BMActionItem).action as AnAction?)!!, e)
        for (item in actionPopupStep.values) {
          updateActionItem(item)
        }
        list.repaint()
        return
      }
    }
    super.handleSelect(handleFinalChoices, e)
  }

  private fun handleToggleAction() {
    val selectedValues = list.selectedValuesList

    val listStep = listStep
    val actionPopupStep = ObjectUtils.tryCast(listStep, BMActionPopupStep::class.java) ?: return

    val filtered = ContainerUtil.mapNotNull(selectedValues) { o: Any? ->
      getActionByClass(
        o, actionPopupStep, ToggleAction::class.java
      )
    }

    for (action in filtered) {
      actionPopupStep.performAction(action)
    }

    for (item in actionPopupStep.values) {
      updateActionItem(item)
    }

    list.repaint()
  }

  override fun dispose() {
    if (myDisposeCallback != null) {
      myDisposeCallback!!.run()
    }
    ActionMenu.showDescriptionInStatusBar(true, myComponent, null)
    super.dispose()
  }

  override fun createPopup(
    parent: WizardPopup?, step: PopupStep<*>, parentValue: Any?
  ): WizardPopup {
    if (parent is BMActionGroupPopup) {
      return BMActionGroupPopup(project, parent, step as ListPopupStep<*>, parentValue)
    }
    return super.createPopup(parent, step, parentValue)
  }

  override fun getListElementRenderer(): ListCellRenderer<*> {
    return BMPopupListElementRenderer<BMActionItem>(this)
  }

  companion object {

    private val LOG = Logger.getInstance("#io.github.markusmo3.bm.BMActionGroupPopup")
    private val PASSTHROUGH_KEYS = arrayOf(
      KeyEvent.VK_LEFT,
      KeyEvent.VK_RIGHT,
      KeyEvent.VK_UP,
      KeyEvent.VK_DOWN,
      KeyEvent.VK_ENTER,
      KeyEvent.VK_ESCAPE
    )

    private fun getComponentContextSupplier(component: Component?): Supplier<DataContext> {
      return Supplier { DataManager.getInstance().getDataContext(component) }
    }

    private fun createStep(
      title: String?,
      bmNode: BMNode,
      dataContext: DataContext,
      showDisabledActions: Boolean,
      preselectActionCondition: Condition<in AnAction?>?,
      actionPlace: String?,
      autoSelection: Boolean
    ): ListPopupStep<*> {
      val component = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext)
      LOG.assertTrue(component != null, "dataContext has no component for new ListPopupStep")
      val items = getActionItems(bmNode, dataContext, showDisabledActions, actionPlace)
      return BMActionPopupStep(
        items,
        title,
        getComponentContextSupplier(component),
        actionPlace,
        preselectActionCondition,
        autoSelection,
        showDisabledActions
      )
    }

    private fun getActionItems(
      bmNode: BMNode, dataContext: DataContext, showDisabledActions: Boolean, actionPlace: String?
    ): List<BMActionItem> {
      val builder = BMActionStepBuilder(dataContext, showDisabledActions)
      if (actionPlace != null) {
        builder.setActionPlace(actionPlace)
      }
      builder.buildGroup(bmNode)
      return builder.items
    }

    private fun <T> getActionByClass(
      value: Any?, actionPopupStep: BMActionPopupStep, actionClass: Class<T>
    ): T? {
      val item = (if (value is BMActionItem) value else null) ?: return null
      if (!actionPopupStep.isSelectable(item)) return null
      return if (actionClass.isInstance(item.action)) actionClass.cast(item.action) else null
    }

    private fun isKeepOpenAction(
      value: Any?, actionPopupStep: BMActionPopupStep
    ): Boolean {
      val item = (if (value is BMActionItem) value else null) ?: return false
      return item.isEnabled && item.action.templatePresentation.keepPopupOnPerform == KeepPopupOnPerform.Always

    }
  }
}
