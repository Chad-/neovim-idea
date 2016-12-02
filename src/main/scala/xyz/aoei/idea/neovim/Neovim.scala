package xyz.aoei.idea.neovim

import java.awt.BorderLayout
import java.net.Socket
import javax.swing.{JList, JPanel}

import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerListener}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.LightweightHint
import xyz.aoei.idea.neovim.listener._
import xyz.aoei.idea.neovim.ui.Popupmenu
import xyz.aoei.idea.neovim.util.Util
import xyz.aoei.neovim.{Neovim => Nvim}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Neovim(project: Project) extends ProjectComponent {
  val nvim = {
    val socket = new Socket("127.0.0.1", 8888)
    new Nvim(socket.getInputStream, socket.getOutputStream)
  }

  val state = new NeovimState(nvim, project)

  val fileListener = new NeovimFileEditorManagerListener(nvim)

  nvim.onNotification((method, args) => method match {
    case "redraw" => {
      val updates = args.map(x => x.asInstanceOf[List[_]]).map(x => (x.head, x(1)))

      var shouldUpdateState = false

      updates.foreach {
        case ("put", _) => shouldUpdateState = true
        case ("cursor_goto", _) => shouldUpdateState = true
        case ("popupmenu_show", List(items: List[Any], selected: Int, row: Int, col: Int)) =>
          popupmenuShow(items, selected, row, col)
        case ("popupmenu_select", List(selected: Int)) =>
          popupmenuSelect(selected)
        case ("popupmenu_hide", _) =>
          popupmenuHide()
        case _ => // not handled
      }

      if (shouldUpdateState) {
        updateState()
      }
    }
    case x => println("Notification: " + x)
  })

  override def getComponentName: String = "Neovim"

  override def projectOpened(): Unit = {}
  override def projectClosed(): Unit = {}

  override def initComponent(): Unit = {
    EditorFactory.getInstance().addEditorFactoryListener(new NeovimEditorFactoryListener, new Disposable(){
      override def dispose(): Unit = {}
    })

    val messageBus = project.getMessageBus.connect()
    messageBus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileListener)
    messageBus.subscribe(InputEventListener.INPUT_EVENT, new NeovimInputEventListener(nvim))

    messageBus.subscribe(CursorMoveListener.CURSOR_MOVED, new NeovimCursorMoveListener())
    messageBus.subscribe(BuffersChangeListener.BUFFERS_CHANGED, new NeovimBuffersChangeListener())

    val windowChangeListener = new NeovimWindowsChangeListener()
    messageBus.subscribe(WindowsChangeListener.WINDOWS_CHANGED, windowChangeListener)
    messageBus.subscribe(WindowsChangeListener.SELECTED_CHANGED, windowChangeListener)

    nvim.uiAttach(100, 30, Map("popupmenu_external" -> true))
    nvim.setCurrentDir(project.getBaseDir.getPath)
  }

  override def disposeComponent(): Unit = {
    nvim.uiDetach()
//    nvim.quit()
  }

  private def updateState(): Unit = {
    for {
      windows <- nvim.listWins
      selectedWindow <- nvim.getCurrentWin
      cursors <- Future.sequence(windows.map(w => w.getCursor))
      windowBuffers <- Future.sequence(windows.map(w => w.getBuf))
      windowPositions <- Future.sequence(windows.map(w => w.getPosition))
    } yield {
      state.windows = windows
      state.selectedWindow = selectedWindow
      state.windowPositions = windowPositions.map(x => (x.head-1, x(1)))
      state.windowBuffers = windowBuffers

      // Row numbers should start at 0 not 1
      state.cursors = cursors.map(x => (x.head-1, x(1)))
    }
  }

  val popupmenu = new Popupmenu
  private def popupmenuShow(items: List[Any], selected: Int, row: Int, col: Int): Unit = {
    val data = items.asInstanceOf[List[List[String]]].map(_.mkString("\t"))
    popupmenu.setItems(data)
    popupmenu.setSelected(selected)

    Util.invokeLater(() => {
      popupmenu.show()
    })
  }

  private def popupmenuSelect(selected: Int): Unit = {
    popupmenu.setSelected(selected)
  }

  private def popupmenuHide(): Unit = {
    Util.invokeLater(() => {
      popupmenu.hide()
    })
  }
}
