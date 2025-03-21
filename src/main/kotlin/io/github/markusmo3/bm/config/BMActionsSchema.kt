package io.github.markusmo3.bm.config

import com.intellij.configurationStore.SerializableScheme
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.impl.stores.stateStore
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Scheme
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import io.github.markusmo3.bm.BMManager
import org.jdom.Element
import org.jdom.input.DOMBuilder
import org.w3c.dom.Document
import javax.swing.KeyStroke
import javax.xml.parsers.DocumentBuilderFactory

@State(
  name = "io.github.markusmo3.bm.config.BMActionsSchema",
  storages = [Storage("betterMnemonicsSchema.xml", exportable = true)]
)
class BMActionsSchema : PersistentStateComponent<BMActionsSchemaState> {

  private val keyboardChars: CharArray = "1234567890QWERTZUIOPASDFGHJKLYXCVBNM".toCharArray()
  private val bmManager: BMManager by lazy { service<BMManager>() }
  private var state: BMActionsSchemaState = BMActionsSchemaState()

  override fun getState(): BMActionsSchemaState {
    return state
  }

  override fun loadState(state: BMActionsSchemaState) {
    validateState(state)
    this.state = state
    bmManager.reset(state)
  }

  private fun validateState(state: BMActionsSchemaState) {
    for (rootItem in state.root) {
//            assert(rootItem.isGroup())
      validateNode(rootItem)
    }
  }

  private fun validateNode(bmNode: BMNode) {
    if (bmNode.customText != null && bmNode.customText!!.isBlank()) {
      bmNode.customText = null
    }
    if (bmNode.isGroup()) {
      for (child in bmNode) {
        validateNode(child)
      }
    } else if (bmNode.isSeparator()) {
      if (bmNode.keyStroke != null) {
        bmNode.keyStroke = null
      }
      if (bmNode.globalKeyStroke != null) {
        bmNode.globalKeyStroke = null
      }
    } else if (bmNode.isAction()) {
      if (bmNode.globalKeyStroke != null) {
        bmNode.globalKeyStroke = null
      }
    }
  }

  override fun noStateLoaded() {
    if (state.root.isEmpty()) {
      val inputStream = javaClass.getResourceAsStream("/defaultBMActionsSchema.xml")
      val factory = DocumentBuilderFactory.newInstance()
      val documentBuilder = factory.newDocumentBuilder()
      val w3cDocument: Document = documentBuilder.parse(inputStream)
      val document = DOMBuilder().build(w3cDocument)
      val rootBmNode = document.rootElement.getChild("component").getChild("BMNode")
      state.root = XmlSerializer.deserialize(rootBmNode, BMNode::class.java)
      bmManager.reset(state)
//      val mainMenuAction =
//        CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_MENU)
//      if (mainMenuAction != null) {
//        val wrap = mainMenuAction.toBMNode() ?: return
//        state.root.add(0, wrap)
//      }
    }
  }

  fun save() {
    ApplicationManager.getApplication().stateStore.saveComponent(this)
  }

//  fun importFromVimPlugin() {
//    val keyRoot = VimPlugin.getKey().getKeyRoot(MappingMode.NORMAL)
//    state.root.add(keyRoot.toBMNode(null, null))
//  }
//
//  private fun IdeaVimNode.toBMNode(keyStroke: KeyStroke?, parent: BMNode?): BMNode {
//    if (this is CommandPartNode) {
//      val newGroup = if (this is RootNode) {
//        BMNode.newGroup(KeyStroke.getKeyStroke("Alt M"), "Imported from Vim Plugin")
//      } else {
//        BMNode.newGroup(keyStroke, "Menu for $keyStroke")
//      }
//      for ((k, v) in entries) {
//        if (v is CommandPartNode) {
//          newGroup.add(v.toBMNode(k, newGroup))
//        } else if (v is CommandNode) {
//          newGroup.add(BMNode(v.actionHolder.actionId, k, v.actionHolder.implementation))
//        }
//      }
//      return newGroup
//    } else {
//      throw IllegalArgumentException("this isnt compatible")
//    }
//  }

//  private fun AnAction.toBMNode(parent: BMNode? = null, index: IntArray = intArrayOf(0)): BMNode? {
//    val action = this
//    if (action is Separator) {
//      index[0]--
//      return BMNode.newSeparator(action.text)
//    } else if (action is ActionGroup) {
//      val children = ActionWrapperUtil.getChildren(null, action, action)
//      if (!action.isPopup && parent != null) {
//        for (child in children) {
//          val childNode = child.toBMNode(parent, index) ?: continue
//          parent.add(childNode)
//          index[0]++
//        }
//        return null
//      } else {
//        val node = BMNode.newGroup(
//          KeyStroke.getKeyStroke(keyboardChars[index[0] % keyboardChars.size].toString()),
//          action.templateText ?: "group without a name"
//        )
//        val i = intArrayOf(0)
//        for (child in children) {
//          val childNode = child.toBMNode(node, i) ?: continue
//          node.add(childNode)
//          i[0]++
//        }
//        return node
//      }
//    } else {
//      val actionId = ActionManager.getInstance().getId(action) ?: return null
//      val keyStroke: KeyStroke? = action.shortcutSet.shortcuts.firstOrNull()?.let {
//        if (it.isKeyboard && it is KeyboardShortcut) {
//          it.firstKeyStroke
//        } else {
//          null
//        }
//      }
//      if (keyStroke != null) {
//        index[0]--
//        return BMNode.newAction(
//          actionId, keyStroke
//        )
//      } else {
//        return BMNode.newAction(
//          actionId, KeyStroke.getKeyStroke(keyboardChars[index[0] % keyboardChars.size].toString())
//        )
//      }
//    }
//  }

