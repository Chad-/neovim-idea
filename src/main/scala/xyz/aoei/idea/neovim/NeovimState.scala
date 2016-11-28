package xyz.aoei.idea.neovim

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import xyz.aoei.neovim.{Buffer, Window, Neovim => Nvim}

trait CursorMoveListener { def cursorMoved(state: NeovimState): Unit }
object CursorMoveListener {
  val CURSOR_MOVED = Topic.create("Cursor move event", classOf[CursorMoveListener])
}

trait WindowsChangeListener {
  def windowsChanged(state: NeovimState): Unit
  def selectedChanged(state: NeovimState): Unit
}
object WindowsChangeListener {
  val WINDOWS_CHANGED = Topic.create("Window change event", classOf[WindowsChangeListener])
  val SELECTED_CHANGED = Topic.create("Selected window changed", classOf[WindowsChangeListener])
}

trait BuffersChangeListener {
  def buffersChanged(state: NeovimState): Unit
}
object BuffersChangeListener {
  val BUFFERS_CHANGED = Topic.create("Buffers change event", classOf[BuffersChangeListener])
}

class ChangeWatcher[T](@volatile var curVal: T, message: (NeovimState) => Unit, state: NeovimState) {
  def get = curVal
  def set(newVal: T, alwaysUpdate: Boolean = false) = {
    val oldVal = curVal
    curVal = newVal

    if (alwaysUpdate || oldVal != newVal) message(state)
  }
}

class NeovimState(val nvim: Nvim, val project: Project) {
  private val messageBus = project.getMessageBus

  @volatile var windowToEditor: Map[Window, EditorWindow] = Map()

  @volatile var windowPositions: List[(Int, Int)] = Nil

  private val _windows = new ChangeWatcher[List[Window]](Nil,
    messageBus.syncPublisher(WindowsChangeListener.WINDOWS_CHANGED).windowsChanged, this)
  def windows = _windows.get
  def windows_=(n: List[Window]) = _windows.set(n)

  private val _selectedWindow = new ChangeWatcher[Window](null,
    messageBus.syncPublisher(WindowsChangeListener.SELECTED_CHANGED).selectedChanged, this)
  def selectedWindow = _selectedWindow.get
  def selectedWindow_=(n: Window) = _selectedWindow.set(n)

  private val _windowBuffers = new ChangeWatcher[List[Buffer]](Nil,
    messageBus.syncPublisher(BuffersChangeListener.BUFFERS_CHANGED).buffersChanged, this)
  def windowBuffers = _windowBuffers.get
  def windowBuffers_=(n: List[Buffer]) = _windowBuffers.set(n, alwaysUpdate = true)

  private val _cursors = new ChangeWatcher[List[(Int, Int)]](Nil,
    messageBus.syncPublisher(CursorMoveListener.CURSOR_MOVED).cursorMoved, this)
  def cursors = _cursors.get
  def cursors_=(n: List[(Int, Int)]) = _cursors.set(n)
}
