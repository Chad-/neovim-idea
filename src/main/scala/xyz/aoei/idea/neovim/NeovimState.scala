package xyz.aoei.idea.neovim

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import xyz.aoei.neovim.{Window, Neovim => Nvim}

trait CursorMoveListener { def cursorMoved(state: NeovimState): Unit }
object CursorMoveListener {
  val CURSOR_MOVED = Topic.create("Cursor move event", classOf[CursorMoveListener])
}

trait WindowsChangeListener { def windowsChanged(state: NeovimState): Unit }
object WindowsChangeListener {
  val WINDOWS_CHANGED = Topic.create("Window change event", classOf[WindowsChangeListener])
}

class NeovimState(val nvim: Nvim, val project: Project) {
  private val messageBus = ApplicationManager.getApplication.getMessageBus

  @volatile var windowToEditor: Map[Window, EditorWindow] = Map()

  @volatile var windowPositions: List[(Int, Int)] = Nil

  @volatile var _windows: List[Window] = Nil
  def windows = _windows
  def windows_=(newWindows: List[Window]): Unit = {
    val oldWindows = _windows
    _windows = newWindows

    if (oldWindows != newWindows)
      messageBus.syncPublisher(WindowsChangeListener.WINDOWS_CHANGED).windowsChanged(this)
  }

  @volatile private var _cursors: List[(Int, Int)] = Nil
  def cursors = _cursors
  def cursors_=(newCursors: List[(Int, Int)]): Unit = {
    val oldCursors = _cursors
    _cursors = newCursors

    if (oldCursors != newCursors)
      messageBus.syncPublisher(CursorMoveListener.CURSOR_MOVED).cursorMoved(this)
  }
}