  companion object {
    @JvmStatic
    fun getInstance(): BMActionsSchema = ApplicationManager.getApplication().getService(BMActionsSchema::class.java)
  }
}

class BMActionsSchemaState : SerializableScheme, Scheme {
  @Property(surroundWithTag = false)
  var root: BMNode = BMNode.newRoot()

  @get:Attribute
  var maxRowCount = 50

  @get:Attribute
  @Deprecated("Not included in this plugin anymore")
  var useBetterIcon = false

  fun deepCopy(): BMActionsSchemaState {
    val that = BMActionsSchemaState()
    that.root = this.root.deepCopy()
    that.maxRowCount = this.maxRowCount
    return that
  }

  fun getXmlString(): String {
    val serialized = XmlSerializer.serialize(this)
    return JDOMUtil.write(serialized)
  }

  override fun writeScheme(): Element {
    return XmlSerializer.serialize(this)
  }

  override fun getName(): String {
    return "BMActionsSchemaState"
  }
}

enum class BMNodeType {
  ROOT, GROUP, ACTION, SEPARATOR, UNDEFINED
}

data class BMNode internal constructor(
  @Attribute var type: BMNodeType,
  @Attribute var actionId: String? = null,
  @Attribute(converter = KeyStrokeConverter::class) var keyStroke: KeyStroke? = null,
  @Attribute var customText: String? = null,
  @Attribute(converter = KeyStrokeConverter::class) var globalKeyStroke: KeyStroke? = null,
  @Property(surroundWithTag = false) @XCollection val children: MutableList<BMNode> = ArrayList()
) : MutableList<BMNode> by children {

  val action: AnAction? by lazy {
    when (type) {
      BMNodeType.GROUP -> {
        val group = DefaultActionGroup(customText, true)
        group.addAll(children.mapNotNull { it.action })
        group
      }
      BMNodeType.SEPARATOR -> {
        Separator(customText)
      }
      BMNodeType.ACTION -> {
        ActionManager.getInstance().getAction(actionId!!)
      }
      else -> {
        null
      }
    }
  }

  val actionIdForKeymap: String
    get() = bmActionIdForKeymapPrefix + actionId + "_" + customText

  constructor() : this(BMNodeType.UNDEFINED)

  fun isAction(): Boolean {
    return type == BMNodeType.ACTION
  }

  fun isSeparator(): Boolean {
    return type == BMNodeType.SEPARATOR
  }

  fun isGroup(): Boolean {
    return type == BMNodeType.GROUP
  }

  fun isRoot(): Boolean {
    return type == BMNodeType.ROOT
  }

  fun deepCopy(): BMNode {
    val serialized = XmlSerializer.serialize(this)
    val asString = JDOMUtil.write(serialized)
    val asDomAgain = JDOMUtil.load(asString)
    return XmlSerializer.deserialize(asDomAgain, BMNode::class.java)
  }

  companion object {
    const val bmActionIdForKeymapPrefix = "InvokeBMPopupAction_"

    @JvmStatic
    fun newRoot(): BMNode {
      return BMNode(BMNodeType.ROOT)
    }

    @JvmStatic
    fun newUndefined(): BMNode {
      return BMNode(BMNodeType.UNDEFINED)
    }

    @JvmStatic
    fun newAction(actionId: String?, keyStroke: KeyStroke?, customText: String? = null): BMNode {
      return BMNode(
        BMNodeType.ACTION, actionId = actionId, keyStroke = keyStroke, customText = customText
      )
    }

    @JvmStatic
    fun newSeparator(customText: String?): BMNode {
      return BMNode(
        BMNodeType.SEPARATOR, customText = customText
      )
    }

    @JvmStatic
    fun newGroup(keyStroke: KeyStroke?, customText: String?): BMNode {
      return BMNode(
        BMNodeType.GROUP, keyStroke = keyStroke, customText = customText
      )
    }
  }
}
