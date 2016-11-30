package xyz.aoei.idea.neovim.listener

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager, TextEditor}
import com.intellij.openapi.vfs.LocalFileSystem
import xyz.aoei.idea.neovim.util.Util
import xyz.aoei.idea.neovim.{BuffersChangeListener, NeovimState}

import scala.concurrent.ExecutionContext.Implicits.global

class NeovimBuffersChangeListener extends BuffersChangeListener {
  override def buffersChanged(state: NeovimState): Unit = {
    if (state.windows.length != state.windowBuffers.length) return

    state.windows.zip(state.windowBuffers).foreach {
      case (window, buffer) => {
        for {
          lines <- buffer.getLines(0, -1, strict_indexing = false)
          name <- buffer.getName
        } yield {
          val text = lines.mkString("\n")

          val editorWindow = state.windowToEditor(window)
          val editor = editorWindow.getSelectedEditor.getEditors.head.asInstanceOf[TextEditor].getEditor

          val project = editor.getProject
          val document = editor.getDocument

          val virtualFile = FileDocumentManager.getInstance().getFile(document)
          if (name != virtualFile.getPath) {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(name)
            if (virtualFile != null) {
              Util.invokeLater(() => {
                val fileEditorManager = FileEditorManager.getInstance(state.project).asInstanceOf[FileEditorManagerImpl]

                fileEditorManager.setCurrentWindow(editorWindow)
                fileEditorManager.openFile(virtualFile, true)
                editorWindow.closeAllExcept(virtualFile)

                buffersChanged(state)
              })
            }
          }
          else {
            val caretModel = editor.getCaretModel

            WriteCommandAction.runWriteCommandAction(project, new Runnable {
              override def run(): Unit = {
                document.replaceString(0, document.getTextLength, text)

                // Make sure the cursor is updated immediately after changing text
                val cursor = state.windows.zip(state.cursors).filter(x => x._1 == window).head._2
                val curPos = new LogicalPosition(cursor._1, cursor._2)
                caretModel.moveToLogicalPosition(curPos)
              }
            })
          }
        }
      }
    }
  }
}
