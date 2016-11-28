package xyz.aoei.idea.neovim

import java.net.Socket

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import xyz.aoei.idea.neovim.listener._
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
      val shouldUpdateText = args.exists(x => x.asInstanceOf[List[_]].head == "put")
      val shouldUpdateCursor = args.exists(x => x.asInstanceOf[List[_]].head == "cursor_goto")

      if (shouldUpdateText || shouldUpdateCursor) {
        updateText()
//        updateCursor()
        updateState()
      }
    }
  })

  override def getComponentName: String = "Neovim"

  override def projectOpened(): Unit = {}
  override def projectClosed(): Unit = {}

  override def initComponent(): Unit = {
    EditorFactory.getInstance().addEditorFactoryListener(new NeovimEditorFactoryListener, new Disposable(){
      override def dispose(): Unit = {}
    })

    val messageBus = ApplicationManager.getApplication.getMessageBus.connect()
    messageBus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileListener)
    messageBus.subscribe(InputEventListener.INPUT_EVENT, new NeovimInputEventListener(nvim))

    messageBus.subscribe(CursorMoveListener.CURSOR_MOVED, new NeovimCursorMoveListener())
    messageBus.subscribe(WindowsChangeListener.WINDOWS_CHANGED, new NeovimWindowsChangeListener())

    println("Init")
    nvim.uiAttach(100, 30, Map())
  }

  override def disposeComponent(): Unit = {
//    nvim.quit()
  }

  private def updateText(): Unit = {
    for {
      buffer <- nvim.getCurrentBuf
      lines <- buffer.getLines(0, -1, false)
    } yield {
      val text = lines.mkString("\n")

      val editor = fileListener.selectedTextEditor

      val project = editor.getProject
      val document = editor.getDocument

      WriteCommandAction.runWriteCommandAction(project, new Runnable {
        override def run(): Unit = {
          document.replaceString(0, document.getTextLength, text)
//          document.setText(text)
        }
      })
    }
  }

  private def updateState(): Unit = {
    for {
      windows <- nvim.listWins
      cursors <- Future.sequence(windows.map(w => w.getCursor))
      windowPositions <- Future.sequence(windows.map(w => w.getPosition))
    } yield {
      state.windows = windows

      // Row numbers should start at 0 not 1
      state.cursors = cursors.map(x => (x.head-1, x(1)))

      state.windowPositions = windowPositions.map(x => (x.head-1, x(1)))
    }
  }

}
