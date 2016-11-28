package xyz.aoei.idea.neovim

import java.awt.event.{InputEvent, KeyEvent}
import javax.swing.KeyStroke

import com.intellij.openapi.actionSystem._
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.util.messages.Topic
import xyz.aoei.idea.neovim.util.KeyCodes

// Create an event to be used on the message bus
object InputEventListener {
  val INPUT_EVENT = Topic.create("Input event", classOf[InputEventListener])
}
trait InputEventListener {
  def inputEvent(e: InputEvent): Unit
}

object ShortcutKeyAction {
  // Create an instance of the action that is wrapped to allow shortcut registrations
  val instance: AnAction = {
    val action = ActionManager.getInstance().getAction("ShortcutKeyAction")
    EmptyAction.wrap(action)
  }

  // Generate a list of all possible shortcut combinations
  private val shortcuts: List[KeyboardShortcut] = {
    val modCombinations = KeyCodes.modifiers.subsets
      .map(_.foldLeft(0)((acc, x) => acc | x)).toList

    val keystrokes = for (k <- KeyCodes.keys; m <- modCombinations)
      yield KeyStroke.getKeyStroke(k, m)

    keystrokes.map(new KeyboardShortcut(_, null)).toList
  }

  // register the shortcuts with the editor
  def registerEditor(e: Editor): Unit = {
    instance.registerCustomShortcutSet(new CustomShortcutSet(shortcuts:_*), e.getComponent)
  }
}

class ShortcutKeyAction extends AnAction {
  val messageBus = ApplicationManager.getApplication.getMessageBus

  override def actionPerformed(e: AnActionEvent): Unit = {
    messageBus.syncPublisher(InputEventListener.INPUT_EVENT).inputEvent(e.getInputEvent)
  }
}
