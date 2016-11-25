package xyz.aoei.idea.neovim

import java.net.Socket

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.editor.{EditorFactory, LogicalPosition, ScrollType}
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import xyz.aoei.idea.neovim.Listener.{NeovimEditorFactoryListener, NeovimFileEditorManagerListener, NeovimInputEventListener}
import xyz.aoei.neovim.{Neovim => Nvim}

import scala.concurrent.ExecutionContext.Implicits.global

class Neovim extends ApplicationComponent {
  val nvim = {
    val socket = new Socket("127.0.0.1", 8888)
    new Nvim(socket.getInputStream, socket.getOutputStream)
  }

  val fileListener = new NeovimFileEditorManagerListener(nvim)

  nvim.onNotification((method, args) => method match {
    case "redraw" => {
      val shouldUpdateText = args.exists(x => x.asInstanceOf[List[_]].head == "put")
      val shouldUpdateCursor = args.exists(x => x.asInstanceOf[List[_]].head == "cursor_goto")

      if (shouldUpdateText || shouldUpdateCursor) {
        updateText()
        updateCursor()
      }
    }
  })

  override def getComponentName: String = "Neovim"

  override def initComponent(): Unit = {
    EditorFactory.getInstance().addEditorFactoryListener(new NeovimEditorFactoryListener, new Disposable(){
      override def dispose(): Unit = {}
    })

    val messageBus = ApplicationManager.getApplication.getMessageBus.connect()
    messageBus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileListener)
    messageBus.subscribe(InputEventListener.INPUT_EVENT, new NeovimInputEventListener(nvim))

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
          updateCursor()
        }
      })
    }
  }

  private def updateCursor(): Unit = {
    for {
      window <- nvim.getCurrentWin
      cursor <- window.getCursor
    } yield {
      // Row numbers should start at 0 not 1
      val pos = (cursor.head - 1, cursor(1))

      val editor = fileListener.selectedTextEditor

      ApplicationManager.getApplication.invokeLater(new Runnable {
        override def run(): Unit = {
          // Move the cursor to pos
          editor.getCaretModel.moveToLogicalPosition(new LogicalPosition(pos._1, pos._2))
          editor.getScrollingModel.scrollToCaret(ScrollType.RELATIVE)
        }
      })
    }
  }
}
