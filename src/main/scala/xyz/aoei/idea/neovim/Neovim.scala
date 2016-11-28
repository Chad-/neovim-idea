package xyz.aoei.idea.neovim

import java.awt.BorderLayout
import java.net.Socket
import javax.swing.JSplitPane

import com.intellij.lang.java.FileDocumentationProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.editor.{Caret, EditorFactory, LogicalPosition, ScrollType}
import com.intellij.openapi.fileEditor.impl.{EditorWindow, FileEditorManagerImpl}
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager, FileEditorManagerListener}
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.OnePixelSplitter
import xyz.aoei.idea.neovim.Listener.{NeovimEditorFactoryListener, NeovimFileEditorManagerListener, NeovimInputEventListener}
import xyz.aoei.idea.neovim.Util.SplitUtil
import xyz.aoei.neovim.{Neovim => Nvim}

import scala.concurrent.Await
import scala.concurrent.duration._
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
        updateWindows()
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
      val curPos = new LogicalPosition(cursor.head - 1, cursor(1))

      val editor = fileListener.selectedTextEditor
      val caretModel = editor.getCaretModel

      if (caretModel.getLogicalPosition != curPos) {
        ApplicationManager.getApplication.invokeLater(new Runnable {
          override def run(): Unit = {
            // Move the cursor to pos
            caretModel.moveToLogicalPosition(curPos)
            editor.getScrollingModel.scrollToCaret(ScrollType.RELATIVE)
          }
        })
      }
    }
  }

  private def updateWindows(): Unit = {
    if (fileListener.selectedTextEditor == null) return

    val project = fileListener.selectedTextEditor.getProject

    val fileEditorManager = FileEditorManager.getInstance(project).asInstanceOf[FileEditorManagerImpl]
    val splitters = fileEditorManager.getSplitters

    for (windows <- nvim.listWins) yield {
      ApplicationManager.getApplication.invokeLater(new Runnable {
        override def run(): Unit = {
          if (windows.length != splitters.getWindows.length) {
            val positions = windows.map(win => {
              val data = for (pos <- win.getPosition) yield ((pos.head, pos(1)), win)

              Await.result(data,.5 seconds)
            })

            val tree = SplitUtil.getTree(positions)
            println(tree)

            val win = splitters.getWindows.head

            val current = fileEditorManager.getCurrentWindow
            for (win <- fileEditorManager.getWindows) {
              if (win != current) win.closeAllExcept(null)
            }

            def split(tree: SplitUtil.Node, win: EditorWindow): Unit = {
              tree match {
                case SplitUtil.Branch(dir, _, l, r) => {
                  val newWin = win.split(dir, true, null, true)
                  split(l, win)
                  split(r, newWin)
                }
                case SplitUtil.Leaf(_, w) => {
                  for {
                    buf <- w.getBuf
                    name <- buf.getName
                  } yield {
                    ApplicationManager.getApplication.invokeLater(new Runnable {
                      override def run(): Unit = {
                        val virtualFile = LocalFileSystem.getInstance().findFileByPath(name)
                        fileEditorManager.setCurrentWindow(win)
                        fileEditorManager.openFile(virtualFile, true)
                        win.closeAllExcept(virtualFile)
                      }
                    })
                  }
                }
              }
            }
            split(tree, win)
          }
        }
      })
    }
  }
}
