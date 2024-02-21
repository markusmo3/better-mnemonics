package io.github.markusmo3.bm.popup

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.util.Condition
import com.intellij.util.ObjectUtils
import com.intellij.util.ui.StatusText
import io.github.markusmo3.bm.config.BMNode
import java.awt.event.InputEvent
import java.util.function.Supplier
import javax.swing.Icon

open class BMActionPopupStep(
  private val myItems: List<BMActionItem>,
  private val myTitle: String?,
  private val myContext: Supplier<out DataContext>,
  actionPlace: String?,
  preselectActionCondition: Condition<in AnAction?>?,
  autoSelection: Boolean,
  showDisabledActions: Boolean
) : ListPopupStepEx<BMActionItem> {

  private val myActionPlace: String = ObjectUtils.notNull(actionPlace, ActionPlaces.UNKNOWN)
  private val myDefaultOptionIndex: Int
  private val myAutoSelectionEnabled: Boolean
  private val myShowDisabledActions: Boolean
  private var myFinalRunnable: Runnable? = null
  private val myPreselectActionCondition: Condition<in AnAction?>?

  init {
    myDefaultOptionIndex = getDefaultOptionIndexFromSelectCondition(
      preselectActionCondition, myItems
    )
    myPreselectActionCondition = preselectActionCondition
    myAutoSelectionEnabled = autoSelection
    myShowDisabledActions = showDisabledActions
  }

  override fun getValues(): List<BMActionItem> {
    return myItems
  }

  override fun isSelectable(value: BMActionItem): Boolean {
    return value.isEnabled
  }

  override fun getIconFor(aValue: BMActionItem): Icon? {
    return aValue.getIcon(false)
  }

  override fun getSelectedIconFor(value: BMActionItem): Icon? {
    return value.getIcon(true)
  }

  override fun getTextFor(value: BMActionItem): String {
    return value.text
  }

  override fun getTooltipTextFor(value: BMActionItem): String? {
    return value.description
  }

  override fun setEmptyText(emptyText: StatusText) {}

  override fun getSeparatorAbove(value: BMActionItem): ListSeparator? {
    return if (value.isPrependWithSeparator) ListSeparator(value.separatorText) else null
  }

  override fun getDefaultOptionIndex(): Int {
    return myDefaultOptionIndex
  }

  override fun getTitle(): String? {
    return myTitle
  }

  override fun onChosen(actionChoice: BMActionItem, finalChoice: Boolean): PopupStep<*>? {
    return onChosen(actionChoice, finalChoice, null)
  }

  override fun onChosen(
    actionChoice: BMActionItem,
    finalChoice: Boolean,
    inputEvent: InputEvent?
  ): PopupStep<*>? {
    if (!actionChoice.isEnabled) return PopupStep.FINAL_CHOICE
    val action = actionChoice.action
    val dataContext = myContext.get()
    return if (action is ActionGroup) {
      createActionsStep(
        actionChoice.bmNode,
        dataContext,
        myShowDisabledActions,
        null,
        false,
        myContext,
        myActionPlace,
        myPreselectActionCondition,
        -1
      )
    } else {
      myFinalRunnable = Runnable { performAction(action, inputEvent) }
      PopupStep.FINAL_CHOICE
    }
  }

  @JvmOverloads
  fun performAction(action: AnAction, inputEvent: InputEvent? = null) {
    // HACK: need to invoke the action later because otherwise the inputEvent gets passed to the
    //  next action and causes interference
    inputEvent?.consume()
    ApplicationManager.getApplication().invokeLater({
      val dataContext = myContext.get()
      val event =
        AnActionEvent.createFromInputEvent(inputEvent, myActionPlace, action.templatePresentation.clone(), dataContext)
      event.setInjectedContext(action.isInInjectedContext)
      if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
        ActionUtil.performActionDumbAwareWithCallbacks(action, event)
      }
    }, ModalityState.defaultModalityState())
  }

  override fun getFinalRunnable(): Runnable? {
    return myFinalRunnable
  }

  override fun hasSubstep(selectedValue: BMActionItem?): Boolean {
    return selectedValue != null && selectedValue.isEnabled && selectedValue.action is ActionGroup
  }

  override fun canceled() {}

  override fun isMnemonicsNavigationEnabled(): Boolean {
    return false
  }

  override fun getMnemonicNavigationFilter(): MnemonicNavigationFilter<BMActionItem>? {
    return null
  }

  override fun isSpeedSearchEnabled(): Boolean {
    return false
  }

  override fun getSpeedSearchFilter(): SpeedSearchFilter<BMActionItem>? {
    return null
  }

  override fun isAutoSelectionEnabled(): Boolean {
    return myAutoSelectionEnabled
  }

  companion object {
    private fun getDefaultOptionIndexFromSelectCondition(
      preselectActionCondition: Condition<in AnAction>?, items: List<BMActionItem>
    ): Int {
      var defaultOptionIndex = 0
      if (preselectActionCondition != null) {
        for (i in items.indices) {
          val action = items[i].action
          if (preselectActionCondition.value(action)) {
            defaultOptionIndex = i
            break
          }
        }
      }
      return defaultOptionIndex
    }

    fun createActionsStep(
      bmNode: BMNode,
      dataContext: DataContext,
      showDisabledActions: Boolean,
      title: String?,
      autoSelectionEnabled: Boolean,
      contextSupplier: Supplier<out DataContext?>?,
      actionPlace: String?,
      preselectCondition: Condition<in AnAction?>?,
      defaultOptionIndex: Int
    ): ListPopupStep<*> {
      val builder = BMActionStepBuilder(dataContext, showDisabledActions)
      builder.buildGroup(bmNode)
      val items = builder.items
      return BMActionPopupStep(
        items,
        title,
        contextSupplier!!,
        actionPlace,
        preselectCondition ?: Condition { action: AnAction? ->
          defaultOptionIndex >= 0 && defaultOptionIndex < items.size && items[defaultOptionIndex].action == action
        },
        autoSelectionEnabled,
        showDisabledActions
      )
    }
  }
}
