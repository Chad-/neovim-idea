package xyz.aoei.idea.neovim

import java.net.Socket

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.editor.{LogicalPosition, ScrollType}
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import xyz.aoei.neovim.{Neovim => Nvim}

import scala.concurrent.ExecutionContext.Implicits.global

class Neovim extends ApplicationComponent {
  val nvim = {
    val socket = new Socket("127.0.0.1", 8888)
    new Nvim(socket.getInputStream, socket.getOutputStream)
  }

  val fileListener = new FileListener(nvim)

  nvim.onNotification((method, args) => method match {
    case "redraw" => {
      val shouldUpdateText = args.exists(x => x.asInstanceOf[List[_]].head == "put")
      val shouldUpdateCursor = args.exists(x => x.asInstanceOf[List[_]].head == "cursor_goto")

      if (shouldUpdateCursor) updateCursor()
    }
  })

  override def getComponentName: String = "Neovim"

  override def initComponent(): Unit = {
    EditorActionManager.getInstance().getTypedAction.setupHandler(new NeovimTypedActionHandler)

    val messageBus = ApplicationManager.getApplication.getMessageBus.connect()
    messageBus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileListener)

    println("Init")
    nvim.uiAttach(100, 30, Map())
  }

  override def disposeComponent(): Unit = {
//    nvim.quit()
  }

  private def updateCursor(): Unit = {
    for {
      window <- nvim.getCurrentWin
      cursor <- window.getCursor
    } yield {
      // Row numbers should start at 0 not 1
      val pos = (cursor.head - 1, cursor(1))

      ApplicationManager.getApplication.invokeLater(new Runnable {
        override def run(): Unit = {
          val editor = fileListener.selectedTextEditor

          // Move the cursor to pos
          editor.getCaretModel.moveToLogicalPosition(new LogicalPosition(pos._1, pos._2))
          editor.getScrollingModel.scrollToCaret(ScrollType.RELATIVE)
        }
      })
    }
  }
}
